# Amoo Mac 桥接实机调查（2026-10-20）

目标为用户手机系统 Amoo，包名 `com.stepos.ailauncher`，版本 `1.0.0_20261002_221513`；设备 PZ261A。以下按实际证据区分结论。

## 已观察到

- ADB 通过现有 USB 授权连接成功；随后 `adb tcpip 5555` 与 `adb connect PHONE_IP:ADB_PORT` 成功。Mac 和手机都在同一 Wi-Fi 网段。
- `AiLauncher.apk` manifest 声明导出的 `ChatDesktopDebugActivity`。实际启动只打开手机聊天界面，不能由该名字断言存在 Mac 网络聊天 API。
- 普通 shell 对 TaskInfoCardProvider 和 StepAssistantSearchProvider 查询均被 Android 权限检查拒绝。Amoo 包不可 `run-as`。
- ADB 映射手机 `18789` 后，GET `/` 返回标题为 `OpenClaw Control` 的页面；它不能直接作为 Amoo 原生会话接口。
- 手机上的 `com.zyql.agentfront` 未安装。Amoo 中保留该桥接客户端字符串，不等于本机存在相应服务。
- 本次临时 shell helper 的 UiAutomation 初始化成功；能读取 Amoo 可见控件、输入 Unicode、点击 `btn_text_send`、读取原生回复。
- 发出两条仅要求回显测试标识、不执行其他操作的测试消息，收到 `AMOO_BRIDGE_OK` 和 `AMOO_BRIDGE_OK_2`。同一发送请求标识重复提交，没有产生第二条相同用户消息。
- 会话侧栏读到 11 个条目，翻页后为 16 个条目，并可返回较近页；已验证选择本次测试会话。未修改或删除用户已有会话。

## 静态观察与尚未验证的接口

- REA MCP `inspect_android_package` 因缺少 JADX MCP JAR 不可用；本次 APK 后续分析使用现有 Android SDK 的 `aapt2`、`dexdump` 和 DEX 字符串解析。这些是普通工具的静态观察，不宣称为 REA 成功反编译或运行证据。
- AgentBus 包确有 REST/SSE 实现，默认 `127.0.0.1:18801`，但发行包的容器工厂显式传入 `enabled=false`。未找到正常外部开关。`chat.send` 经过 OpenClaw 路由，没有证明可以同步 Amoo 原生历史。抽象 Unix socket `agentbus` 有 peer UID 和包名白名单检查。
- ClawX 的 `18790` 为 hooks 服务，`18791` 为代理服务；打包的 OpenClaw 把 `18800–18899` 定义为浏览器 CDP 端口范围。未通过这些接口取得 Amoo 原生会话。
- 已编写并交付的客户端使用 UI 桥接，没有绕过上述 provider 权限，也没有依赖云端 token。

完整静态调查原始数据保存在工作区被忽略的 `.local/amoo/`，包括 APK、本地 dexdump、manifest、两份独立调查说明；它们不是交付应用运行所需资源。

## 交付范围

Mac 原生客户端的实际能力为：连接 ADB、临时部署 shell helper、可见消息轮询、单次发送、侧栏翻页/选择、新建聊天、断开清理。手机需解锁且 Amoo 在前台；没有承诺后台无界面运行、全量历史/附件同步或原生消息 ID。Native API 级复刻截图中的 StepBridge 仍未达成。
