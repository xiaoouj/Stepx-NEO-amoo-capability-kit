# AMOO 科技新闻任务：查询、工具回包与回答验证

2026-10-23，用户要求用真实问题替代简单标记复述，以验证AMOO会查询、处理返回结果并回答。通过原生新会话发送：

> 今天有哪些值得关注的科技新闻？请联网查询，选三条，分别说明发生了什么、为什么重要，并附上来源链接和发布日期。先确认今天的日期；如果查不到当日新闻，请如实说明。本次测试编号：AMOO_NEWS_20261023_N1。

## 实测结果

完整观察到：chat.send → 原生助手确认日期及查询说明 → web_search调用 → 工具结果回包 → 回答流式增长 → 最终回复及引用。不是只凭界面上的“正在思考”判断成功。

| 相对发送时间 | 可观察事件 |
| --- | --- |
| 0秒 | AgentBusBindingBase.p0发送chat.send，匹配本次测试编号并限定会话 |
| 约3.0秒 | session.message中的第一个web_search为PENDING，参数字段count、freshness、query |
| 约4.5秒 | 第一个web_search变为OK，出现result及stepclaw_references |
| 随后 | 继续发起查询，最终工具列表有4个web_search，全部OK且带结果 |
| 约21.0秒 | 最终session.message的回答文本长2049字符；工具列表仍是4项成功 |
| 约21.3秒 | 观察到stepclaw.reference事件 |
| 界面回读结束 | busy=false；最终回答有三条新闻、影响说明、来源和日期 |

4个工具结果分别带10、10、5、4项参考资料；这些数量不代表去重后的独立来源数。内部回包在同一会话中包含工具名、参数结构、状态和结果结构，证据强于上一轮简单复述标记。

本次没有获取隐藏思维链，也不据此判断具体内部推理算法。记录的是公开任务说明、工具事件和最后回答。未另行核实AMOO新闻内容的真实性，新闻正文不是本报告独立采信的事实。

## 证据与实现范围

- REA Evidence：ev_10a811722c1a24b18ce19f39068e8ac2f403e216c48b356da9bd82b44c2bece5。
- 本地完整脱敏Evidence：.local/amoo-news-2026-10-23/news-evidence.json，58条观察消息，messages_truncated=false。
- 摘要：summary.json；原生UI回读：technology-news.json、amoo-reply.md。
- REA Frida会话：ca9e802c-04dd-4a4a-8f52-777257125b68，原生AMOO PID7420。
- 临时脚本只观察带测试编号的发送，随后按sessionId/sessionKey过滤回复；保留原始调用参数及返回值。输出字段结构、工具名/状态和文本长度，没有输出密钥、认证头、无关聊天、完整工具结果或内部推理文本。

本次证明原生AMOO会话能承载真实搜索任务及结果回传。工具由AMOO现有执行链调用，Mac只是观察端。没有证明可以把这些内置工具替换为Mac文件/终端工具，也未完成自定义tools声明、Mac执行、工具结果回传模型的闭环。请求仍通过原生界面触发，直接无UI发送与后台生命周期未验证。

## 收尾

REA会话已关闭，临时观察脚本已卸载。临时amoo-news-frida服务按PID和路径核对后停止并删除；息屏超时恢复60000，SELinux保持Enforcing，ADB转发和Frida进程为空；AMOO及既有高级设置broker仍运行。未修改模型配置或桌面客户端设置。

收尾证据：.local/amoo-news-2026-10-23/cleanup-result.json。
