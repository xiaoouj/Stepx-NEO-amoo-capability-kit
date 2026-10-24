# AMOO 能力补齐工具集

本仓库保存为 StepX Neo / PZ261A 上的 AMOO 补齐端侧能力而编写的源码、工具、技能说明和实测记录。整理日期：2026-10-24。

核心实现是 **AMOO → Android AppFunctions → 本机工具应用**。设置直达使用手机本地 Root 服务；截图、读取控件、点击、滑动和输入使用无障碍服务。原生工具能完成的任务优先使用原生工具。

## 从这里开始

1. 阅读 [安装与恢复](docs/SETUP.md)，配置 SDK、JDK 和 ADB。
2. 构建并安装 [AMOO 本机工具 0.2.0](phone-tools/README.md)。
3. 将 [技能说明](phone-tools/skill/SKILL.md) 交给原生 AMOO 保存到其自己的技能环境，并实际调用健康检查。
4. 用 [验证与排错](docs/VERIFY.md) 确认系统登记、执行服务和无障碍授权。

## 目录

| 目录 | 内容 |
| --- | --- |
| `phone-tools/` | Android 应用完整源码、AppFunctions 元数据、无障碍服务、构建与安装器 |
| `phone-settings/` | Root 设置服务、Python 客户端、四个直达路由、静态设置索引和索引生成器 |
| `amoo-bridge/` | SwiftUI Mac 客户端与 Android UiAutomation 桥接源码 |
| `skills/` | 商店安装应用和第三方 AgentTouch 的补充操作说明 |
| `tools/` | AppFunctions 诊断、能力目录生成、协议诊断与构建环境脚本 |
| `research/runtime/` | 已验证的被动会话观察脚本、依赖及使用说明 |
| `reports/` | 脱敏后的历史调查、验证结果与能力目录 |
| `artifacts/` | 本次源码构建的本机工具 APK、桥接 JAR及 SHA256 |
| `docs/` | 安装、授权、恢复、验证、来源与归档边界 |

## 已实现的能力

- 直达开发者选项、无线调试、输入法设置、电池健康度；打开页面不切换开关。
- 返回桌面、返回上一页、打开最近任务。
- 搜索静态设置索引：519个 Activity/alias、2451条标题项目；这些是候选，只有登记的四个路由承诺直达。
- 截屏、读取控件树、按坐标点击和滑动、向当前输入框写入文字。
- 健康检查分别报告设置服务和无障碍连接状态。
- 正确使用原生商店的 `appstore_install_app`，避免只搜索不安装。

`home` 表示返回桌面，不是强制结束进程。没有通用任意 Shell/文件读写接口；微信消息发送未完成专项验证。截图返回手机路径/URI，不代表云侧模型已经看到了图片。

## 桥接研究完成度

Mac GUI 客户端是需要手机解锁、AMOO在前台的 UI 桥。运行时研究观察到了 `chat.send`、`session.message` 以及真实新闻任务中的4次 `web_search` 从PENDING到OK和最后回答。

**无UI直接请求、后台常驻、通用模型API以及Mac工具执行回传尚未验证。** 本仓库不把会话观察结果描述为已经接通 WorkBuddy/Qoder 的编码后端。详见 [新闻任务验证](reports/amoo-news-roundtrip-2026-10-23.md)。

## 构建

```sh
source tools/env.sh
python3 phone-tools/build.py
bash amoo-bridge/build-android.sh
# Apple Silicon macOS 13+，需 Swift 命令行工具：
bash amoo-bridge/build-mac.sh
```

构建入口支持 `AMOO_ANDROID_SDK`、`AMOO_BUILD_TOOLS`、`AMOO_JAVA_HOME`、`AMOO_ADB_PATH`；安装器要求显式设备序列号。详见 [SETUP](docs/SETUP.md)。

## 归档与来源

本仓库不包含签名私钥、认证配置、聊天数据库、原始抓包或系统 APK。第三方 AgentTouch、DFRoot、KernelSU、Frida、StepBridge 也不冒充自研源码分发。详情见 [来源和排除清单](docs/SOURCES.md)。历史报告里的 `.local/` 是原工作区的私有证据路径，不在此仓库；旧阶段结论应结合日期及后续报告阅读。
