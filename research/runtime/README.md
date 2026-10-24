# AMOO 会话运行时观察

这里是被动观察源码，不是通用模型API。`observe-transport.js`观察测试标记对应的chat.send和同会话回包；`observe-news.js`记录真实搜索任务中的工具名、参数结构、状态、结果结构和文本长度；`signatures.js`只查看指定类的方法/字段签名。

需要与手机server版本匹配的Frida工具，以及frida-java-bridge与frida-compile。实际测试server版本17.23.2。源码依赖显式导入，不包含第三方生成bundle。

在独立环境中准备依赖并编译，例如：

```sh
npm install --no-save frida-java-bridge frida-compile
npx frida-compile observe-news.js -o observe-news.bundle.js
```

使用你已授权的Frida设备连接attach到com.stepos.ailauncher，在加载脚本后，向AMOO原生新会话发送真实查询，并包含脚本中的MARKER以限定观察范围。可先修改MARKER以区分新测试；不要把测试标记要求变成仅复述测试标记。观察器不主动调用模型、不改变参数或返回值。

手机server应只监听回环地址；工具连接使用ADB通道。完成后卸载脚本、detach并停止自己启动的server，移除自己的临时转发。不要关闭SELinux或改动应用授权校验。原始会话数据不应上传到本仓库。

具体成功证据见 [简单回传](../../reports/amoo-runtime-observation-2026-10-23.md) 和 [新闻任务](../../reports/amoo-news-roundtrip-2026-10-23.md)。这两次均由原生界面触发发送；无UI主动调用和Mac工具回传未完成。
