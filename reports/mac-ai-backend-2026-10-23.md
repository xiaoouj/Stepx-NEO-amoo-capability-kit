# 手机 AI 接入 Mac 编码客户端：目标与接口核查

用户要复用手机已有 AI 服务，在 Mac 上通过 WorkBuddy、Qoder 或 Codex 运行本地文件/终端工具、完成编码项目；不要求固定客户端。不是只从电脑遥控手机聊天界面。

## 客户端和协议

WorkBuddy 官方模型配置支持自定义 URL、API Key、模型名及工具调用能力，适合先验证 Chat Completions 路线：
https://www.codebuddy.ai/docs/zh/workbuddy/From-Beginner-to-Expert-Guide/Function-Description/Model

当前 Codex 官方配置只支持 Responses wire API，不能把 Chat Completions 地址直接视为兼容：
https://learn.chatgpt.com/docs/config-file/config-reference

Mac 已安装 WorkBuddy，未发现 `/Applications/Qoder.app`。没有修改任何客户端配置、启动假模型服务或添加不可用模型条目。

## 本轮真机结果

手机 ADB `PHONE_IP:ADB_PORT`。Root 只读 ss 确认 StepClaw 进程监听127.0.0.1:18791；OpenClaw网关监听18789。默认模型配置仍是stepos/stepos-model-auto。

临时ADB转发到18791，发送无工具、仅要求回复随机测试标记的请求：

- GET /health、GET /v1/models 返回405。
- POST /chat/completions 返回HTTP403、code10014、msg“该应用无权访问此接口”。
- 随后在手机chroot内部，按其已有stepos:default认证profile进行正常带Authorization请求；密钥没有打印、导出或存入电脑。配置baseUrl对应的/v1/chat/completions返回404“No static resource v1/chat/completions”。校正到/chat/completions再次请求，仍返回403、code10014及相同错误。

上述结果表明该模型代理当前未提供可用生成结果，并不证明原生AMOO的所有后台会话通道都不可用，也不能据此断言整个方向不可能。没有尝试绕过服务端接口授权。

## StepBridge 的实际差异

本地源码参考提交df5dd95bd34cb7e3fea618ce5153ccb0e1111049：

- APIGatewayService.swift 提供 /v1/chat/completions 和 /v1/messages 的HTTP包装。
- step-3.5-flash-2603、MiniMax-M2.7 路线使用各自上游API Key；密钥可来自环境变量、本地文件或手机中对应的stepfun:default/minimax:default profiles。
- 手机当前profile仅发现stepos:default，没有对应StepFun/MiniMax profile；只检查ID、provider、type和有无密钥布尔值，没有读取输出密钥值。
- stepos-model-auto 尝试手机代理，失败会回退StepFun路线。
- clawphone-agent调用ClawXProvider.sendMessage，不能直接等同于原生AMOO。
- 路由还允许电脑使用有效密钥直接请求云端。API成功响应本身不能证明正在复用手机AMOO。

用户追问“需要手机前台的方案与StepBridge有什么区别”，已明确回答：没有本质区别，已有UI桥接可复用，不应作为新的后台/模型接口成果。

## 当前完成度与边界

已澄清目标、核对客户端接入协议、排查手机直接模型代理并验证当前权限错误。尚未取得一次成功的原始模型API响应，也未验证模型的标准tool_calls和回传工具结果，因此没有交付“手机模型驱动Mac编码工具”的实现。

前台UI方案曾作为可选兜底询问，用户只质疑它与StepBridge的区别，没有选择或批准该路线。本轮没有开始重做UI代理。原生AMOO的独立BFF会话通道仍需协议与后台生命周期验证。

本地脱敏证据 `.local/mac-ai-bridge-2026-10-23/`：listeners.txt、phone-proxy-probe.json、authenticated-proxy-probe.json、authenticated-proxy-correct-path.json、upstream-presence.json。临时转发均已移除。

## 后续更新：运行时已观察到内部会话请求和回复

用户选择运行时观察后，在原生AMOO内被动命中AgentBusCoreAgentClient.E，并在传输边界关联chat.send请求与含同一测试标记的session.message回复。这是可工作的内部会话路径证据，更新了前文只假设独立BFF通道的方向；不等于18791模型API已可用，也没有验证自定义Mac工具调用。详见 `amoo-runtime-observation-2026-10-23.md`。临时观察器及服务已清理，未交付UI代理或修改桌面客户端配置。
