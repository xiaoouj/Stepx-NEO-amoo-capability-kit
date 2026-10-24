---
name: amoo-phone-tools
description: "手机本机操作工具：打开无线调试、开发者选项、输入法设置、电池健康度，查询系统设置项目，返回桌面、退出当前应用到桌面、返回上一页、最近任务；并可在无 Root 下截屏、读取界面控件树、点击、滑动、向输入框写入文字。"
---

手机已安装 `com.amoo.executor`（AMOO 本机工具 0.2.0），九个 AppFunctions 已登记。设置直达与导航通过已配对的本机执行服务完成；截屏、UI 读取、点击、滑动、文字输入通过应用内无障碍服务完成。两者都不需要在云端 Python 中访问手机 127.0.0.1，也不需要调用关闭的 gui_agent。

恢复出厂后的原生能力优先：原生工具已经能完成的安装应用、系统设置、打开 App 和导航继续使用原生工具。本技能用于用户明确指定“本机工具/直达工具”，或者原生缺少对应高级页面入口、实际调用失败，或需要读取界面控件/点击/滑动/输入时的补充。不要替换原厂规则或让所有设置、导航强制走本工具。

实际调用形态为 `ability.call`，`method` 和 `params` 如下；使用你当前实际的 nodes/端侧exec工具包装，不把裸Java方法名当method。参数按字符串传递，数字型参数也可直接传数字：

| 意图 | method | params |
| --- | --- | --- |
| 打开无线调试页面 | executor_open_settings | {"page":"wireless-debugging"} |
| 打开开发者选项页面 | executor_open_settings | {"page":"developer"} |
| 打开输入法设置 | executor_open_settings | {"page":"keyboard"} |
| 打开电池健康度 | executor_open_settings | {"page":"battery-health"} |
| 返回桌面、退出当前App到桌面 | executor_navigate | {"action":"home"} |
| 返回上一页 | executor_navigate | {"action":"back"} |
| 最近任务 | executor_navigate | {"action":"recents"} |
| 查询设置索引 | executor_search_settings | {"query":"无线调试"} |
| 检查本机工具 | executor_get_tool_status | {} |
| 截屏 | executor_screenshot | {} |
| 读取当前界面控件树 | executor_ui_dump | {} 或 {"maxNodes":"200"} |
| 点击屏幕坐标 | executor_tap | {"x":"540","y":"1200"} |
| 滑动屏幕 | executor_swipe | {"x1":"540","y1":"1800","x2":"540","y2":"600","durationMs":"300"} |
| 向输入框写入文字 | executor_input_text | {"text":"要输入的内容"} |

推荐界面操作流程：

1. 先调用 `executor_ui_dump`，在返回的 `windows[].nodes[]` 中按 `text`、`description`、`hint` 或 `viewId` 找目标控件，取其 `bounds`（屏幕坐标 `[left,top,right,bottom]`）的中心点。返回里会同时有 application、input_method、system 窗口，优先使用 `active=true` 且 `typeName="application"` 的窗口；只对 `visible=true` 且宽高为正的节点点击，离屏节点可能出现负值或 `right<left`。
2. 调用 `executor_tap`，随后再次 `executor_ui_dump` 核对界面是否变化；不要仅凭点击已发出就声称成功。
3. 输入文字：先 `executor_tap` 点击输入框，再 `executor_input_text`；`text` 为空字符串可清空输入框。返回的 `verified=true` 表示已回读确认；`verified=false` 表示写入动作已被接受但约2秒内未回读一致，应报告“未确认”，不要声称输入成功。密码框的 `verified` 为 null（跳过回读）。输入内容不会记录或上传。
4. 打开 App 继续使用原生 `open_app_by_package` / `open_app_by_name`，不要用本工具模拟桌面点击来找 App。
5. 需要视觉结果时用 `executor_screenshot`；它只返回 `path`、`uri`、宽高和字节数，不返回图片内容。除非你能实际读取该路径，否则不要声称已经看过截图，界面文字优先用 `executor_ui_dump`。

`content.returnValue` 是 JSON 字符串，必须解析并检查其中的 `ok`。失败时报告实际错误，不声称完成。`executor_ui_dump` 返回 `nodeCount` 和 `truncated`；达到 `maxNodes`（默认200，最大1000）时节点树会被截断。`bounds` 可直接作为点击坐标。`executor_get_tool_status` 同时返回 `service`（设置/导航执行服务）和 `accessibility`（截屏/UI/手势/输入）两个状态，缺哪项就报告哪项。

无障碍服务必须已启用并连接；未连接时截屏、UI 读取、点击、滑动、输入会返回“无障碍服务未连接”类错误。可通过 AMOO 本机工具应用页面打开无障碍设置。截屏受 `FLAG_SECURE` 限制，安全页面会失败；滑动手势可能被系统手势区拦截。只操作当前可见界面，不要用本工具绕过系统权限、支付或密码确认，也不要替用户执行敏感确认。

设置直达只打开设置页面，不自行切换无线调试/开发者开关；`executor_tap`/`executor_swipe` 会按给定坐标操作，调用方不要对敏感开关、支付、权限确认执行未授权点击。`home` 是返回桌面，不强制停止进程。设置索引的静态候选不等于已登记直达入口，目前只登记上面四个页面。设置直达与导航依赖手机当前 Root 会话，重启后如报不可用需恢复 Root/服务；无障碍相关工具不依赖 Root。不要删除原有技能，不要修改其他规则。
