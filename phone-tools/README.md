# AMOO 本机工具 0.2

已安装的 Android 应用 `com.amoo.executor`，通过系统 AppFunctions 暴露真实手机操作。AMOO 原生聊天可通过现有 `ability.call` 访问，避开云端脚本看不到手机文件/回环服务的问题。

手机端应用以普通应用 UID 运行，没有获得 Root。两类能力分别依赖：

- **设置直达与系统导航**：通过已配对的 Root 执行服务完成，服务监听手机回环地址，认证密钥保存在应用私有目录，不经云端模型或电脑输出。
- **截屏、UI 节点读取、点击、滑动、文字输入**：通过应用内 AccessibilityService 完成。这个服务由用户在系统中一次性启用，不需要 Root，也不依赖电脑。

AppFunctionService 由系统绑定权限保护，另限制为本机已知 AMOO 能力进程及 ADB Shell 调用。

## 可调用接口

| ability.call method | params | 作用 |
| --- | --- | --- |
| executor_open_settings | {"page":"wireless-debugging"} | 无线调试页面 |
| executor_open_settings | {"page":"developer"} | 开发者选项页面 |
| executor_open_settings | {"page":"keyboard"} | 输入法设置 |
| executor_open_settings | {"page":"battery-health"} | 电池健康度 |
| executor_navigate | {"action":"home"} | 返回桌面 |
| executor_navigate | {"action":"back"} | 返回上一页 |
| executor_navigate | {"action":"recents"} | 最近任务 |
| executor_search_settings | {"query":"无线调试"} | 查询已保存的设置索引 |
| executor_get_tool_status | {} | 检查执行服务与无障碍状态 |
| executor_screenshot | {} | 截屏并保存为 PNG，返回 URI/路径/尺寸 |
| executor_ui_dump | {} 或 {"maxNodes":"200"} | 读取当前界面控件树与屏幕坐标 |
| executor_tap | {"x":"540","y":"1200"} | 点击屏幕坐标 |
| executor_swipe | {"x1":"540","y1":"1800","x2":"540","y2":"600","durationMs":"300"} | 滑动屏幕 |
| executor_input_text | {"text":"你好"} | 向当前输入框写入文字，最多2000字符 |

打开设置页面不切换开关；home 不强制停止进程。接口目前只允许已登记的四个设置页面，索引内其他静态候选不承诺可直接打开。查询返回最多20条并提供总匹配数。打开 App 继续使用原生 `open_app_by_package` / `open_app_by_name`。

返回中的 `content.returnValue` 为 JSON 字符串，需解析并检查其中 `ok`。系统 AppFunction 的“请求成功”本身不代表内部手机动作成功。`executor_get_tool_status` 的 `service` 表示设置/导航执行服务，`accessibility` 表示截屏/UI/手势/输入能力。

`executor_screenshot` 把图片保存到 `Pictures/AmooTools`（媒体库拒绝时退回应用外部目录），只返回 URI、手机路径、宽高和字节数，不返回图片内容，也不上传。`FLAG_SECURE` 页面会截屏失败。`executor_ui_dump` 的 `bounds` 为屏幕坐标 `[left,top,right,bottom]`，优先使用 `active=true` 的 application 窗口；达到 `maxNodes`（默认200，最大1000）时 `truncated=true`。`executor_input_text` 会回读输入框，返回 `verified`；`verified=false` 表示动作已被接受但未确认写入结果，密码框跳过回读。

## 无障碍服务

五个无 Root UI 工具需要系统启用 `com.amoo.executor/.AmooAccessibilityService`。服务配置声明了读取窗口内容、执行手势和截屏，属于无障碍工具类型；仅在 AMOO 调用时读取当前界面，不记录、不上传屏幕内容。

安装时加 `--enable-accessibility` 会保留手机中已有的无障碍服务，把本服务追加进启用列表，并打开 `accessibility_enabled`。也可以手动在系统“无障碍”设置中开启“AMOO 本机工具”。

## 安装与恢复

使用工作区已有 Android SDK37 和 JDK17，不需要 Gradle 或下载第三方运行库：

```sh
python3 phone-tools/install.py --serial PHONE_IP:ADB_PORT --enable-accessibility
```

安装器会检查 Shell Root 和本机执行服务；服务不可用时调用 `phone-settings/install.py` 恢复服务，再编译、安装、在手机内部配对并可选启用无障碍。生成的 APK 为 `phone-tools/dist/Amoo-Phone-Tools-0.2.0.apk`。签名私钥保存在忽略目录 `.local/phone-tools-signing.keystore`，更新时保留该文件。

2026-10-23 起，恢复执行服务使用 `--service-only`，不往手机本地工作区写入旧的设置技能或修改 TOOLS/AGENTS。原生能力保持优先，本工具用于用户指定、原生确实缺少入口、调用失败，或需要直接读取/操作界面时补充。安装关闭 incremental 模式，避免本机系统拒绝该安装模式后等待回退。

电脑只用于安装/恢复。日常 AMOO → AppFunctions → 手机执行服务不依赖电脑 ADB 连接。设置直达与导航依赖临时 Root，重启后可能需要重新恢复 Root 并运行安装器；无障碍 UI 工具不依赖 Root。本工具未修改启动镜像，也未配置开机服务。

如果单独重装 `phone-settings` 导致认证密钥轮换，需再次运行本安装器给应用重新配对。新增路由需同时更新 `phone-settings/data/routes.json`、重启对应服务并重建本应用。

## 让原生 AMOO 会使用

系统登记只解决“可调用”，模型还需要正确的方法说明。已在原生 AMOO 自身执行环境把 `skills/amoo-phone-tools/SKILL.md` 更新到 0.2.0，并在全新会话实测它实际调用了截图、UI dump、点击、滑动、输入和状态检查；手机应用日志独立确认 `caller=com.stepos.ability`。AMOO 自报保存路径 `/root/.openclaw/workspace/skills/amoo-phone-tools/SKILL.md`，但该路径在手机根文件系统不可见，未取得逐字原文；`skill/SKILL.md` 是可重建的标准说明。若原生模型更换运行环境或技能丢失，向它提供本目录技能说明，要求实际调用健康检查并保存为新会话可见技能，然后重新验证。不要把手机本地 OpenClaw 的工作区当作原生模型全部执行环境。

## 扩展入口

新增有限动作可扩展 `PhoneToolsService`、`AmooAccessibilityService` 与元数据生成器 `generate_metadata.py`，重新构建安装；需要 Root 的动作另在 `phone-settings` 的执行服务中实现，参数和成功标准应明确。当前没有任意 Shell 命令、任意文件读写或消息发送接口；UI 工具只返回当前界面内容给已允许的调用方。

标准接入机制参考：[Android AppFunctionService](https://developer.android.com/reference/android/app/appfunctions/AppFunctionService)、[AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)。实际兼容性以本机安装、系统登记、节点调用和原生会话验证为准。AppFunctions 索引 schema 为 AOSP Apache2.0 文件，见 NOTICE 与 LICENSE-APACHE-2.0。
