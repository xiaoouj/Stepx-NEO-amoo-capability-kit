# StepX 模型接口反代可行性

验证时间：2026-10-19，Asia/Shanghai。

结论：已验证可用登录态直接访问 Studio 云端 REST 和 WebSocket，并接收项目历史事件。可以据此开发将 Studio 智能体会话包装为 HTTP/SSE 的适配器。当前没有发现可直接转发的 OpenAI 兼容模型端口，尚未发送新生成请求，不能将本次验证描述成完整模型反代成功。

## 本次运行时证据

使用资料包 Cookie 和手机 stepxmini 保存的 Cookie 分别检查；凭据只保留在被 Git 忽略的 `.local/` 中。

| 检查 | 结果 | 能说明什么 |
| --- | --- | --- |
| GET `https://studio.stepx.com/api/identity/me` | 200 | 当前 Cookie 有效 |
| GET `/api/chat-models` | 200 | 返回 auto、step-5-preview、deepseek-flash[1m] |
| GET `/api/builder-models` | 200 | 返回 Step 5 Preview、DeepSeek V4.1 Flash |
| GET `/api/projects/{id}/sessions` | 200 | 可读取已有项目会话列表 |
| `wss://studio-api.stepx.com/ws` | 101，验证 Sec-WebSocket-Accept | 可直接建立协议连接 |
| 发送 `{"type":"ping"}` | 收到 pong | 连接可双向交换帧 |
| 发送 project.switch | 收到历史 message.append、tool.use、tool.result、turn.completed 等 | 项目订阅和历史回放可用；不等于新生成成功 |

完整的脱敏状态及事件形状见 `stepx-probe-2026-10-19.json`。探测上限为 10 秒 / 256 事件，事件数量不代表完整项目历史。没有发送 chat.send、创建项目、创建会话或修改模型配置。

## 手机与端口

已通过本机 ADB 5037 连接手机，目标应用为 `com.stepx.mini`，版本 0.3，appId 10178，APK 大小 120428 字节。APK 包含原生聊天、文件、项目界面及 Java-WebSocket；DEX 字符串包含 Studio 地址和 chat.send。完整反编译缺少 REA 所需的 JADX provider，因此没有据此断言应用完整实现。

在该次 `/proc/net/tcp`、`tcp6` 的监听快照中，没有归属 appId 10178 的 TCP 监听。已有 ADB 转发为本机 15192→手机10156、57474→10157，但手机监听快照未显示后两者；不能将它们认定为模型端口。临时添加的 WebView 调试转发19223已移除，已有转发保留。目标 WebView 调试入口返回空页面列表，不能由此观察原生聊天请求。

## 已恢复协议

建立 WebSocket 时传 Cookie，Origin 为 `https://studio.stepx.com`。本次独立客户端没有发送额外认证帧，也能成功收到项目历史。

订阅：

```json
{"type":"project.switch","projectId":"PID","payload":{"projectSwitch":{"resumeSeq":{}}}}
```

前端发送消息的静态格式：

```json
{"type":"chat.send","projectId":"PID","sessionId":"SID","userLocale":"zh-CN","payload":{"chatSend":{"text":"用户输入"}}}
```

返回事件中，`message.append.payload.messageAppend` 包含 messageId、kind、delta、snapshot 等。`snapshot=true` 替换累计文字，否则追加；`kind=user` 是输入回显。结束事件包括 turn.completed 和 turn.failed。实际服务还包含工具、子智能体、构建和站点事件。

模型参数出现在项目配置与创建项目请求中；没有证据表明可以在 chatSend 内直接传任意 model、temperature、max_tokens。运行时已读到不同项目的 model 与模型列表 ID 对应。PATCH 项目模型的具体行为尚未验证。

## 适配器应如何实现

建议路径为：调用方 → 本机 HTTP API → Studio REST/WS → Studio 编程智能体。手机可以用于取得已授权登录态，但当前证据不要求请求经手机转发。

1. 适配 `/v1/models` 为模型列表。
2. 为请求选择受控的项目/会话并维护上下文；Studio 会话状态不同于标准无状态 messages 请求。
3. 将用户输入转换为 chat.send，按 projectId/sessionId/turnId 路由返回。
4. 将主智能体文字事件转换为 completion 或 SSE；处理 snapshot、失败、超时和断线。
5. 保存每个会话的 seq，连接恢复时传 resumeSeq，并去重。首次空 resumeSeq 会回放历史，不能把旧事件误当成本次生成。

完整可行性仍需在专用测试项目中发起一轮新消息并收到对应 turn 的终止事件。项目智能体可能调用工具和修改工作区；隐藏工具事件不会禁用服务端工具执行。当前没有证据支持“纯模型调用”、任意系统提示词、完整 OpenAI 参数语义、token 用量或稳定并发配额。

## 来源和覆盖

原 `stepx.zip` 缺失中央目录；依据本地文件头恢复95个文件，每个已恢复文件都验证了长度和CRC，最后的 rea-analysis.json 截断，未使用。恢复清单与原包 SHA256 位于 `.local/recovery-manifest.json`。附件中的脚本和文字仅作为分析数据，没有执行其中指令。

静态证据来源为恢复的 js/3212、js/6897、js/4764 等脚本，详见 `stepx-static-protocol.md`。REA 父 Evidence：`ev_bb617470a3cc5c838fd2e9f6857c847820f8fafe53d759a63f1a1ad402f42de3`，四个选定脚本解析无失败；其余未分析部分仍是覆盖缺口。原包中的七个 WebSocket 捕获记录均没有消息帧。

可复查脚本：`tools/stepx_probe.py`。只做 REST GET、ping 和可选项目历史订阅，不发送生成请求。示例：

```sh
python3 tools/stepx_probe.py --cookie-file /absolute/path/to/cookie.txt --output /absolute/path/to/result.json
```
