# Amoo Bridge 0.1

本地 macOS 客户端，通过 ADB 连接 PZ261A 手机上的系统 Amoo (`com.stepos.ailauncher`)。支持中文消息发送、读取当前可见回复、读取和翻页手机会话侧栏、选择历史会话及新建聊天。

本版本是 **UI 桥接**：Mac → ADB 端口转发 → 手机上的临时 shell 程序 → Amoo 可访问性界面。没有使用 VPN、云端登录凭据或私有会话数据库；也没有把 OpenClaw 的 18789 端口当作 Amoo 的聊天接口。

## 使用

1. 打开 `dist/Amoo Bridge.app`。手机先解锁，打开 Amoo 的完整聊天界面。
2. 在设备列表选择手机，点击“连接手机”。当前环境已建立 Wi-Fi ADB：`PHONE_IP:ADB_PORT`；也可使用 USB。Wi-Fi 地址、ADB 授权及无线调试状态变化后需要重新连接。
3. 左侧“刷新”读取手机侧栏当前页；“较近 / 更早”翻页，点击标题切换手机会话。
4. 在输入框写消息，按 ⌘ Return 或点击发送按钮。回复通过每秒轮询手机界面同步显示。
5. 断开或退出客户端会关闭它启动的桥接程序，并删除它创建的端口转发。

## 能力边界

- 手机必须解锁，Amoo 必须在前台。客户端操作会改变手机当前界面；本版不支持熄屏和后台运行 Amoo 会话。
- 消息只包含当前可访问性树中的可见文本；“较早 / 较新消息”操作手机列表翻页。图片、文件、完整卡片、不可见历史和完整原生消息元数据尚未同步。
- 侧栏只展示手机当前页会话。相同标题以当前页的出现次序区分，没有把标题散列冒充手机原生会话 ID。消息内容散列用于显示和变化检测，不是原生消息 ID。
- 主区始终跟随手机当前会话，不把后来读到的消息缓存在先前选择的会话名下。
- 发送前检查界面内容是否变化和手机是否已有草稿。点击发送后的不确定结果不会自动重试；请检查手机后再决定下一步。
- 实测系统版本为 Amoo `1.0.0_20261002_221513`。手机系统更新后，控件名称和界面层级可能变化，需要重新验证适配。

## 构建

```sh
./build-mac.sh
```

需要本机 Swift Command Line Tools、Android SDK Platform Tools / Build Tools 和 JDK 17。默认沿用本工作区已有的工具路径；可通过 `AMOO_ANDROID_SDK`、`AMOO_JAVA_HOME`、`AMOO_ADB_PATH` 覆盖。产物为 Apple Silicon、macOS 13+ 的本地签名 `.app`，已包含 ADB 和手机桥接 JAR，无需额外 Python/Node 运行时。

手机端 JAR 以 ADB shell 身份运行在 `/data/local/tmp/amoo-bridge.jar`，没有安装或替换系统应用。它使用 Android UiAutomation；与其他正在运行的 UiAutomation 工具可能互斥。每次连接随机生成访问密钥，仅在内存中使用；手机服务只监听 `127.0.0.1:18799`，Mac 端使用动态本地转发端口。服务连续 30 秒没有请求会自行结束。

## 开发协议

所有请求均需要 `Authorization: Bearer <本次连接密钥>`，失败返回非 2xx 和 `{ "error": "说明" }`。

| 请求 | 作用 |
| --- | --- |
| `GET /snapshot` | 当前屏幕、可见消息、内容校验值 |
| `POST /open {}` | 打开 Amoo |
| `POST /history {"page":0}` | 读取侧栏当前页，读完关闭侧栏；`1/-1` 向后/向前翻页 |
| `POST /select {"title":"…","occurrence":0}` | 选择当前页同名标题第几个会话 |
| `POST /new {}` | 新建手机聊天，已有草稿时拒绝 |
| `POST /scroll {"older":true}` | 翻阅当前聊天消息 |
| `POST /send {"requestId":"…","text":"…","contextKey":"…"}` | 校验后单次点击发送，按请求标识去重 |
| `POST /shutdown {}` | 关闭本次桥接 |

`contextKey` 是当前可见消息内容的校验值，不是稳定会话身份；它能拒绝常见的界面变化竞争，不能提供原生会话 ID 的保证。

验证记录见工作区 `reports/amoo-bridge-smoke-2026-10-20.json` 与 `reports/amoo-bridge-feasibility.md`。
