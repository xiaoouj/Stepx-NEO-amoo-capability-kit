---
name: phone-settings-direct
description: "直达本机系统设置子页面、开发者选项、无线调试、输入法设置、电池健康度；查询设置项目索引；退出当前应用到桌面、返回上一页。"
---

使用本机已安装的设置工具。调用脚本执行工具（exec 或平台实际提供的 stepclaw.system.run），运行：

`python3 /root/.openclaw/workspace/phone-settings/phone_settings.py <操作>`

脚本执行的工作目录设为 `/root/.openclaw`。当前 stepclaw-exec 的 cwd 白名单不包含 workspace；脚本使用绝对路径，不需要切换到该目录。若调用 stepclaw.system.run，参数示例为：

`{"command":["python3","/root/.openclaw/workspace/phone-settings/phone_settings.py","open","wireless-debugging"],"cwd":"/root/.openclaw","timeoutMs":20000}`

- `routes`：列出已登记的页面直达入口。
- `search "关键词"`：查询本机设置索引，包含设置页面和页面内项目。
- `open developer`：开发者选项页面。
- `open wireless-debugging`：无线调试页面。
- `open keyboard`：输入法设置页面。
- `open battery-health`：电池健康度页面。
- `home`：返回桌面（用户说退出应用且没有要求强制停止时）。
- `back`：返回上一页。
- `recents`：最近任务。
- `health`：查看服务是否可用。

这些动作已由本机工具实现，不需要调用已关闭的 gui_agent，也不需要猜原子能力名称。用户要求打开页面时直接执行；打开页面与开启开关是不同操作，本工具不会切换设置。若用户要求切换开关，如实说明本工具暂只直达页面。

以工具实际输出为准，失败时明确报告。`ok=true` 表示命令已执行；focus 是活动窗口验证，pageContentVerified=false 不代表已经检查过页面内所有内容。索引里的 static_candidate 仅说明 APK 包含该项，不能承诺能直接打开。不要打印或发送本目录的 .broker.json 内容。
