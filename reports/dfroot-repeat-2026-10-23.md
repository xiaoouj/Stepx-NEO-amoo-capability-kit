# 恢复出厂后重新 Root — 2026-10-23

用户明确要求重新 DFRoot，并选择无线 ADB。完成重新配对后连接 `PHONE_IP:ADB_PORT`，硬件仍为 PZ261A。配对码不记录在报告中。

## 结果

已安装本地留存且重新核验 SHA256 的 DFRoot4.6、KernelSU3.3.1。DFRoot 日志显示 `ksud start: SUCCESS` 并完成临时注入清理。KernelSU 显示“工作中 [越狱模式]”，Shell 的超级用户开关已开启；独立 ADB 命令验证：

```text
su -c id
uid=0(root) gid=0(root) groups=0(root) context=u:r:ksu:s0
```

SELinux=Enforcing、Bootloader flash.locked=1、Verified Boot=green。内核仍为 `6.12.58-android16-6-maybe-dirty-4k`，系统安全补丁日期2025-12-05。没有刷机或完整重启。当前仍是临时 Root，重启后应重新检查并按需运行。

Start at Boot 初始界面为开启；界面关闭后包管理器仍显示 Receiver enabled，收尾已显式禁用 `df.root/.BootReceiver` 并独立确认出现在 disabledComponents。Auto Soft Reboot 和 Disable KSU Modules 均核对为关闭。没有安排开机自动提权。

## 当前范围

本轮只重新取得 Root。用户补充：恢复出厂后，AMOO 原生已能安装应用并进入部分更深设置；高级设置可能仍需此前指令。后续应先测试原生能力，仅针对实测缺口补工具，不整套恢复旧设置或覆盖原生提示。用户观察已记录，但尚未独立复测这些原生功能，也没有确认恢复出厂改善能力的具体原因。

没有恢复旧的 phone-tools、phone-settings 服务、AgentTouch 技能或此前 AMOO 配置；旧报告的已安装/运行/授权状态均为恢复出厂前状态。

## 证据

`.local/dfroot-2026-10-23/` 内有 preflight.json、attempt-logcat.txt、root-verification.json、settings-final.xml、ksu-shell-profile.xml、dfroot-prefs-final.xml、boot-receiver-final.txt。日志观察到一次成功运行；未重复触发已加载的模块。没有读取 AMOO 私有聊天或登录凭据。

安装包摘要与10月21日报告一致：DFRoot `21d952f37e16a61fdbf1559524e3e8f20cbcc8a3a61f010ee832eede6e4bf107`；KernelSU `b4a059ace71de2fa65cc7f12b1c58d55fc23ab25e057ee4230ce779742abe450`。
