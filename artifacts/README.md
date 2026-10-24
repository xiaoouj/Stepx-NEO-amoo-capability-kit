# 可安装工具

这些产物由本仓库整理后的源码于2026-10-24重新构建。SHA256SUMS用于核对文件。

- Amoo-Phone-Tools-0.2.0.apk：本机工具应用，使用原工作区开发签名。安装APK本身不会自动恢复设置Root服务或完成配对；完整恢复请用phone-tools/install.py。
- amoo-shell.jar：Mac UI桥使用的Android shell辅助程序。
- settings-broker.jar：设置直达与导航服务；需要安装器生成私有认证并在已授权Root会话运行。

没有包含签名私钥或Broker配置。不同签名APK不能直接覆盖已有安装。
