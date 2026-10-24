# AMOO 运行时通信观察：2026-10-23

## 结论

用户选择“2”：在原生 AMOO 内部观察发送前、接收后的通信，而不是先搭 HTTPS 中间人代理。本次已被动观察到可工作的原生会话发送和回复路径：

```text
AMOO 原生聊天
  → AgentBusCoreAgentClient.E(K6.n, callback, continuation)
  → ClawChannelAgentBusBinding / AgentBusBindingBase.p0
  → chat.send JSON 请求
  ← session.message / stepclaw.session.update / stepclaw.ui
```

这证明本机可以观察到应用内部可用的会话协议，不必先解密 HTTPS 才能继续调查。尚未通过该入口主动发起脱离界面的请求，也没有做成电脑可用的模型代理。不能把观察到回复等同于已经实现 WorkBuddy/Qoder 的 Mac 本地工具调用。

## 实测环境与范围

- 手机 ADB：PHONE_IP:ADB_PORT；原生包 com.stepos.ailauncher，PID7420；安装版本1.0.0_20261002_221513。
- 当前会话已有 Shell Root；SELinux 始终 Enforcing。
- REA Frida attach 会话：21657837-e4b4-4b66-9421-e068d0c14889。
- 使用本地已有官方 Frida17.23.2 Android arm64 server，临时运行在手机127.0.0.1:27042。
- 临时脚本保留原始参数、返回值和调用次数；没有修改客户端身份、授权校验、TLS/证书逻辑或模型配置。
- 原生新会话只发送自测标记，要求不使用工具、不操作设备、不修改文件。既有 UI 辅助器仅用来触发这几次受控测试，没有交付新的 UI 代理。
- 输出限制为类名、方法名、字段名、长度、是否含测试标记；未输出认证头、密钥或无关聊天正文。

## 可核对的运行证据

1. 原生聊天入口命中 AgentBusCoreAgentClient.E，内容元素类为K6.i$h。其他候选客户端已挂钩但本次未观察到命中；不据此断言它们从不使用。
   - REA Evidence：ev_0f64819d2931d5bf71f5459ee13559229803f5858c258652693a7770df8658c3。
   - 脱敏摘录：.local/amoo-runtime-2026-10-23/route-evidence.json。
2. 在AgentBusBindingBase.p0捕获含AMOO_RUNTIME_20261023_A5的受控发送，method为chat.send。
   - JSON外层字段：type、id、method、params。
   - params字段：traceId、agentId、idempotencyKey、stepclaw、_meta、sessionId、text。
   - _meta.trace字段：traceId、spanId。
   - 记录帧字符数857；未保存这些字段的实际值或完整请求。
3. 在ClawChannelAgentBusBinding.r中，仅对上述请求同一sessionId/sessionKey的回包记录结构，观察到：
   - stepclaw.session.update：sessionId、runId、seq、label。
   - session.message：sessionId、text、role、replyTo、toolCalls、metadata；部分帧含delta。
   - stepclaw.ui：sessionId、runId、seq、actions。
   - session.message中观察到测试标记；原生界面独立回读最终回复恰为AMOO_RUNTIME_20261023_A5。
   - REA Evidence：ev_945c5bdadcbc8a902eb055e92e1b12d336b47da4e30cf3bed6f8ae67a802731d。
   - 脱敏记录：.local/amoo-runtime-2026-10-23/transport-evidence.json；UI回读：marked-chat-v5.json。

部分回包结构被观察两次，可能包含重复分发或协程恢复，不将事件记录数当作独立生成次数。前几版标记识别没有匹配或观察脚本报TypeError，仅属于探针覆盖/实现问题；不能作为模型或接口拒绝的证据。改为在实际传输边界按测试标记和会话过滤后，才取得明确的发送与回复关联。

## 静态支持与局限

本地APK的DEX检查支持：ClawChannelAgentBusBinding.M构造chat.send；AgentBusBindingBase.p0委托l0；Binding实现含Android LocalSocket连接。静态输出在本调查目录chatapi-static.txt、channel-static.txt及旧background-wire-and-binding.txt。静态DEX解析是本地工具结果，不冒充REA Evidence。

本次运行时没有观察完整的服务端网络链路，不能据此认定最终模型在手机芯片本地推理，也没有确定其云端模型供应商。不能把“复用手机已有AI服务”直接描述为“复用手机本地算力”。

当前看到的是AMOO会话协议。请求并不是已验证的标准messages/tools模型接口；回复出现toolCalls字段，也不证明可以由电脑声明任意工具并接收标准tool_calls。此次明确要求不调用工具，没有执行Mac文件或终端命令。

之前18791的/chat/completions仍是已验证的403/code10014；本次内部会话可用不等于该代理已恢复。没有修改或绕过该接口的服务端授权。

## 后续验证顺序

1. 在AMOO现有授权客户端内，以受控新会话验证一轮直接发送与回复关联，避免复制密钥或伪造其他应用身份。
2. 验证不依赖界面、前台、亮屏的生命周期；仅发现内部入口不足以承诺后台常驻。
3. 验证自定义工具声明、结构化工具请求、由Mac执行、工具结果回传的闭环。若该会话协议不开放工具替换，应明确其只能承载AMOO会话，不能当作通用编码模型。
4. 上述成功后再考虑适配WorkBuddy等客户端，不能仅把chat.send返回的文本包成HTTP就宣称编码工具已接通。

## 收尾

REA会话已关闭并卸载全部临时脚本。按拥有的PID及可执行文件身份核对后停止临时Frida服务，删除本轮server/pid/log文件。最初SIGTERM后短暂仍有进程，随后独立确认退出再清理。

screen_off_timeout从600000恢复60000；当前ADB转发为空、Frida进程为空，SELinux Enforcing；AMOO PID7420仍在，原先高级设置broker PID20969仍存活。没有停止用户已安装的设置工具服务，没有修改WorkBuddy/Qoder/Codex配置。

收尾证据：.local/amoo-runtime-2026-10-23/cleanup-result.json。
