# 恢复出厂后补回高级设置工具 — 2026-10-23

用户在重新 Root 后明确要求继续补回高级设置能力。已恢复 `com.amoo.executor`（AMOO 本机工具0.1.0）及其会话内 Root 执行服务。本轮保留原生能力优先，不恢复旧的全局工具说明、不重装 GUI 控制器、不修改原厂规则。

## 实测结果

系统 AppFunctions 直调 `openSettings`，开发者选项、无线调试、输入法设置、电池健康度四个入口均返回 ok=true，实际窗口焦点与目标组件一致。服务只打开页面，没有切换开关；无线调试开关测试前后相同。

AMOO 原生会话实际调用 `executor_get_tool_status`、`executor_search_settings` 成功，独立应用日志确认 caller=com.stepos.ability。健康返回Root服务PID20969；无线调试索引有2条静态候选。

随后全新原生会话使用“请用 AMOO 本机直达工具打开无线调试设置页面，只打开页面，不要改变任何开关”，实际打开目标页面。应用日志确认 openSettings caller=com.stepos.ability ok=true，Root服务新增 `SETTINGS_OPEN wireless-debugging ok=true`，窗口焦点 SubSettings。没有用外部直接调用替代原生验证。

## 原生优先与文件范围

恢复出厂后用户观察原生安装应用及部分深入设置已可用。本轮按此偏好，只补用户指定的本机工具或原生缺少入口、实际失败时的回退路径。没有独立查明恢复出厂使原生能力变化的具体原因。

AMOO 自己的运行环境仍保留旧 `amoo-phone-tools` 技能；它自报先备份4135字节旧版，再写入并回读2446字节新版，路径为自身执行环境的 `/root/.openclaw/workspace/skills/amoo-phone-tools/SKILL.md`。此位置不能直接认定为手机本地同名工作区。新版明确原生优先，不删除其他技能、不修改原厂提示。

安装器新增 `phone-settings/install.py --service-only`，由 phone-tools 默认调用；只恢复服务文件，不添加旧 phone-settings-direct 技能、不修改手机本地 TOOLS.md。安装前后及收尾的 TOOLS/AGENTS SHA256一致，且独立确认没有新写入旧的本地设置技能。AppFunction元数据也已更新为原生优先。

执行服务PID20969、UID0；新应用UID10190，本身未授予Root。认证配置留在手机内部，两个配置均为600，分别归应用UID及本地服务客户端UID1000所有；没有输出密钥。仅恢复了有限页面/导航/索引接口，没有增加任意Shell、微信发送或通用GUI能力。

## 安装器修正与限制

- `phone-tools/install.py` 关闭 incremental 安装。首次系统拒绝 incremental 后自动回退普通安装成功；新版已使用普通安装覆盖验证成功。
- 本轮构建成功，四个AppFunctions登记已核对；不修改AppFunction服务名和调用method。
- 当前 Root 和执行服务仍依赖当前开机会话，没有开机自启；重启后需重新检查Root，并运行安装器恢复服务。
- 页面返回 `pageContentVerified=false`，表示核对命令/窗口焦点，没有逐项读取页面内容。未宣称所有静态索引候选都能直达。
- 收尾恢复原来的息屏时间，ADB转发为空，SELinux Enforcing，无线调试仍开启。

## 本地证据

`.local/phone-tools-restore-2026-10-23/`：platform-pages.json、registered-tools.txt、native-teach.json、native-open-wireless.json、appfunction-calls.txt、workspace-hashes-before/after/final.txt、final-status.json。新版调用说明在 `phone-tools/skill/SKILL.md`。

恢复命令：`python3 phone-tools/install.py --serial 当前ADB地址`。安装需要Shell Root，日常调用不依赖电脑保持ADB连接。
