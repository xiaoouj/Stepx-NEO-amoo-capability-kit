# PZ261A 临时 Root 验证 — 2026-10-21

用户授权尝试 DirtyFrag 提权。已取得并实际验证 Root。

## 实测结果

```text
adb -s PHONE_IP:ADB_PORT shell su -c id
uid=0(root) gid=0(root) groups=0(root) context=u:r:ksu:s0
```

- 设备：StepX PZ261A，Android 16。
- 内核：`6.12.58-android16-6-maybe-dirty-4k`。
- 系统报告安全补丁日期：`2025-12-05`。
- SELinux：`Enforcing`。
- Bootloader：`ro.boot.flash.locked=1`；Verified Boot：`green`。
- 仅通过目录存在性检查验证可访问 Amoo 私有目录，没有读取聊天、数据库或登录凭据。

## 工具与过程

- 官方 DFRoot v4.6：https://github.com/diabl0w/DFRoot/releases/tag/v4.6
- 官方 KernelSU v3.3.1：https://github.com/tiann/KernelSU/releases/tag/v3.3.1
- 两个 APK 均通过 APK 签名校验，SHA-256 与 GitHub Release 提供的 digest 一致。
- DFRoot APK SHA-256：`21d952f37e16a61fdbf1559524e3e8f20cbcc8a3a61f010ee832eede6e4bf107`。
- KernelSU APK SHA-256：`b4a059ace71de2fa65cc7f12b1c58d55fc23ab25e057ee4230ce779742abe450`。
- 第一次运行已触发漏洞并启动内核模块，在 `Disable KSU Modules` 步骤退出。源码中该步骤在模块目录无法打开时返回失败；这是首次安装，尚无可用 SU 管理环境。
- 关闭该选项并正常重启一次。重连后取得第二次成功日志：`ksud start: SUCCESS`。
- 用户在 KernelSU 中明确授予 Shell Root 权限，随后通过真实 `su -c id` 验证。
- 没有解锁 Bootloader、刷写分区或安装额外 KernelSU 模块；DFRoot、KernelSU 管理应用保留在手机上。
- 本方案为临时 Root；完整重启后通常需要重新运行提权。Root 期间对数据的更改不会因重启自动撤销。本次没有改动 Amoo 数据。

## 本地证据

- `.local/dirtyfrag-check/attempt-logcat.txt`：第一次日志。
- `.local/dirtyfrag-check/attempt2-logcat.txt`：第二次成功日志。
- `.local/dirtyfrag-check/root-verification.txt`：Root、SELinux 与启动状态验证输出。
- `.local/dirtyfrag-check/dfroot-final-prefs.xml`：DFRoot 最终设置。
