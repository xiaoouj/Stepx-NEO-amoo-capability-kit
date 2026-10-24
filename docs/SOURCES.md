# 源码、第三方组件与归档范围

包含本工作区编写的phone-tools、phone-settings、amoo-bridge及诊断工具源码；被动运行时观察脚本保留成功使用的版本，依赖改为显式导入。发布副本将个人绝对路径改为可配置路径、设备地址与序列号换为占位符，并修复首次构建时签名目录不存在的问题。

phone-tools/app/assets/app_functions_schema.xsd属于AOSP Apache2.0文件，保留NOTICE和许可证。设置索引来自测试固件静态组件和资源标题，不包含Settings APK或用户设置值。能力目录属于历史文档候选及验证标记，不代表每个条目可运行。

以下未包含：

- 签名私钥、Broker认证文件、登录态、API Key、配对码、聊天数据库和截图。
- 原始Frida/ADB日志、完整抓包、第三方应用反编译dump、系统APK和已安装应用数据。
- 第三方DFRoot、KernelSU、AgentTouch、Frida二进制；它们不是本工作区自研项目，也未取得全部源码。保留相关操作与验证记录。
- StepBridge副本；参考项目为 https://github.com/nexaforgelab/StepBridge 。
- StepX Mini修复工程与其原APK运行时DEX：属于另一项应用修复工作，不是AMOO端侧能力补齐。相关历史报告仅作为上下文保留。
- 失败/过时的探针迭代及合并了第三方Java bridge的生成bundle；保留成功的观察源码。

本仓库没有给全部第三方内容重新指定统一许可证。自编源码归原权利人所有；第三方文件按各自声明使用。历史报告引用的 `.local/`、Evidence ID 和私有测试输出只是追溯标识，不保证读者能在此仓库访问到这些材料。
