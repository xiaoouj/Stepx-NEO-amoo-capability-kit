# Amoo 本机能力与限制文件调查 — 2026-10-21

## 结论

用户选择增强“读写文件、执行脚本和操作手机”。这三项在当前原生 Amoo 中已经可用：本轮通过手机原生聊天发送一次受限自测请求，Amoo 实际读取测试文件、创建并执行 Python 脚本、回读结果，并查询自动亮度。随后通过 ADB 独立确认结果文件存在且内容正确。无需删除 AGENTS.md 才能完成这些动作。

## 实机验证

- 测试目录：`/sdcard/Documents/amoo-lab-20261021-d08aa0/`。
- `input.txt` 的回读内容：`AMOO_FILE_READ_WRITE_OK`。
- Amoo 创建 `native_probe.py`，289 字节。
- Amoo 执行该脚本，报告退出码 0、输出 `sum=55`。
- 独立文件系统验证：`native-agent-result.txt` 存在、24 字节、内容为 `AMOO_NATIVE_EXEC_OK: 55` 加换行。
- 脚本 SHA-256：`baa7b77cf37fd7cf1b2486fc53673091668a7a4b802c01796ea73b971cc6922d`。
- Amoo 查询自动亮度，返回已开启。没有修改该开关。
- 另经现有网关验证 Python 3.12.3 文件读写和脚本运行，实际 uid=1000，说明这些基础能力不需要 Android Root。
- 经 Android 节点 `open_app_by_package` 调用打开设置，随后窗口状态确认为 `com.android.settings/.Settings`。实机参数为 `packageName`，打包技能文档的 `package` 在当前接口返回缺少 packageName；这是一处文档与实现不一致。
- SELinux 仍为 Enforcing。

本轮只创建自测目录和测试会话，未编辑 AGENTS.md、TOOLS.md、openclaw.json、系统提示词、已有聊天或其他用户文件。手机上保留自测目录与会话，便于查看结果。

## 限制文件

实际读取了：
`/mnt/openclaw/rootfs/root/.openclaw/workspace/AGENTS.md`

本地实机副本：`.local/amoo/root-investigation/AGENTS.live.md`。

其中存在不解释内部约束、隐藏实现细节、统一身份与设备称谓等行为要求，以及删除文件走回收站、定时任务正确处理时间等操作规则。该文件不是单一“能力开关”。

实机 AGENTS、`_defaults/AGENTS.md`、`.prompt-templates/zh/workspace/AGENTS.md` 三份哈希相同：
`ccff36127bcbcd1aafff407a4f7814316fe6c8cabfe685f42cfbb07b5128ad12`。

默认 OpenClaw bootstrap 在 `workspace-Ddypv-c6.js:426` 加载工作区 AGENTS，即使 prompt-engine 未启用。普通部署通常保留已有工作区文件；强制部署或升级路径可能覆盖。不能据此断言所有原生 Amoo 对话都由这份文件完全控制。

## 工具配置和链路边界

只输出了配置的非敏感子集，见 `.local/amoo/root-investigation/openclaw-config-sanitized.json`，没有输出凭据。

- tools.allow 含 fs、runtime、openclaw、plugins 工具组。
- tools.deny 明确含 web_search、browser。这两个条目不等于整个手机不能联网：原生 Amoo 和 Android 原子能力另有路由。本轮没有测试或更改这些条目。
- prompt-engine 与 clawxrouter 在所读配置中 disabled，不能将 prompt-engine 当作当前唯一提示词来源。
- stepclaw-intercept hook 可以向 Android 18790 请求 systemPrompt / prependContext / appendSystemContext。因此只编辑磁盘 AGENTS 并不能保证覆盖所有提示词来源。
- 原生 Amoo 与通用 OpenClaw agent 不是完全相同的调用链。直接 Gateway agent 自测返回 `404 request_failed`，没有创建预期文件；原生 Amoo UI 自测随后成功。不能把这个 404 解释为原生 Amoo 无法执行脚本。
- ADB Shell 的 Root 授权不会自动让 Amoo 进程取得 Root。本轮没有给 Amoo 增加 Root 授权。

## 可用的工作方式

向 Amoo 提供明确输入范围、输出位置和完成验证即可。例如：
“在我的文档中新建一个测试目录，用 Python 计算并生成结果文件，实际运行脚本，再读取结果确认；只操作这个目录。”

对于批量文件处理，可指定输入目录、匹配规则、输出目录及是否保留原文件。对于手机动作，可明确要打开的 App、要做的步骤和停止位置。应用私有数据、系统分区等 Root 专属操作仍需单独设计执行入口；本轮没有验证或开放无限制系统修改。

## 证据与局限

- 原生请求与可见回复：`.local/amoo/root-investigation/native-ui-probe-request.txt`、`native-ui-probe-result.json`。
- 独立结果副本：`native_probe.py`、`native-agent-result.txt`。
- 网关测试：`probe-setup-output.txt`、`script-output.txt`、`readback-output.txt`。
- 原生链路与覆盖规则的静态说明：`native-path-findings.md`。
- REA 静态分析 parent Evidence：`ev_9f51226c73eb3d0e13fb5f3d1bcc4846b0713d513d7751fb3caeab5a53040494`。目标为实机提取的 stepclaw-intercept/index.js，摘要 Evidence 为 `ev_fa1de1b08fad747ca8673a3f6d8e58221c5373bab3b645ab383df5d25c07788f`。静态分析只选择一个 JS，其他源文件与动态调用关系未完整分析；它本身不证明运行时行为。
- 未读取其他用户文件、聊天历史、登录凭据，也未测试第三方 App 内的复杂 GUI 自动化或 Root 级应用数据操作。
