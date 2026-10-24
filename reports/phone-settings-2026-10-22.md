# 设置索引与直达实机验证 — 2026-10-22

后续更新：本报告中的“原生路由尚未接通”已由新增 AppFunctions 应用解决。新的原生会话实测和准确边界见 `reports/amoo-phone-tools-2026-10-22.md`，不要据本报告的早期状态重复判断为当前仍未接通。

## 当前结果

手机本地的设置索引和执行服务已安装，并验证可用；原生 Amoo 的自然语言路由尚未接通。没有“释放所有权限”，也没有清空原厂规则、停用签名检查、替换系统 APK 或切换任何设置开关。

USB 序列号 `DEVICE_SERIAL`，型号 PZ261A。Shell 的 `su -c id` 返回 UID0、KSU context，SELinux Enforcing。检查时开发者选项已开（development_settings_enabled=1），无线调试关闭（adb_wifi_enabled=0）。昨天的 Wi-Fi 端口已不再可用。

## 已实机验证

1. 普通 ADB Shell 使用 `android.settings.APPLICATION_DEVELOPMENT_SETTINGS` 打开开发者选项，焦点为 `Settings$DevelopmentSettingsActivity`。
2. 无线调试在 `SubSettings` 的 `WirelessDebuggingFragment`。普通 Shell 启动明确报缺少 `com.stepos.permission.SEARCH_SETTINGS`。Root 启动成功；UiAutomator 树独立确认标题“无线调试”、使用开关 checked=false。验证后删除临时 UI XML，没有保存页面上的设备地址或账户信息。
3. 已实现 `phone-settings/android/SettingsBroker.java`。服务只监听手机 `127.0.0.1:18819`，使用随机 Bearer 密钥，提供登记页面启动、home/back/recents 和 shutdown，不接受任意 Shell 命令。
4. 普通 UID1000 的本机客户端经服务打开开发者选项、输入法、电池健康度；每次 am 返回成功且窗口焦点为目标组件。无线调试经本机 OpenClaw Gateway 的 `stepclaw.system.run` 调用成功。home/back 命令成功，未登记 id 被拒，无认证请求返回401。
5. APK 静态索引包含519个 Activity/alias、2,451条有标题的 XML 项目，扫描515个 XML。类别、标题也计入项目；它们不是全部可直达页面。登记并验证的直达路由目前为4个。

## 原生 Amoo 验证与新发现

共使用3个新建测试会话，没有修改已有聊天、替换草稿或给外部联系人发消息。

- 纯自然语言“打开无线调试设置页面，只打开页面，不要切换开关”：Amoo 连续搜索内置原子能力后回复无入口，没有调用新服务。
- 明确要求读取新增工作区技能并执行绝对路径脚本：Amoo 报对应目录不存在，列出的工作区技能与手机本地目录不同，没有调用服务。
- 受限运行环境诊断：Amoo 报脚本 UID0；`/sdcard`、`/mnt/openclaw/rootfs`、`/root/.openclaw/workspace/phone-settings` 三个路径均不存在；访问 `127.0.0.1:18819/health` 连接被拒。作为对照，同一时段手机本机 UID1000 能访问该服务，不带认证得到401。

这些是 Amoo 测试会话的可见工具结果/回复，以及独立本机验证。它们支持“原生脚本执行环境与手机本地 OpenClaw 不共享这些路径和回环服务”。Amoo 自称其脚本在云端；本轮未独立取得远端运行环境的部署信息，因此不把“具体云服务归属”当作已证事实。也不能把诊断中的 UID0 解释为 Amoo 已取得手机 Root。

昨天原生会话能完成测试文件读写及计算，仍然成立；本轮补充说明，不能由那些文件结果推断每个 Python 工具都在手机 chroot 里运行。

## 实际安装改动

- 新增手机本地工作区 `phone-settings/`：JAR、Python 客户端、索引、4个路由、私有认证配置、运行日志及 PID。
- 新增工作区 `skills/phone-settings-direct/SKILL.md`。
- 保留原有 TOOLS.md，在手机 `.phone-settings-backups/` 先备份，再追加有界标记说明。
- 未改 AGENTS.md、openclaw.json、原厂插件、原子能力注册表或系统包。
- 收尾只读核对时，AGENTS.md 当前 SHA-256 为 `3d2ae66dbc1d461795c48b4c8bf0435188503f760cfb981cd9031e98fc19e212`，与昨天留存值不同。本轮安装器没有写该文件，变化来源未调查；不能称整个文件自昨天起未发生变化。
- 服务当前保留运行供后续接入；这是用户功能服务，不是临时 Frida Hook。没有安装开机项。手机重启后需恢复临时 Root 并重新启动/安装服务。
- 昨天的 Frida 服务进程本轮检查已不存在；本轮未使用 Frida。已移除 Mac 上过期连接密钥元数据。原生 UI 测试桥每次结束均关闭并移除自己的 ADB 转发。
- 收尾仍有两条非本轮创建的 ADB 转发指向10156/10157，未擅自移除；本轮 UI 桥指向18809的转发已移除。

## 后续应做什么

优先调查原生 Amoo 实际使用的端侧工具调用通道，确认能否登记自有能力或启用本地 OpenClaw 客户端路由；不要继续把修改手机工作区文件当作原生 BFF 全链路解锁。若原生端不支持可扩展工具，应由可配置助手调用这个本地服务。

已验证的 Root 页面入口可以保留和扩展；其余索引项目需要组件、参数、运行时可用性验证，不能按名称猜路径或承诺“所有设置全覆盖”。单个开关与独立页面也须区分。

## 证据位置

- `.local/amoo/settings-index/first-live-probe.json`：普通 Shell/Root 启动对比。
- `.local/amoo/settings-index/wireless-visible-check.json`：仅保留目标标题与开关节点。
- `.local/amoo/settings-index/broker-smoke.json`：本机 UID1000 客户端结果。
- `.local/amoo/settings-index/native-settings-probe-result.json`：自然语言未采用新工具。
- `.local/amoo/settings-index/native-runtime-diagnostic-result.json`：执行环境诊断的原生回复。
- 显式技能指引测试的原生回复已在本聊天工具输出中记录；后续诊断覆盖了同名本地结果文件，没有独立完整副本，不补造证据。
- `phone-settings/data/settings-index.json`：包含 APK 版本和 SHA-256。

Android 公共入口参考：[Settings API](https://developer.android.com/reference/android/provider/Settings)；无线调试 Fragment 的系统实现参考：[AOSP WirelessDebuggingFragment](https://android.googlesource.com/platform/packages/apps/Settings/+/master/src/com/android/settings/development/WirelessDebuggingFragment.java)。厂商权限限制与当前页面成功结果以本机证据为准。
