# AMOO 新增端侧执行工具 — 2026-10-22

## 结果

已新增并安装 Android 应用“AMOO 本机工具”（`com.amoo.executor`，0.1.0），由系统 AppFunctions 注册四个接口。AMOO 原生聊天已能调用；在不同的新会话中，仅给普通指令即可打开无线调试、打开开发者选项、回到桌面、返回上一页。

这次是新增手机端执行代码和真实接口，补齐了此前手机本地Root服务无法被原生AMOO调用的一段链路。没有清空原厂提示词、替换系统APK、关闭GUI总闸、改变系统签名校验或让云端脚本直接访问手机回环地址。

## 架构与安装

AMOO → 原有端侧 `ability.call` → 新AppFunctionService → 手机回环认证接口 → 已有Root执行服务 → Android页面/按键。

普通应用服务由系统 `BIND_APP_FUNCTION_SERVICE` 绑定权限保护，并校验真实callingPackage；其自身没有Root。Root服务仍为之前的 `SettingsBroker`，测试时PID27054。认证配置仅复制到新应用私有目录并恢复SELinux标签，没有输出密钥。

电脑源码 `phone-tools/`，可复现构建/安装：

```sh
python3 phone-tools/install.py --serial PHONE_IP:ADB_PORT
```

当前无线ADB端口成功连接。系统AppFunctions登记已独立读取，四个功能均默认启用：

- `com.amoo.executor.PhoneTools#openSettings`
- `com.amoo.executor.PhoneTools#navigate`
- `com.amoo.executor.PhoneTools#searchSettings`
- `com.amoo.executor.PhoneTools#getToolStatus`

对应原子方法为 `executor_open_settings`、`executor_navigate`、`executor_search_settings`、`executor_get_tool_status`。

## 能力范围

已实现四个登记页面：开发者选项、无线调试、输入法设置、电池健康度；系统导航home/back/recents；设置索引查询；执行服务检查。索引仍为519个Activity/alias和2,451个带标题XML项目，仅部分候选有直达入口。

页面启动不切换开关。home只回桌面，不强制停止进程。不存在任意Shell执行、任意文件访问、点击/滑动或微信消息发送接口。当前Root方式和执行服务在手机重启后可能消失，需要恢复Root并重新运行安装器；没有添加开机项。

## 验证

### 系统接口

`cmd app_function execute-app-function` 成功调用服务健康检查、查询无线调试索引（2条）、打开无线调试；未知页面和非法导航参数均被拒。随后通过实际 `ability.call` 调用 `executor_get_tool_status` 成功，证明新接口进入AMOO所用端侧通道。

### 原生模型学习与持久说明

让原生AMOO实际调用健康检查和索引查询，均由新应用日志独立确认 caller=`com.stepos.ability`。AMOO自报新增自身工作区 `skills/amoo-phone-tools/SKILL.md`，4135字节，保留原有9个技能目录。没有取得该原生环境文件的完整原始副本；电脑 `phone-tools/skill/SKILL.md` 是可复建的准确调用说明，不是冒充的原文导出。

### 新会话自然语言

各自新建会话，既有草稿/生成中会话受到检查保护；发送普通文字指令，验证语音识别后的调用链路，未注入麦克风音频。

| 指令 | 独立手机日志/焦点 |
| --- | --- |
| 打开无线调试页面，只打开页面，不要改变开关 | 新应用openSettings，caller=com.stepos.ability，ok=true；Root服务wireless-debugging事件；焦点SubSettings |
| 打开开发者选项页面，只打开页面，不改变任何开关 | 新应用openSettings，caller=com.stepos.ability，ok=true；Root服务developer事件；焦点DevelopmentSettingsActivity |
| 回到桌面 | 新应用navigate，caller=com.stepos.ability，ok=true；Root服务home事件；焦点由测试文件保留 |
| 返回上一页 | 新应用navigate，caller=com.stepos.ability，ok=true；Root服务back事件；本次从AMOO测试聊天返回到桌面 |

输入法和电池健康的相同Root服务路由在此前测试已通过，本轮未将它们逐一作为原生新会话再测试。recents代码已实现，未做原生新会话测试。

本轮未切换无线调试开关；当前它为1，源于用户此次已启用无线连接，与今天早些时候关闭状态不同，不应误称始终关闭。收尾确认SELinux Enforcing，配对文件权限600、归应用自身UID所有，本轮ADB转发已全部移除；功能应用和Root服务保留。

## 证据位置

`.local/amoo/phone-tools-investigation/`：

- `registered-functions.json`：系统实际登记。
- `platform-tests.json`：系统接口正反例。
- `ability-health.txt`：AMOO节点调用健康检查。
- `platform-page-check.json`：仅保存无线调试标题和开关相关UI节点。
- `native-teach-tools.json`：原生健康/查询及自报技能创建。
- `native-open-wireless.json`、`native-open-developer.json`、`native-home.json`、`native-back.json`：不同新会话指令、独立Root服务事件和焦点。
- `appfunction-call-log.txt`：新应用最小日志，只包含功能名、callingPackage和ok，不记录密钥或聊天。

## 后续扩展

新动作应在手机端实现、添加AppFunction元数据、重新登记后验证真实method，再补AMOO的技能说明，最后在新会话实测。现有文件/脚本能力运行环境仍需区分端侧和云端；这次只增加上述有限动作，没有宣称所有权限或所有App自动化已开放。
