# AMOO 语音安装应用：调用路由修复 — 2026-10-22

## 已完成

用户的原话是“帮我去应用市场安装 QQ 音乐”。已通过手机自带应用商店下载安装 QQ音乐，再由原生 AMOO 在新会话中以同句自然语言复测，正确调用安装接口并返回“已安装”。本轮通过文字输入复现语音识别之后的执行链路，没有注入麦克风音频或宣称完成独立的语音识别测试。

- 无线 ADB：`PHONE_IP:ADB_PORT`，mDNS 对应设备 `DEVICE_SERIAL`。同一设备广播了其他端口，本轮使用46781成功连接。
- QQ音乐包名：`com.tencent.qqmusic`。
- 安装版本：`20.9.5.8`，versionCode7508。
- Android PackageManager 确认 installerPackageName 和 initiatingPackageName 均为 `com.stepos.appstore`。
- 商店日志确认 `INSTALL_SUCCESS`。没有通过 adb install 代替商店下载安装。

## 根因与修复

初次用同句指令复现时，AMOO 只调用 `appstore_search_app`。商店日志显示 `searchApp: keyword=QQ音乐`、`KEY_AUTO_INSTALL=false (普通搜索), skip`。AMOO 随后误称系统没有可以执行安装的能力。

商店APK `assets/app_functions.xml` 与 `app_functions_v2.xml`，以及运行中的 `cmd app_function list-app-functions`，均存在并声明默认开启：

- `com.stepos.appcenter.appfunctions.AppStoreFunctions#searchApp`
- `com.stepos.appcenter.appfunctions.AppStoreFunctions#installApp`

Android AppFunction 的 functionId 与 AMOO 的 ability.call method 是不同层级的名称。只将裸 `installApp` 作为 method 会返回20004；正确的端侧 method 是：

```json
{"method":"appstore_install_app","params":{"appName":"QQ音乐"}}
```

直接经手机现有 OpenClaw 节点的 ability.call 验证成功，触发商店自动下载安装；无需开启通用 GUI、取得额外安装权限或修改系统 APK。

随后在原生 AMOO 新建测试会话中，告知已验证的正确 method，并要求保留原笔记、为后续会话保存工具说明。AMOO 实际调用该方法，商店记录 installApp 被调用且目标已安装。AMOO 自报新增 `workspace/skills/appstore-install-app/SKILL.md`，原工具笔记受保护未编辑。该技能文件处于 AMOO 自己的执行环境，不是此前手机 chroot 的工作区；本轮未取得其完整原始文件副本。

再次新建会话，仅发送“帮我去应用市场安装QQ音乐。”：可见先读取文件，再执行调用；商店新增日志如下，与之前的调用有不同时间戳：

```text
10-22 21:21:11.883 AppStoreFunctions: installApp: name=QQ音乐
10-22 21:21:12.693 AppStoreFunctions: installApp: already installed, skip navigation, pkg=com.tencent.qqmusic, keyword=QQ音乐
```

AMOO 回复：“「QQ音乐」已经装好了，应用市场返回状态为已安装。可以直接打开使用。”

这验证了新会话自然语言已路由到安装接口；由于QQ音乐此前已装好，没有为了重复测试而卸载，也没有安装用户未指定的其它应用。新技能跨重启/服务升级后的持久性尚未验证。

## 修改和保留内容

- 手机通过自带商店新增QQ音乐，用户未被登录任何QQ音乐账号，也未同意其首次启动协议；本轮没有打开它。
- 由AMOO新增自己的安装能力技能笔记；原有受保护工具笔记未改。
- 新建数个AMOO测试会话以复现与验证，保护已有草稿和正在生成的会话。
- 未改 AGENTS.md、系统权限、签名校验、GUI闸门、商店APK、安装安全设置或全局AppFunction enabled状态。
- 原生UI测试桥均在finally关闭，并移除本轮创建的端口转发；保留无线ADB连接。

## 证据

`.local/amoo/store-investigation/`：

- `appstore-manifest.txt`、`app_functions.xml`、`app_functions_v2.xml`：实机APK元数据。
- `app-functions-raw.json`、`app-function-service-dump.txt`：系统登记数据，属于能力元数据，不是用户聊天或凭据。
- `store-log-filtered.txt`：初始调用搜索接口并跳过安装。
- `direct-install-call.txt`：正确method的节点调用结果。
- `install-log-filtered.txt`：下载/安装成功日志，URL已移除。
- `native-install-bare-method-result.json`：裸 installApp 返回20004的原生回复。
- `native-install-correct-method-result.json`：正确调用及AMOO自报技能保存位置。
- `native-install-fresh-chat-retry-result.json`、`fresh-call-log.txt`：新会话自然语言复测及独立商店日志。

一次新会话测试因当前前台切到桌面导致UI桥拒绝读取，未发送请求；清理后重试成功，不能将该次409理解为安装接口失败。

## 对能力扩展的意义

本例是找回并正确路由已有能力，不是新增执行代码。以后可先检查应用是否已提供 AppFunctions 和真实method；已有能力通过正确技能说明接入即可。对于系统设置、返回等尚缺入口的能力，需补执行端、可调用注册与说明，并做原生新会话验证。普通安装一个应用，不代表该应用自动向AMOO提供所有GUI操作。
