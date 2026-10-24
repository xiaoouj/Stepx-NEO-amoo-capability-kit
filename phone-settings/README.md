# Amoo 本机设置工具

2026-10-22后续更新：原生 AMOO 的调用通道现已通过新增 Android AppFunctions 应用接通，见 [phone-tools](../phone-tools/README.md)。下文保留本地执行服务的构建与早期验证边界；“原生路由尚未接通”描述的是该应用接入前的状态。

为 PZ261A 提供设置索引、已登记页面直达，以及返回桌面/上一页。Android 端服务使用已授权的临时 Root；手机本地 OpenClaw 环境可用普通 UID1000 执行 Python 客户端，通过手机回环地址调用服务。电脑只负责安装；服务启动后，本机客户端执行动作不经过电脑 ADB。原生 Amoo 聊天的脚本执行环境另有隔离，尚未接通这个服务。

## 已有入口

| id | 页面 |
| --- | --- |
| developer | 开发者选项 |
| wireless-debugging | 无线调试 |
| keyboard | 输入法设置 |
| battery-health | 电池健康度 |

打开无线调试的关键是向 `com.android.settings.SubSettings` 传入 `:settings:show_fragment=com.android.settings.development.WirelessDebuggingFragment`。本机该容器要求 `com.stepos.permission.SEARCH_SETTINGS`；普通 ADB Shell 被拒，Root 启动已通过窗口和页面标题验证。打开页面不等于开启无线调试。

## 本机调用

在手机本地 OpenClaw 的 Linux 执行环境内，工作目录使用 `/root/.openclaw`：

```sh
python3 /root/.openclaw/workspace/phone-settings/phone_settings.py search 无线调试
python3 /root/.openclaw/workspace/phone-settings/phone_settings.py open wireless-debugging
python3 /root/.openclaw/workspace/phone-settings/phone_settings.py home
python3 /root/.openclaw/workspace/phone-settings/phone_settings.py back
```

客户端另支持 `routes`、`health`、`recents`、`shutdown`。`open` 只接受 routes.json 内的 id；服务没有任意命令执行、任意文件读取、设置开关修改或任意 Intent 参数接口。

服务监听 `127.0.0.1:18819`，请求使用随机认证密钥。密钥仅保存于手机安装目录 `.broker.json`，文件权限 600，不应输出到聊天、日志或报告。接口为 GET `/health`，POST `/open`、`/navigate`、`/shutdown`。

## 设置索引与范围

`data/settings-index.json` 从昨天提取的 Settings.apk 与 MzBatteryManager.apk 生成，保留来源版本和 SHA-256。包含 519 个 Activity/alias 记录和 2,451 个带标题 XML 项目，扫描了 515 个 res/xml 文件。它们是静态候选，包含分类和页面标题，不是 2,451 个可直达页面，也不能代表系统动态生成的全部设置。

索引保留中文名称、组件名、Fragment、动作、声明权限、manifest enabled 状态、Preference key 和源 XML。运行时开关、覆盖资源、硬件支持、动态页面、额外参数仍需逐项验证。只向 routes.json 加入有明确入口和验证结果的页面；加入口后需重启服务，以重载路由。

在电脑上搜索静态索引可直接运行 `python3 phone-settings/phone_settings.py search 关键词`。重建索引：

```sh
python3 phone-settings/build_index.py \
  --aapt2 /Users/your-user/.workbuddy/binaries/android/android-37.0/aapt2 \
  --apk .local/amoo/Settings.apk \
  --apk .local/amoo/MzBatteryManager.apk \
  --output phone-settings/data/settings-index.json
```

## 安装与恢复

先通过 KernelSU 授权 Shell Root；手机无需解锁引导加载程序。USB 当前序列号为 `DEVICE_SERIAL`。

```sh
python3 phone-settings/install.py --serial DEVICE_SERIAL
```

安装器使用现有 JDK/Android SDK 编译，不安装 APK、不替换系统包。产物和客户端放在 `/mnt/openclaw/rootfs/root/.openclaw/workspace/phone-settings/`；增加工作区 `skills/phone-settings-direct/SKILL.md`，备份后向 TOOLS.md 追加有标记的工具说明。AGENTS.md、原始插件和原子能力目录保持原样。

只恢复执行服务、不写入工作区技能或修改 TOOLS.md 时，使用 `--service-only`。`phone-tools/install.py` 默认通过该模式恢复底层服务，原生 AMOO 的接入由 AppFunctions 和其自身技能完成，避免旧的本地工具说明影响原生能力优先的选择。

这是当前 Root 会话中的服务，不是开机服务。手机重启后临时 Root 和服务可能消失，恢复 Root 后重新运行安装器即可。升级可能改变设置入口，应重新提取、生成索引并验证。

## 验证边界

2026-10-22 已验证普通 UID1000 客户端能调用 Root 服务，四个页面命令及窗口焦点正确，home/back 执行成功，未知路由被拒，无认证请求返回 HTTP401。OpenClaw `stepclaw.system.run` 以 cwd=`/root/.openclaw` 调用客户端也成功；cwd=`/root/.openclaw/workspace` 不在该插件白名单，会报不明确的 unknown error。

原生 Amoo 的首次自然语言测试仍只查询内置原子能力，没有采用新增工作区工具。新增技能文件/TOOLS 说明本身尚不能证明原生路由会自动加载。原生聊天的后续验证见 `reports/phone-settings-2026-10-22.md`；不要把底层测试成功写成原生自然语言路由已修好。
