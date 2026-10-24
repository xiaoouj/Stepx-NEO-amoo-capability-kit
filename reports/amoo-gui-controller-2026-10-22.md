# AMOO GUI 自动化接入记录 — 2026-10-22

## 结果

已通过手机现有「Agent手机控制器」1.6.1/code11（`life.suk.agenttouch`）接通原生 AMOO 的基本 GUI 操作。系统直调、真实界面变化、AMOO 新会话自主调用三层均有证据。原厂 `gui_agent` 的禁用闸门没有修改。

用户手动开启无障碍后，health 确认 `serviceConnected=true`、`paused=false`、`systemAgentEnabled=true`。在原生全新会话，只给自然语言自检任务，AMOO 实际读取已安装技能，滚动首页、进入原生自检场、点击 +1、输入文本并回读。AMOO 最终报告计数 0→1。电脑独立 observe 确认同页计数为1、普通输入框内容精确为“AMOO 原生 GUI 20261022”。

此次流程耗时数分钟，发生多次重试，不能据此声称稳定通用控制已完成。中途 AMOO 打开过“管理允许应用”，未在弹出的授权窗口勾选或保存；允许列表仍仅包含本应用。视觉观察未开启，未调用 OCR，未操作密码框或发送联系人消息。

## 已验证的原生调用

| method | 参数 | 本轮证据 |
| --- | --- | --- |
| `agenttouch_health` | `{}` | 原生会话返回真实1.6.1健康状态 |
| `agenttouch_observe` | `{"expectedPackage":"life.suk.agenttouch"}` | 真实节点树、快照和页面状态 |
| `agenttouch_execute` | `{"requestJson":"完整协议JSON字符串"}` | launchApp、scroll、tap、inputText；输入回执 verified |
| `agenttouch_visual_observe` | `expectedPackage` | 命名规则候选，未调用，未验证 |

Android function ID 前缀为 `life.suk.agenttouch.AgentFunctionService#`，后缀分别 health/observe/execute/visualObserve。function ID 与 ability method 不可混用。

AMOO 的 ability 搜索三次漏收该应用；根据本机命名规则诊断性直调 health 成功后，再验证 observe/execute。空搜索不等于端侧无能力。旧本地 OpenClaw node 当前 disconnected，没有用它完成本次原生验证。

## 技能与实现

AMOO 自报已把包内 canonical 说明保存到自己执行环境的 `~/.openclaw/skills/agenttouch/`，实际映射 `/root/.openclaw/workspace/skills/agenttouch/`，包括5个规范文件和 DEVICE.md。后续新会话观察到读取文件并完成自检。这不等于手机本地同名路径，也未验证跨重启/系统更新持久性。

`tools/agenttouch_diagnostic.py` 是电脑 ADB 诊断客户端，支持 health/observe/execute；execute 使用真实健康检查设备 ID、唯一请求 ID、实际未来时间、保留请求正文，不自动重试。普通执行流程为 observe → 使用最新 snapshotId/nodeId 单步执行 → 重新观察确认。`partial/action_dispatched` 不能当作业务成功；failed/unknown 返回非零退出码。

目标应用必须由手机本人加入允许列表；没有修改私有配置绕过限制。视觉观察需另开授权，只做本地 OCR，返回文字与坐标，没有原图或通用图标理解。新开启其他应用后仍需分别测试，不保证所有自绘控件、系统安全页面或安装确认窗口都能自动操作。

## 测试中的问题

- 手机60秒自动息屏曾打断测试。临时延长为600000ms，收尾恢复原值；记录在 `screen-timeout-restore.json`。普通唤醒和 wm dismiss-keyguard 成功，无密码输入。
- 自检 Activity 不导出，普通 shell 直接启动被拒绝；通过 MainActivity 内真实按钮进入成功。
- 原生自带 open_app_by_package 打开 Amoo 返回10002“Launch intent not found”，按名称同样失败。诊断读取结果时用 ADB 显式 ChatMainActivity 恢复。
- 外部诊断在原生任务尚未完成时切回 Amoo，导致包不匹配并增加重试。后来停止切前台，原生继续完成输入。后续应等输入回执或业务完成后再切回读取对话，不能以诊断器超时推断模型停止。
- 旧 UI 桥未识别“正在处理中…”和 tv_tool_group_title 的“正在执行…”忙碌状态，误报 idle。已补充识别并成功构建；停止探针没有点击任何控件，没有实际取消原生任务。
- 已让 AMOO 追加本机实测记录与操作建议：不为找入口进入授权菜单、两次无进展停止、不无意义长按标题；只更新 DEVICE.md。

## 证据

本地目录 `.local/amoo/gui-controller-investigation/`：

- `guides/`：APK 中的规范原文；历史1.6.0验收仅属于作者记录。
- `direct-gui-selftest.json`：系统直调输入成功，和先前未完成回读的点击记录。
- `native-health-direct.json`、`native-install-followup.json`：原生方法和技能安装说明。
- `native-selftest-final-transcript.json`：原生最终自检报告。
- `native-selftest-final-observe.json`：独立界面回读，计数1和指定文本。
- `receipts-after-native-completed.json`：应用自身脱敏执行回执。原生输入 requestId=`selftest-input-1792682701449` 为 verified，计数点击 `selftest-inc2-1792682687908` 为 dispatched 后由界面回读确认。

REA Android provider 缺少所需 JAR，使用已有 Android SDK 和本地 DEX 解析辅助；这些输出不称为 REA Evidence。当前 ADB `PHONE_IP:ADB_PORT`。本轮没有刷机、重启、持久 Hook 或全局关闭安全检查。

收尾：息屏时间已核对恢复60000ms，ADB转发为空，health仍为服务连接、未暂停、系统调用已开启。AMOO曾把新增建议误写为历史无异常，并把自主验收写成未完成；已要求追加勘误，回读确认事实修正，见 `native-gui-skill-correction.json`。已更新能力目录状态，并通过 Java/D8 构建验证 UI 桥修改。
