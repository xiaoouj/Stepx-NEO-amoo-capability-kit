import AppKit
import SwiftUI
import Foundation
import Security

struct Device: Identifiable, Hashable {
    let id: String
    let name: String
    var label: String { "\(name) · \(id.contains(":") ? "Wi-Fi" : "USB")" }
}
struct PhoneMessage: Codable, Identifiable {
    let id: String
    let role: String
    let text: String
}
struct PhoneSession: Codable, Identifiable, Hashable {
    let title: String
    let occurrence: Int
    let id: String
}
struct Snapshot: Decodable {
    let mode: String?
    let package: String?
    let contextKey: String?
    let screen: String?
    let busy: Bool?
    let messages: [PhoneMessage]?
    let sessions: [PhoneSession]?
}
struct BridgeFailure: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}
struct CommandResult: Sendable { let output: String; let status: Int32 }

func runCommand(_ executable: String, _ arguments: [String], timeout: Double = 20) async throws -> CommandResult {
    try await Task.detached(priority: .userInitiated) {
        let process = Process()
        let out = Pipe(), err = Pipe()
        process.executableURL = URL(fileURLWithPath: executable)
        process.arguments = arguments
        process.standardOutput = out
        process.standardError = err
        try process.run()
        let timer = DispatchWorkItem { if process.isRunning { process.terminate() } }
        DispatchQueue.global().asyncAfter(deadline: .now() + timeout, execute: timer)
        process.waitUntilExit()
        timer.cancel()
        let stdout = out.fileHandleForReading.readDataToEndOfFile()
        let stderr = err.fileHandleForReading.readDataToEndOfFile()
        let value = String(data: stdout + stderr, encoding: .utf8) ?? ""
        return CommandResult(output: String(value.prefix(65536)), status: process.terminationStatus)
    }.value
}

final class HelperProcess: @unchecked Sendable {
    private let process = Process()
    private let output = Pipe()
    private let lock = NSLock()
    private var continuation: CheckedContinuation<Void, Error>?
    private var finished = false
    private var buffer = ""
    private var timeout: DispatchWorkItem?

    func start(adb: String, serial: String, token: String) async throws {
        process.executableURL = URL(fileURLWithPath: adb)
        process.arguments = ["-s", serial, "shell", "CLASSPATH=/data/local/tmp/amoo-bridge.jar app_process / com.amoo.bridge.Bridge 18799 \(token)"]
        process.standardOutput = output
        process.standardError = FileHandle.nullDevice
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            lock.lock(); self.continuation = continuation; lock.unlock()
            output.fileHandleForReading.readabilityHandler = { [weak self] handle in
                guard let self else { return }
                let data = handle.availableData
                if data.isEmpty { return }
                let chunk = String(data: data, encoding: .utf8) ?? ""
                self.lock.lock()
                self.buffer += chunk
                let ready = self.buffer.contains("AMOO_BRIDGE_READY")
                if self.buffer.count > 8192 { self.buffer = String(self.buffer.suffix(4096)) }
                self.lock.unlock()
                if ready { self.complete(nil) }
            }
            process.terminationHandler = { [weak self] _ in
                self?.complete(BridgeFailure(message: "手机桥接进程已退出，请重新连接。"))
            }
            let timer = DispatchWorkItem { [weak self] in
                self?.complete(BridgeFailure(message: "等待手机桥接启动超时，请确认手机已解锁。"))
                self?.stop()
            }
            timeout = timer
            DispatchQueue.global().asyncAfter(deadline: .now() + 15, execute: timer)
            do { try process.run() } catch { complete(BridgeFailure(message: "无法启动 ADB 桥接进程。")) }
        }
    }
    private func complete(_ error: Error?) {
        lock.lock()
        guard !finished else { lock.unlock(); return }
        finished = true
        let callback = continuation
        continuation = nil
        lock.unlock()
        timeout?.cancel()
        if let error { callback?.resume(throwing: error) } else { callback?.resume() }
    }
    func stop() {
        timeout?.cancel()
        output.fileHandleForReading.readabilityHandler = nil
        if process.isRunning { process.terminate() }
    }
    var isRunning: Bool { process.isRunning }
    deinit { stop() }
}

actor PhoneBridge {
    private let adb: String
    private let jar: String
    nonisolated var adbPath: String { adb }
    private var serial: String?
    private var token = ""
    private var port: Int?
    private var helper: HelperProcess?
    private let session: URLSession
    private var generation = 0

    init() {
        adb = Bundle.main.resourceURL!.appendingPathComponent("adb").path
        jar = Bundle.main.resourceURL!.appendingPathComponent("amoo-shell.jar").path
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 15
        config.timeoutIntervalForResource = 20
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        config.urlCache = nil
        session = URLSession(configuration: config)
    }
    func devices() async throws -> [Device] {
        guard FileManager.default.isExecutableFile(atPath: adb) else {
            throw BridgeFailure(message: "应用内缺少 ADB，请重新运行构建脚本。")
        }
        let result = try await runCommand(adb, ["devices", "-l"])
        guard result.status == 0 else { throw BridgeFailure(message: "无法读取 ADB 设备列表。") }
        return result.output.split(separator: "\n").compactMap { line in
            let parts = line.split(whereSeparator: { $0.isWhitespace }).map(String.init)
            guard parts.count >= 2, parts[1] == "device" else { return nil }
            let model = parts.first(where: { $0.hasPrefix("model:") }).map { String($0.dropFirst(6)) } ?? parts[0]
            return Device(id: parts[0], name: model)
        }
    }
    func connect(_ device: String) async throws -> Snapshot {
        await disconnect()
        let attempt = generation
        guard FileManager.default.fileExists(atPath: jar) else { throw BridgeFailure(message: "应用内缺少手机桥接组件，请重新构建。") }
        var random = [UInt8](repeating: 0, count: 32)
        guard SecRandomCopyBytes(kSecRandomDefault, random.count, &random) == errSecSuccess else {
            throw BridgeFailure(message: "无法生成本次连接密钥。")
        }
        token = random.map { String(format: "%02x", $0) }.joined()
        serial = device
        do {
            let pushed = try await runCommand(adb, ["-s", device, "push", jar, "/data/local/tmp/amoo-bridge.jar"])
            guard attempt == generation else { throw CancellationError() }
            guard pushed.status == 0 else { throw BridgeFailure(message: "无法向手机发送桥接组件，请检查 ADB 授权。") }
            let running = HelperProcess()
            helper = running
            try await running.start(adb: adb, serial: device, token: token)
            guard attempt == generation else { running.stop(); throw CancellationError() }
            let forward = try await runCommand(adb, ["-s", device, "forward", "tcp:0", "tcp:18799"])
            guard forward.status == 0, let allocated = forward.output.split(whereSeparator: { $0.isWhitespace }).compactMap({ Int($0) }).last, allocated > 0 else {
                throw BridgeFailure(message: "无法建立本地端口转发。")
            }
            guard attempt == generation else {
                _ = try? await runCommand(adb, ["-s", device, "forward", "--remove", "tcp:\(allocated)"], timeout: 3)
                throw CancellationError()
            }
            port = allocated
            // /open 同样受 shell context 的 startActivity 限制，改由 adb 拉起
            _ = try? await runCommand(adb, ["-s", device, "shell", "am", "start", "-n",
                                            "com.stepos.ailauncher/com.stepos.ailauncher.chat.activity.ChatMainActivity"])
            try await Task.sleep(nanoseconds: 1_200_000_000)
            let first = try await snapshot()
            guard attempt == generation else { throw CancellationError() }
            return first
        } catch {
            if attempt == generation { await disconnect() }
            throw error
        }
    }
    func request(_ route: String, body: [String: Any]? = nil, timeout: TimeInterval = 15) async throws -> Data {
        guard let port, !token.isEmpty, helper?.isRunning == true else { throw BridgeFailure(message: "手机连接已断开，请重新连接。") }
        var request = URLRequest(url: URL(string: "http://127.0.0.1:\(port)\(route)")!)
        request.httpMethod = body == nil ? "GET" : "POST"
        request.timeoutInterval = timeout
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        if let body {
            request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        }
        let data: Data
        let response: URLResponse
        do { (data, response) = try await session.data(for: request) }
        catch { throw BridgeFailure(message: "手机连接未响应，请检查 USB / Wi-Fi、解锁状态和 Amoo 前台界面。") }
        guard let http = response as? HTTPURLResponse else { throw BridgeFailure(message: "手机返回了无效响应。") }
        guard (200..<300).contains(http.statusCode) else {
            let error = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
            throw BridgeFailure(message: error?["error"] as? String ?? "手机操作未完成（\(http.statusCode)），请检查手机界面。")
        }
        return data
    }
    func snapshot() async throws -> Snapshot {
        try JSONDecoder().decode(Snapshot.self, from: await request("/snapshot"))
    }
    func action(_ route: String, _ body: [String: Any] = [:]) async throws -> Snapshot {
        try JSONDecoder().decode(Snapshot.self, from: await request(route, body: body))
    }
    func send(text: String, contextKey: String) async throws {
        let data = try await request("/send", body: ["requestId": UUID().uuidString, "text": text, "contextKey": contextKey])
        let result = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
        guard result?["accepted"] as? Bool == true else { throw BridgeFailure(message: "无法确认消息是否发送，请先检查手机；草稿已保留。") }
    }
    func cancelPendingRequests() async {
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            session.getAllTasks { tasks in
                tasks.forEach { $0.cancel() }
                continuation.resume()
            }
        }
    }
    func disconnect() async {
        generation += 1
        if port != nil, !token.isEmpty {
            _ = try? await request("/shutdown", body: [:], timeout: 2)
        }
        helper?.stop()
        helper = nil
        if let serial, let port {
            _ = try? await runCommand(adb, ["-s", serial, "forward", "--remove", "tcp:\(port)"], timeout: 5)
        }
        port = nil
        token = ""
        serial = nil
    }
}

// MARK: - 广播通道
//
// 手机侧官方入口（AiLauncher 的 StartBroadcastReceiver）：
//   com.stepos.ailauncher.action.AI_KEY_WAKE      等价按一下 AI 键
//   com.stepos.ailauncher.action.CARD_SEND_TEXT   extra "card_text" = 要发的话
//   com.stepos.ailauncher.action.DISMISS_WAKE     收起
//
// 发送是「推」：一条 adb 广播就到。
// 回复没有对应的广播可订阅，只能「拉」：
//   · 静默模式 → Amoo 把结果写进 Live Notification，读 dumpsys notification
//   · 唤醒模式 → 回复在聊天界面里，读 uiautomator dump

enum ChannelMode: String, CaseIterable, Identifiable {
    case ui = "UI 桥接"
    case silent = "广播 · 静默"
    case wake = "广播 · 唤醒"
    var id: String { rawValue }
    var isBroadcast: Bool { self != .ui }
    var detail: String {
        switch self {
        case .ui:
            return "跟随手机界面，需要 Amoo 在前台；可切换会话、读全部可见消息。回复从界面文本取。"
        case .silent:
            return "不弹界面，直接把指令投给 Amoo。回复落在手机通知栏 —— 通知标题是会话名不是正文，所以这里不会显示回复。"
        case .wake:
            return "界面闪一下（约 5~10 秒），发完自动退回原 App，回复从界面文本取。最稳。"
        }
    }
}

struct AmooNotice: Sendable, Identifiable {
    let id: String
    let title: String
    let text: String
}

final class BroadcastChannel: @unchecked Sendable {
    static let pkg = "com.stepos.ailauncher"
    private let adb: String
    private let serial: String

    init(adb: String, serial: String) {
        self.adb = adb
        self.serial = serial
    }

    private func shell(_ args: [String]) async throws -> String {
        let result = try await runCommand(adb, ["-s", serial, "shell"] + args, timeout: 25)
        return result.output
    }

    func broadcast(_ action: String, key: String? = nil, value: String? = nil) async throws {
        var args = ["am", "broadcast", "-a", "\(Self.pkg).action.\(action)", "-p", Self.pkg]
        if let key, let value { args += ["--es", key, value] }
        let out = try await shell(args)
        guard !out.contains("Error"), !out.contains("Exception") else {
            throw BridgeFailure(message: "手机拒绝了广播：\(out.trimmingCharacters(in: .whitespacesAndNewlines))")
        }
    }

    /// 读当前所有 Amoo 通知（Live Notification 的标题里就是回复正文）
    func notices() async throws -> [AmooNotice] {
        let out = try await shell(["dumpsys", "notification", "--noredact"])
        var list: [AmooNotice] = []
        let blocks = out.components(separatedBy: "NotificationRecord(0x")
        for (index, raw) in blocks.enumerated() where index > 0 {
            guard raw.prefix(600).contains("pkg=\(Self.pkg)") else { continue }
            let id = Self.firstMatch(raw, #"id=(\d+)"#) ?? ""
            let title = Self.firstMatch(raw, #"android\.title=String \((.*?)\)\n"#) ?? ""
            let text = Self.firstMatch(raw, #"android\.text=String \((.*?)\)\n"#) ?? ""
            guard !title.isEmpty else { continue }
            list.append(AmooNotice(id: id, title: title, text: text))
        }
        return list
    }

    private static func firstMatch(_ source: String, _ pattern: String) -> String? {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.dotMatchesLineSeparators]) else { return nil }
        let range = NSRange(source.startIndex..<source.endIndex, in: source)
        guard let match = regex.firstMatch(in: source, options: [], range: range), match.numberOfRanges > 1,
              let captured = Range(match.range(at: 1), in: source) else { return nil }
        return String(source[captured]).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func waitReply(excluding: Set<String>, timeout: TimeInterval) async -> AmooNotice? {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            // dumpsys notification 实测 ~100ms（Wi-Fi），可以高频轮询
            try? await Task.sleep(nanoseconds: 600_000_000)
            guard let list = try? await notices() else { continue }
            for notice in list where !excluding.contains(notice.title) {
                return notice
            }
        }
        return nil
    }

    /// 静默投递：不唤醒。Amoo 有活跃会话时有效，回复进通知栏。
    func sendSilent(_ text: String) async -> AmooNotice? {
        let before = Set(((try? await notices()) ?? []).map(\.title))
        try? await broadcast("CARD_SEND_TEXT", key: "card_text", value: text)
        return await waitReply(excluding: before.union([text]), timeout: 7)
    }

    /// 唤醒投递：AI 键 → 投递 → 收起。稳定，但界面会闪一下。
    /// 唤醒模式的回复只在聊天界面里（不进通知），所以直接读屏：
    /// 等模型出首字（约 6 秒）→ dump 一次（Wi-Fi 约 2.1s）→ 没有就再等。
    func sendAwake(_ text: String) async -> AmooNotice? {
        try? await broadcast("AI_KEY_WAKE")
        try? await Task.sleep(nanoseconds: 1_500_000_000)
        try? await broadcast("CARD_SEND_TEXT", key: "card_text", value: text)
        try? await Task.sleep(nanoseconds: 6_000_000_000)
        var hit: AmooNotice?
        for _ in 0..<3 {
            if let screen = await readScreenReply(echo: text) {
                hit = AmooNotice(id: "screen", title: screen, text: "")
                break
            }
            try? await Task.sleep(nanoseconds: 2_500_000_000)
        }
        try? await broadcast("DISMISS_WAKE")
        try? await Task.sleep(nanoseconds: 600_000_000)
        return hit
    }

    /// 唤醒模式下回复不在通知里，从界面文本里找「最后一条不是自己说的话」。
    /// 一次 adb 调用完成 dump + 读取，并校验当前确实是 Amoo 聊天界面。
    func readScreenReply(echo text: String) async -> String? {
        guard let xml = try? await shell([
            "uiautomator dump /sdcard/_amoo_bridge.xml >/dev/null 2>&1; cat /sdcard/_amoo_bridge.xml"
        ]) else { return nil }
        var values: [String] = []
        guard let regex = try? NSRegularExpression(pattern: #"text="([^"]+)""#, options: []) else { return nil }
        let range = NSRange(xml.startIndex..<xml.endIndex, in: xml)
        regex.enumerateMatches(in: xml, options: [], range: range) { match, _, _ in
            guard let match, let r = Range(match.range(at: 1), in: xml) else { return }
            let value = String(xml[r]).trimmingCharacters(in: .whitespacesAndNewlines)
            if !value.isEmpty { values.append(value) }
        }
        // 不在 Amoo 聊天界面就别乱猜
        guard values.contains(where: { $0.contains("按住说话") || $0.contains("有什么能帮你的") }) else { return nil }
        let noise: Set<String> = ["发消息或按住说话", "有什么能帮你的？", "正在思考中", "深度思考", "发消息"]
        return values.reversed().first { $0 != text && !noise.contains($0) && $0.count > 1 }
    }

}

/// StepBridge（手机端 stepx-amoo-bridge）的 HTTP 通道。
/// 与本进程内 UiAutomation 那条路互补：它没有 UI 能力，但有常驻服务和 /sdcard 读写。
final class StepBridgeClient: @unchecked Sendable {
    static let port = 8896
    private let adb: String
    private let serial: String
    private let session: URLSession
    private(set) var token = ""
    private(set) var ready = false
    private(set) var lastInfo = ""

    init(adb: String, serial: String) {
        self.adb = adb
        self.serial = serial
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 10
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        config.urlCache = nil
        session = URLSession(configuration: config)
    }
    func start() async throws {
        // 包级 appops 没放开时，App 目录建不出来、服务静默失败
        _ = try? await runCommand(adb, ["-s", serial, "shell", "appops", "set",
                                        "com.stepx.amoobridge", "MANAGE_EXTERNAL_STORAGE", "allow"])
        _ = try? await runCommand(adb, ["-s", serial, "shell", "am", "start", "-n",
                                        "com.stepx.amoobridge/.MainActivity"])
        _ = try? await runCommand(adb, ["-s", serial, "forward", "tcp:\(Self.port)", "tcp:\(Self.port)"])
        for _ in 0..<12 {
            let result = try await runCommand(adb, ["-s", serial, "shell", "cat",
                                                    "/sdcard/bridge-amoobridge/token.txt"])
            let value = result.output.trimmingCharacters(in: .whitespacesAndNewlines)
            if result.status == 0, value.count >= 8, !value.contains("No such file") {
                token = value
                break
            }
            try? await Task.sleep(nanoseconds: 500_000_000)
        }
        guard !token.isEmpty else {
            throw BridgeFailure(message: "StepBridge 未就绪：读不到 token。请在手机上打开 stepx-amoo-bridge，点「启动桥服务」。")
        }
        let pong = try await call("ping")
        guard pong["ok"] as? Bool == true else {
            throw BridgeFailure(message: "StepBridge 没有响应 ping，请确认服务已启动。")
        }
        if let info = try? await call("info") {
            lastInfo = ["ver", "model", "sdk", "uid", "http"]
                .compactMap { key in (info[key]).map { "\(key)=\($0)" } }
                .joined(separator: " ")
        }
        ready = true
    }
    func call(_ tool: String, _ args: [String: Any] = [:]) async throws -> [String: Any] {
        var request = URLRequest(url: URL(string: "http://127.0.0.1:\(Self.port)/call")!)
        request.httpMethod = "POST"
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["tool": tool, "args": args])
        let data: Data
        let response: URLResponse
        do { (data, response) = try await session.data(for: request) }
        catch { throw BridgeFailure(message: "StepBridge 通道未响应（端口 \(Self.port)）。") }
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw BridgeFailure(message: "StepBridge 返回了错误状态。")
        }
        return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
    }
    /// 在 App 身份下执行一条 shell 命令
    func exec(_ command: String) async throws -> String {
        let result = try await call("exec", ["cmd": command])
        guard result["ok"] as? Bool == true else {
            throw BridgeFailure(message: result["error"] as? String ?? "exec 执行失败")
        }
        return result["out"] as? String ?? ""
    }
    func stop() {
        ready = false
        token = ""
        _ = try? Process.run(URL(fileURLWithPath: adb), arguments: ["-s", serial, "forward",
                                                                  "--remove", "tcp:\(Self.port)"])
    }
}

@MainActor final class BridgeModel: ObservableObject {
    @Published var devices: [Device] = []
    @Published var selectedDevice = ""
    @Published var connected = false
    @Published var connecting = false
    @Published var actionBusy = false
    @Published var messages: [PhoneMessage] = []
    @Published var sessions: [PhoneSession] = []
    @Published var draft = ""
    @Published var error: String?
    @Published var status = "连接你的 Amoo 手机"
    @Published var contextKey = ""
    @Published var phoneBusy = false
    @Published var lastSync: Date?
    @Published var stepStatus = "未连接"
    @Published var relayInfo: String?
    @Published var autoRelay = false
    @Published var channel: ChannelMode = .ui
    @Published var channelNote = ""
    private var lastMessageCount = 0
    private var gate = false
    private var closing = false
    private let bridge = PhoneBridge()
    private var step: StepBridgeClient?
    private var poller: Task<Void, Never>?
    private var lastRelayContext = ""
    private var lastRelayAt = Date.distantPast

    func refreshDevices() async {
        guard !gate else { return }
        gate = true
        defer { gate = false }
        do {
            devices = try await bridge.devices()
            if !devices.contains(where: { $0.id == selectedDevice }) { selectedDevice = devices.first?.id ?? "" }
            if devices.isEmpty { status = "未找到已授权设备 · 请连接 USB 或已有的 Wi-Fi ADB" }
        } catch { self.error = error.localizedDescription }
    }
    func connect() async {
        guard !closing, !gate, !selectedDevice.isEmpty else { return }
        if channel.isBroadcast { await connectBroadcast(); return }
        gate = true; connecting = true; error = nil
        status = "正在启动手机桥接…"
        defer { gate = false; connecting = false }
        do {
            let snapshot = try await bridge.connect(selectedDevice)
            guard !closing else { return }
            connected = true
            apply(snapshot)
            if snapshot.screen != "history" { status = "已连接 · UI 可见文本桥接" }
            await startStepBridge()
            startPolling()
        } catch { if !closing && !(error is CancellationError) { self.error = error.localizedDescription; status = "连接未完成" } }
    }
    /// 广播通道不需要手机桥接进程，只确认 ADB 可达
    func connectBroadcast() async {
        gate = true; connecting = true; error = nil
        status = "正在检测广播通道…"
        defer { gate = false; connecting = false }
        do {
            let probe = try await runCommand(bridge.adbPath, ["-s", selectedDevice, "shell", "echo", "amoo-ok"], timeout: 12)
            guard probe.output.contains("amoo-ok") else {
                throw BridgeFailure(message: "手机没有响应，请确认 ADB 已授权、设备已解锁。")
            }
            guard !closing else { return }
            connected = true
            channelNote = "已连接 · \(channel.rawValue)"
            status = "已连接 · \(channel.rawValue)"
        } catch {
            if !closing { self.error = error.localizedDescription; status = "连接未完成" }
        }
    }
    func disconnect() async {
        if closing {
            while closing { try? await Task.sleep(nanoseconds: 20_000_000) }
            return
        }
        closing = true
        poller?.cancel(); poller = nil
        connected = false; actionBusy = true
        step?.stop(); step = nil; stepStatus = "未连接"; relayInfo = nil
        await bridge.cancelPendingRequests()
        await bridge.disconnect()
        connecting = false; actionBusy = false
        messages = []; sessions = []; contextKey = ""; lastSync = nil; phoneBusy = false
        status = "已断开连接"; gate = false; closing = false
    }
    func apply(_ snapshot: Snapshot, keepSessions: Bool = true) {
        let showingHistory = snapshot.screen == "history"
        contextKey = showingHistory ? "" : (snapshot.contextKey ?? "")
        messages = snapshot.messages ?? []
        if showingHistory { status = "手机会话侧栏已打开 · 请关闭侧栏后发送" }
        phoneBusy = snapshot.busy ?? false
        if let visible = snapshot.sessions, !visible.isEmpty || !keepSessions { sessions = visible }
        lastSync = Date()
        // AI 键每次都会新开一条空会话：从「有消息」掉到「空」就是一轮新对话的开始
        let empty = messages.isEmpty && (snapshot.screen ?? "chat") == "chat"
        if empty {
            if autoRelay, lastMessageCount > 0, !actionBusy, !closing,
               Date().timeIntervalSince(lastRelayAt) > 60 {
                lastRelayAt = Date()
                Task { await relayOnce() }
            }
        } else {
            lastMessageCount = messages.count
        }
    }
    func perform(_ route: String, body: [String: Any] = [:], resetSessions: Bool = false) async {
        guard connected, !actionBusy else { return }
        guard !channel.isBroadcast else {
            channelNote = "「\(route)」需要 UI 桥接通道，切到「UI 桥接」后再试"
            return
        }
        actionBusy = true
        while gate { try? await Task.sleep(nanoseconds: 20_000_000) }
        guard connected else { actionBusy = false; return }
        gate = true; error = nil
        defer { gate = false; actionBusy = false }
        do {
            let snapshot: Snapshot
            if route == "/open" {
                // shell context 起不了 Activity，交给 adb
                _ = try? await runCommand(bridge.adbPath, ["-s", selectedDevice, "shell", "am", "start", "-n",
                                                           "com.stepos.ailauncher/com.stepos.ailauncher.chat.activity.ChatMainActivity"])
                try await Task.sleep(nanoseconds: 1_000_000_000)
                snapshot = try await bridge.snapshot()
            } else {
                snapshot = try await bridge.action(route, body)
            }
            guard !closing, connected else { return }
            apply(snapshot, keepSessions: !resetSessions)
            if snapshot.screen != "history" { status = "已连接 · UI 可见文本桥接" }
        } catch { if !closing && connected { self.error = error.localizedDescription } }
    }
    func send() async {
        let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard connected, !actionBusy, !text.isEmpty else { return }
        if channel.isBroadcast { await sendByBroadcast(text); return }
        guard !contextKey.isEmpty else { return }
        let originalContext = contextKey
        let sendingDraft = draft
        actionBusy = true
        while gate { try? await Task.sleep(nanoseconds: 20_000_000) }
        guard connected else { actionBusy = false; return }
        gate = true; error = nil
        defer { gate = false; actionBusy = false }
        do {
            guard contextKey == originalContext else {
                throw BridgeFailure(message: "手机当前会话发生变化，请核对当前界面后重新发送。")
            }
            try await bridge.send(text: text, contextKey: originalContext)
            guard !closing, connected else { return }
            if draft == sendingDraft { draft = "" }
            status = "已点击手机发送 · 等待界面更新"
            if let snapshot = try? await bridge.snapshot(), !closing, connected { apply(snapshot) }
        } catch {
            guard !closing, connected else { return }
            self.error = "\(error.localizedDescription)\n未自动重发，草稿已保留。请检查手机后再决定是否发送。"
        }
    }
    // MARK: - 广播通道发送

    func sendByBroadcast(_ text: String) async {
        guard connected, !actionBusy, !selectedDevice.isEmpty else { return }
        actionBusy = true
        error = nil
        let sendingDraft = draft
        draft = ""
        messages.append(PhoneMessage(id: UUID().uuidString, role: "user", text: text))
        status = channel == .silent ? "静默投递中…" : "唤醒投递中…"
        let client = BroadcastChannel(adb: bridge.adbPath, serial: selectedDevice)
        let started = Date()
        var reply: AmooNotice?
        var used = channel
        if channel == .silent {
            // 静默通道只负责投递：通知里的标题是会话名，正文在 RemoteViews 里读不出来，
            // 所以不要在这里假装能取到回复，也不自动降级（降级就会弹界面）。
            try? await client.broadcast("CARD_SEND_TEXT", key: "card_text", value: text)
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            let cost = String(format: "%.1fs", Date().timeIntervalSince(started))
            channelNote = "已投递 · 静默 · \(cost) · 回复在手机通知栏"
            status = "已投递 · 静默（不显示回复）"
            messages.append(PhoneMessage(id: UUID().uuidString, role: "status",
                                         text: "指令已静默投递到手机。回复会出现在手机通知栏/灵动岛，Mac 这边不取正文。"))
            actionBusy = false
            return
        } else {
            reply = await client.sendAwake(text)
        }
        let cost = String(format: "%.1fs", Date().timeIntervalSince(started))
        if let reply {
            messages.append(PhoneMessage(id: UUID().uuidString, role: "assistant",
                                         text: reply.text.isEmpty ? reply.title : "\(reply.title)\n\(reply.text)"))
            channelNote = "已送达 · \(used.rawValue) · \(cost)"
            status = "已送达 · \(used.rawValue)"
        } else {
            channelNote = "未取到回复 · \(used.rawValue) · \(cost)"
            status = "未取到回复 · 可改用 UI 桥接"
            messages.append(PhoneMessage(id: UUID().uuidString, role: "status",
                                         text: "没在界面上读到回复。可能还在生成，或界面被别的应用挡住。"))
        }
        actionBusy = false
    }
    // MARK: - StepBridge（手机端常驻通道）

    func startStepBridge() async {
        guard let serial = selectedDevice.isEmpty ? nil : selectedDevice else { return }
        stepStatus = "正在连接 StepBridge…"
        let client = StepBridgeClient(adb: bridge.adbPath, serial: serial)
        do {
            try await client.start()
            step = client
            stepStatus = "已连接 · \(client.lastInfo)"
        } catch {
            stepStatus = error.localizedDescription
        }
    }

    /// 在 App 身份下跑一条命令（/sdcard 可写，但没有 root）
    func stepExec(_ command: String) async -> String {
        guard let step, step.ready else { return "StepBridge 未连接" }
        do { return try await step.exec(command) }
        catch { return "错误：\(error.localizedDescription)" }
    }

    // MARK: - 会话接力：把上一条会话折进新会话

    private func digest(_ messages: [PhoneMessage], limit: Int = 8) -> String {
        let usable = messages.filter { ($0.role == "user" || $0.role == "assistant") && !$0.text.isEmpty }
        let tail = usable.suffix(limit)
        var lines: [String] = []
        for m in tail {
            let who = m.role == "user" ? "我" : "Amoo"
            let body = m.text.count > 220 ? String(m.text.prefix(220)) + "…" : m.text
            lines.append("\(who)：\(body)")
        }
        return lines.joined(separator: "\n")
    }

    /// 读侧栏里最近的、有正文的会话（跳过新建的空会话）
    func capturePrevious() async throws -> (title: String, text: String) {
        var snap = try await bridge.action("/history", ["page": 0])
        guard let list = snap.sessions, !list.isEmpty else {
            throw BridgeFailure(message: "侧栏没有读到会话，请确认手机已解锁、Amoo 在前台。")
        }
        for candidate in list.prefix(3) {
            snap = try await bridge.action("/select", ["title": candidate.title, "occurrence": candidate.occurrence])
            var merged = snap.messages ?? []
            guard !merged.isEmpty else { continue }
            var seen = Set(merged.map { $0.id })
            for _ in 0..<3 {
                guard let older = try? await bridge.action("/scroll", ["older": true]) else { break }
                let fresh = (older.messages ?? []).filter { !seen.contains($0.id) }
                if fresh.isEmpty { break }
                merged = fresh + merged
                seen.formUnion(fresh.map { $0.id })
            }
            let text = digest(merged)
            if !text.isEmpty { return (candidate.title, text) }
        }
        throw BridgeFailure(message: "前三个会话都没有可见正文。")
    }

    /// 新建一条会话，并把上一条的正文作为上下文发进去
    func relayOnce() async {
        guard connected, !actionBusy, !closing else { return }
        actionBusy = true
        relayInfo = "正在读取上一条会话…"
        defer { actionBusy = false }
        do {
            let captured = try await capturePrevious()
            relayInfo = "已读到「\(captured.title)」，正在开新会话…"
            // /new 在 shell 上下文里没有 startActivity 权限（Permission Denial: package=android），改用 adb
            let launched = try await runCommand(bridge.adbPath, ["-s", selectedDevice, "shell", "am", "start",
                                                                 "-n", "com.stepos.ailauncher/com.stepos.ailauncher.chat.activity.ChatMainActivity",
                                                                 "--ez", "chat_new_chat", "true"])
            guard launched.status == 0 else { throw BridgeFailure(message: "无法在手机上新建会话。") }
            try await Task.sleep(nanoseconds: 2_500_000_000)
            let fresh = try await bridge.snapshot()
            apply(fresh)
            guard let key = fresh.contextKey, !key.isEmpty else {
                throw BridgeFailure(message: "新建会话后拿不到 contextKey。")
            }
            let text = "（接上次对话，下面是历史摘录，不必复述，直接接着聊）\n" + captured.text
            try await bridge.send(text: text, contextKey: key)
            lastRelayContext = key
            lastRelayAt = Date()
            relayInfo = "已接力「\(captured.title)」"
            status = "已把上一条会话注入新会话"
            if let snapshot = try? await bridge.snapshot() { apply(snapshot) }
        } catch {
            relayInfo = nil
            self.error = error.localizedDescription
            status = "接力未完成"
        }
    }

    func startPolling() {
        poller?.cancel()
        poller = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard !Task.isCancelled, let self, self.connected else { return }
                guard !self.gate else { continue }
                self.gate = true
                do {
                    let snapshot = try await self.bridge.snapshot()
                    guard !self.closing, self.connected else { self.gate = false; return }
                    self.apply(snapshot)
                    if self.error == nil && snapshot.screen != "history" { self.status = "已连接 · UI 可见文本桥接" }
                } catch {
                    guard !self.closing, self.connected else { self.gate = false; return }
                    self.error = error.localizedDescription
                    self.messages = []; self.contextKey = ""; self.phoneBusy = false
                }
                self.gate = false
            }
        }
    }
}

@MainActor final class AppDelegate: NSObject, NSApplicationDelegate {
    var model: BridgeModel?
    private var terminating = false
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { true }
    func applicationShouldTerminate(_ sender: NSApplication) -> NSApplication.TerminateReply {
        guard !terminating else { return .terminateNow }
        terminating = true
        Task { await model?.disconnect(); sender.reply(toApplicationShouldTerminate: true) }
        return .terminateLater
    }
}

struct ContentView: View {
    @ObservedObject var model: BridgeModel
    var body: some View {
        VStack(spacing: 0) {
            connectionBar
            Divider()
            HSplitView {
                sidebar.frame(minWidth: 230, idealWidth: 265, maxWidth: 350)
                conversation.frame(minWidth: 520)
            }
        }
        .frame(minWidth: 900, minHeight: 620)
        .background(Color(nsColor: .windowBackgroundColor))
        .task { await model.refreshDevices() }
    }
    private var connectionBar: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 11).fill(.orange.gradient).frame(width: 38, height: 38)
                Image(systemName: "iphone.and.arrow.forward").font(.system(size: 19, weight: .semibold)).foregroundStyle(.white)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("Amoo Bridge").font(.headline)
                Text("手机与 Mac 的会话窗口").font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            Picker("设备", selection: $model.selectedDevice) {
                if model.devices.isEmpty { Text("没有已授权设备").tag("") }
                ForEach(model.devices) { Text($0.label).tag($0.id) }
            }.labelsHidden().frame(maxWidth: 240).disabled(model.connected || model.connecting)
            Picker("通道", selection: $model.channel) {
                ForEach(ChannelMode.allCases) { Text($0.rawValue).tag($0) }
            }.labelsHidden().frame(width: 132)
                .disabled(model.connected || model.connecting)
                .help(model.channel.detail)
            Button { Task { await model.refreshDevices() } } label: { Image(systemName: "arrow.clockwise") }
                .help("刷新已授权的 ADB 设备").disabled(model.connected || model.connecting)
            Button(model.connected ? "断开" : (model.connecting ? "连接中…" : "连接手机")) {
                Task { if model.connected { await model.disconnect() } else { await model.connect() } }
            }
            .buttonStyle(.borderedProminent).tint(model.connected ? .gray : .orange)
            .disabled(model.connecting || (!model.connected && model.selectedDevice.isEmpty) || model.actionBusy)
        }.padding(.horizontal, 20).padding(.vertical, 13)
    }
    private var sidebar: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("手机会话").font(.headline)
                Spacer()
                Button { Task { await model.perform("/new") } } label: { Image(systemName: "square.and.pencil") }.help("在手机上新建会话")
            }.padding(16)
            HStack(spacing: 8) {
                Button("较近") { Task { await model.perform("/history", body: ["page": -1], resetSessions: true) } }
                Button("刷新") { Task { await model.perform("/history", body: ["page": 0], resetSessions: true) } }
                Button("更早") { Task { await model.perform("/history", body: ["page": 1], resetSessions: true) } }
            }.controlSize(.small).padding(.horizontal, 16).padding(.bottom, 12)
            Button {
                Task { await model.relayOnce() }
            } label: {
                Label("接力上一条", systemImage: "arrow.triangle.turn.up.right.diamond")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent).tint(.orange).controlSize(.small)
            .disabled(!model.connected || model.actionBusy)
            .padding(.horizontal, 16).padding(.bottom, 10)
            .help("读取最近一条有内容的会话，开一个新会话，并把它的正文作为上下文发进去")
            Divider()
            if model.sessions.isEmpty {
                VStack(spacing: 10) {
                    Image(systemName: "bubble.left.and.bubble.right").font(.system(size: 29)).foregroundStyle(.tertiary)
                    Text("点击刷新读取手机当前页会话").font(.callout).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    Text("这里只显示手机侧栏当前可见的条目").font(.caption).foregroundStyle(.tertiary).multilineTextAlignment(.center)
                }.frame(maxWidth: .infinity, maxHeight: .infinity).padding(24)
            } else {
                ScrollView {
                    LazyVStack(spacing: 5) {
                        ForEach(model.sessions) { item in
                            Button {
                                Task { await model.perform("/select", body: ["title": item.title, "occurrence": item.occurrence]) }
                            } label: {
                                HStack(alignment: .top, spacing: 9) {
                                    Image(systemName: "bubble.left").font(.system(size: 13)).foregroundStyle(.orange).padding(.top, 2)
                                    Text(item.title).font(.system(size: 13)).lineLimit(2).frame(maxWidth: .infinity, alignment: .leading)
                                }.padding(11).contentShape(Rectangle())
                            }.buttonStyle(.plain)
                            .background(RoundedRectangle(cornerRadius: 8).fill(Color(nsColor: .controlBackgroundColor)))
                        }
                    }.padding(9)
                }
            }
            Divider()
            VStack(alignment: .leading, spacing: 7) {
                if model.channel.isBroadcast {
                    Label(model.channel.rawValue, systemImage: "antenna.radiowaves.left.and.right")
                        .font(.caption.weight(.semibold))
                    Text(model.channel.detail).font(.caption).foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(model.channelNote.isEmpty ? "尚未发送" : model.channelNote)
                        .font(.caption2).foregroundStyle(.orange)
                        .fixedSize(horizontal: false, vertical: true)
                } else {
                    Label("UI 可见文本桥接", systemImage: "viewfinder").font(.caption.weight(.semibold))
                    Text("请保持手机解锁、Amoo 位于前台。此窗口跟随手机当前界面，仅同步可见文本；翻页会操作手机界面。").font(.caption).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
                }
                Divider().padding(.vertical, 2)
                Label("StepBridge 常驻通道", systemImage: "bolt.horizontal").font(.caption.weight(.semibold))
                Text(model.stepStatus).font(.caption2).foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                if let relay = model.relayInfo {
                    Text(relay).font(.caption2).foregroundStyle(.orange)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Divider().padding(.vertical, 2)
                Toggle("新会话自动接力", isOn: $model.autoRelay)
                    .font(.caption).toggleStyle(.switch).controlSize(.mini)
                Text("开启后，检测到空的新会话时自动读取上一条并注入。")
                    .font(.caption2).foregroundStyle(.tertiary)
                    .fixedSize(horizontal: false, vertical: true)
            }.padding(16)
        }.disabled(!model.connected || model.actionBusy).background(Color(nsColor: .underPageBackgroundColor))
    }
    private var conversation: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("当前手机会话").font(.system(size: 17, weight: .semibold))
                    HStack(spacing: 5) {
                        Circle().fill(model.connected ? .green : .gray).frame(width: 6, height: 6)
                        Text(model.status).font(.caption).foregroundStyle(.secondary)
                    }
                }
                Spacer()
                if model.actionBusy { ProgressView().controlSize(.small) }
                Button("打开 Amoo") { Task { await model.perform("/open") } }.disabled(!model.connected || model.actionBusy)
            }.padding(20)
            Divider()
            if !model.connected {
                VStack(spacing: 16) {
                    Image(systemName: "iphone.and.arrow.forward").font(.system(size: 54, weight: .light)).foregroundStyle(.orange)
                    Text("把 Amoo 带到 Mac 上").font(.title2.weight(.semibold))
                    Text("连接已授权的手机，即可查看当前对话、\n切换手机会话，并从 Mac 发送消息。").foregroundStyle(.secondary).multilineTextAlignment(.center)
                    Text("支持 USB 和已建立的 Wi-Fi ADB 连接").font(.caption).foregroundStyle(.tertiary)
                }.frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if model.messages.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: model.channel.isBroadcast ? "antenna.radiowaves.left.and.right" : "text.bubble")
                        .font(.system(size: 36)).foregroundStyle(.tertiary)
                    Text(model.channel.isBroadcast ? "广播通道已就绪" : "手机当前没有可见消息").foregroundStyle(.secondary)
                    Text(model.channel.isBroadcast
                         ? "在下方输入，回车即通过手机官方广播入口投递；回复会出现在这里。"
                         : "打开 Amoo 的聊天界面，或从左侧选择会话。")
                        .font(.caption).foregroundStyle(.tertiary)
                }.frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 18) {
                        ForEach(model.messages) { message in messageView(message) }
                    }.padding(24).frame(maxWidth: 1100)
                }
            }
            if let error = model.error {
                HStack(alignment: .top, spacing: 9) {
                    Image(systemName: "exclamationmark.circle.fill").foregroundStyle(.orange)
                    Text(error).font(.callout).textSelection(.enabled).frame(maxWidth: .infinity, alignment: .leading)
                    Button { model.error = nil } label: { Image(systemName: "xmark") }.buttonStyle(.plain).foregroundStyle(.secondary)
                }.padding(12).background(Color.orange.opacity(0.09)).padding(.horizontal, 16).padding(.bottom, 8)
            }
            if model.connected {
                HStack {
                    Button { Task { await model.perform("/scroll", body: ["older": true]) } } label: { Label("较早消息", systemImage: "arrow.up") }
                    Button { Task { await model.perform("/scroll", body: ["older": false]) } } label: { Label("较新消息", systemImage: "arrow.down") }
                    Spacer()
                    if model.phoneBusy { Label("手机正在回复", systemImage: "ellipsis.bubble").foregroundStyle(.orange) }
                    if let date = model.lastSync { Text(date, style: .time).foregroundStyle(.tertiary) }
                }.font(.caption).controlSize(.small).padding(.horizontal, 20).padding(.bottom, 10).disabled(model.actionBusy)
            }
            Divider()
            composer
        }.background(Color(nsColor: .textBackgroundColor))
    }
    private func messageView(_ message: PhoneMessage) -> some View {
        HStack(alignment: .top, spacing: 11) {
            if message.role == "user" { Spacer(minLength: 40) }
            if message.role != "user" {
                Image(systemName: message.role == "status" ? "info.circle" : "sparkles")
                    .font(.system(size: 16, weight: .medium)).foregroundStyle(.orange).frame(width: 25).padding(.top, 14)
            }
            VStack(alignment: .leading, spacing: 6) {
                Text(message.role == "user" ? "你 · 手机" : (message.role == "assistant" ? "Amoo" : "手机界面"))
                    .font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                Text(message.text).font(.system(size: 14)).lineSpacing(5).textSelection(.enabled).frame(maxWidth: .infinity, alignment: .leading)
            }.padding(14).background(RoundedRectangle(cornerRadius: 14).fill(message.role == "user" ? Color.orange.opacity(0.10) : Color(nsColor: .controlBackgroundColor)))
            if message.role != "user" { Spacer(minLength: 30) }
        }
    }
    private var composer: some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(alignment: .bottom, spacing: 12) {
                TextEditor(text: $model.draft).font(.system(size: 14)).scrollContentBackground(.hidden)
                    .frame(minHeight: 58, maxHeight: 110).padding(7)
                    .background(RoundedRectangle(cornerRadius: 10).fill(Color(nsColor: .controlBackgroundColor)))
                    .overlay(alignment: .topLeading) {
                        if model.draft.isEmpty { Text("向手机 Amoo 发送消息…").font(.system(size: 14)).foregroundStyle(.tertiary).padding(12).allowsHitTesting(false) }
                    }
                    .disabled(!model.connected || model.actionBusy)
                Button { Task { await model.send() } } label: { Image(systemName: "arrow.up").font(.system(size: 18, weight: .semibold)).frame(width: 24, height: 29) }
                    .buttonStyle(.borderedProminent).tint(.orange).keyboardShortcut(.return, modifiers: .command)
                    .disabled(!model.connected || model.actionBusy || model.phoneBusy
                              || (!model.channel.isBroadcast && model.contextKey.isEmpty)
                              || model.draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            Text(model.channel.isBroadcast
                 ? "⌘ Return 发送 · 走手机官方广播入口 · 静默模式失败会自动改用唤醒投递"
                 : "⌘ Return 发送 · 每次只点击一次手机发送 · 不确定是否送达时，请先检查手机")
                .font(.caption2).foregroundStyle(.tertiary)
        }.padding(16)
    }
}

@main struct AmooBridgeApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    @StateObject private var model = BridgeModel()
    var body: some Scene {
        WindowGroup {
            ContentView(model: model)
                .onAppear { delegate.model = model; NSApp.activate(ignoringOtherApps: true) }
        }.defaultSize(width: 1100, height: 780)
            .commands {
                CommandGroup(replacing: .newItem) {
                    Button("新建手机会话") { Task { await model.perform("/new") } }.keyboardShortcut("n").disabled(!model.connected || model.actionBusy)
                }
                CommandMenu("手机") {
                    Button("刷新设备") { Task { await model.refreshDevices() } }.disabled(model.connected)
                    Button("打开 Amoo") { Task { await model.perform("/open") } }.disabled(!model.connected || model.actionBusy)
                    Button("刷新手机会话") { Task { await model.perform("/history", body: ["page": 0], resetSessions: true) } }.disabled(!model.connected || model.actionBusy)
                    Divider()
                    Button("断开连接") { Task { await model.disconnect() } }.disabled(!model.connected || model.actionBusy)
                }
            }
    }
}
