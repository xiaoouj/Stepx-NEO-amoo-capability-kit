# StepBridge 与本机 Root 可扩展范围核查

日期：2026-10-22。上游 https://github.com/nexaforgelab/StepBridge ，本次读取提交 `df5dd95bd34cb7e3fea618ce5153ccb0e1111049`，本地参考副本 `.local/reference/StepBridge`。本报告区分源码实现、实机观察和待实现方案，未安装或运行上游应用及初始化脚本。

## 结论

Root 使手机本地能力扩展和后台桥接更可行，但不能据此宣称原生 Amoo 已经实现关屏后台收发、历史无损同步或 Mac 工具调用。推荐复用 StepBridge 的 macOS 界面、设备连接和能力目录，后台通道与 Mac 执行器需要单独实现及验证。

用户期望的方向保持为：手机原生 Amoo 理解任务，通过自建 AppFunctions 调用手机服务或经过认证的 Mac 执行器，并回传真实结果。现有 `phone-tools` 已验证原生 Amoo 能调用自建工具，是比反复操作手机前台更可靠的扩展起点。

## 源码中的真实架构

- `Sources/Core/Transport/ADBTransport.swift:972`：使用 `adb forward tcp:0 localabstract:agentbus`；`1147–1364` 实现四字节大端长度的 JSON 帧、协议 3 的 operator 握手及 RPC。这是本地通信转发，不是 VPN 抓包或截获 HTTPS。
- `Sources/Core/Providers/SystemAIProvider/SystemAIProvider.swift:1206`：会话同步优先尝试 `sessions.list`、`chat.history`，但仍有 UiAutomator、日志和本地缓存回退，不能从类名或 README 直接认定返回了原生 Amoo 全部历史。
- 同文件 `2032–2059`：发送先清空 logcat；绑定会话尝试 `chat.send` 后仍打开 Amoo 会话；新会话打开聊天界面后使用 `CARD_SEND_TEXT` 广播。
- 同文件 `2085–2094`：轮询期间约每 20 秒执行 `svc power stayon true` 和唤屏按键。`ADBTransport.swift:1048–1067` 还有 screen_off 状态下唤醒/解锁界面的恢复路径。因此当前上游也存在前台、亮屏依赖。
- `Sources/Core/Gateway/GatewayClient.swift:941`：收到网关 request 帧直接忽略；未找到完整的 Mac node 注册、工具请求分派和 Mac 本机执行结果回传服务。该文件 `530–531` 的 AgentBus 兼容分支甚至直接为 `stepclaw.system.run` 返回成功对象，并未执行命令。
- `ClawXProvider.swift` 的实际 `stepclaw.system.run` 用于手机 Linux 容器。聊天中的 `/root`、宝塔、Linux 内核与容器信息，不能证明操作发生在 macOS。

## 今天实机验证

设备为无线 ADB 连接的 PZ261A。检查普通连接后，以 `su -c id` 独立确认 `uid=0(root)`、KernelSU 上下文；SELinux 保持 `Enforcing`。

按上游协议创建仅本机的临时 ADB 转发并发送 connect，收到 `hello-ok`、`ok:true`。随后只读 `sessions.list`（limit=1）在 6 秒内未返回，连接超时；没有发送聊天消息，也没有唤屏、切换应用或运行上游自动恢复逻辑。超时原因未定位，不能把握手成功等同于后台原生聊天已经打通。仅删除本次创建的转发。

Root 能读取 Amoo 私有目录中的 `agent_framework_*.db` 文件字节。但其开头不是明文 SQLite 标头，普通 SQLite 无法直接打开。旧版 APK 字节码证据 `.local/amoo/claw-ailauncher-classes2.dump:1420055` 附近的 `FwkDatabase$i.l` 调用宿主 cipher factory，并含 `refusing plaintext open` 分支，支持存在应用层数据库加密的判断；具体当前加密实现未进一步验证。没有导出用户历史、密钥或聊天正文，没有将原始数据库落盘。

## Root 可以推进的工作

1. **手机后台桥接**：建立受控本地服务，研究 Amoo 自身的 BFF/会话调用路径及生命周期，使请求与结果订阅不依赖打开 Activity。关屏、切换其他应用、长任务和断线恢复均需要真实验证。
2. **真实历史同步**：通过应用自身的数据访问或已解密对象通道取得会话及消息；不能把文件系统可读当作数据库可查询，也不能直接写用户数据库制造会话。
3. **手机能力扩展**：沿已验证的 `com.amoo.executor` AppFunctions 路径增加明确动作，需要 Root 的操作交给本地执行服务。现有设置直达及导航已完成原生自然语言调用验证。
4. **Mac 工具执行**：在 Mac 新增执行服务，手机 AppFunction 通过配对认证的连接提交请求，Mac 执行并回传结果。手机 Root 不会赋予 Mac 的辅助功能、自动化或屏幕录制权限，相关功能需 Mac 侧真实权限。

Root 不自动解决云端模型权限、账号接口授权、厂商 GUI 代码总闸或应用签名检查。前期已有“越过 GUI 第一层后仍授权超时”的证据，不应承诺全量解锁。

## 复用前应修正的结果真实性问题

上游 `SystemAIProvider.swift:2419–2459` 的附件下载失败路径会调用本地合成函数：`2527–2539`、`2800–2805` 在缺少表格时写入固定示例收入数据；`2706–2784` 为本地绘制图片；`2815–2816` 会给其他扩展名写入纯文本。这些不能当作手机生成的原始文件。改造应明确失败并保持来源，避免替代数据冒充执行结果。

上游还存在 `ClawXProvider.swift:166–186` 的手机全网卡 TCP 代理及 Mac 凭据本地镜像等实现细节；本轮未启用。若后续复用，应改成配对认证的窄接口，不应把全部内部总线直接公开到局域网。

## 验收标准

- 手机停留在其他应用或关屏时，Mac 发起测试消息可在原生 Amoo 的同一会话获得真实回复；没有偷偷打开 Activity 或持续强制亮屏。
- 会话按真实 ID 同步，不以标题或相似问题拼接其他会话答案。
- Amoo 调用测试 Mac 工具能得到 Mac 本地独立核验的结果；不能只展示模型自报完成。
- 手机工具保持已验证的原生 AppFunctions 入口；无结果时报告实际失败，不伪造附件或成功码。

本轮完成的是源码评估与只读实机探测，不是上述后台及 Mac 执行器的交付。
