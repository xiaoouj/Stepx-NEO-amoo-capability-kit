# 安装、授权与重启恢复

## 1. 构建环境

需要 Python3、JDK17、Android SDK platform android-37、Build Tools（含aapt2/d8/zipalign/apksigner）和ADB。构建当前针对Android16/API36及厂商AppFunctions环境，其他系统未逐一验证。

如已安装 WorkBuddy 的相同工具布局，可直接 `source tools/env.sh`。标准 Android SDK 可先设置：

```sh
export AMOO_ANDROID_SDK="$HOME/Library/Android/sdk"
export AMOO_BUILD_TOOLS="$AMOO_ANDROID_SDK/build-tools/37.0.0"
export AMOO_JAVA_HOME="$(/usr/libexec/java_home -v 17)"
export AMOO_ADB_PATH="$AMOO_ANDROID_SDK/platform-tools/adb"
source tools/env.sh
```

Build Tools目录请按实际已安装版本填写，脚本不会安装软件或更改系统配置。

## 2. 连接手机

USB：开启开发者选项、USB调试，并在手机上允许此电脑。无线：保持“使用配对码配对设备”弹窗打开，执行 `adb pair` 并在终端交互输入配对码，然后使用无线调试主页面的连接端口：

```sh
"$AMOO_ADB_PATH" pair PHONE_IP:PAIR_PORT
"$AMOO_ADB_PATH" connect PHONE_IP:CONNECT_PORT
"$AMOO_ADB_PATH" devices -l
export AMOO_SERIAL='PHONE_IP:CONNECT_PORT'
```

USB连接时把AMOO_SERIAL换为 `devices -l` 显示的设备序列号。配对端口与连接端口不同；旧地址不能长期硬编码。不要保存配对码到仓库。

## 3. Root前提

设置直达服务的安装/配对需要已授权的Shell Root。本仓库不自动获取Root、不刷机、不配置开机提权。此前本机使用DFRoot4.6和KernelSU3.3.1取得临时Root的结果及包摘要在 [记录](../reports/dfroot-repeat-2026-10-23.md)，不代表其他补丁版本同样适用。

在已取得Root的设备上，在KernelSU里为Shell授权，然后验证：

```sh
"$AMOO_ADB_PATH" -s "$AMOO_SERIAL" shell 'su -c id'
```

预期包含uid=0。不要把AMOO云侧脚本自报Root当作手机Root。

## 4. 安装本机工具

在仓库根目录运行：

```sh
python3 phone-tools/install.py --serial "$AMOO_SERIAL" --adb "$AMOO_ADB_PATH" --enable-accessibility
```

安装器会构建APK、检查/恢复设置服务、安装应用，在手机内部配对认证，并保留已有无障碍服务后追加本服务。也可不加 `--enable-accessibility`，在“设置→无障碍→AMOO本机工具”手动开启。

服务恢复使用 `--service-only`，不改手机本地TOOLS/AGENTS。应用自身保持普通UID，设置服务运行在本机回环地址。安装完成后可能需要等待AppFunctions索引刷新。

## 5. 教给原生AMOO

把 [phone-tools/skill/SKILL.md](../phone-tools/skill/SKILL.md) 的内容交给原生AMOO，要求它保存到自己执行环境的 `skills/amoo-phone-tools/SKILL.md`，并实际调用 `executor_get_tool_status` 验证。

建议提示：

> 请保存以下本机工具说明为你自己环境中新会话可读取的技能，保留原生能力优先。保存后实际调用 executor_get_tool_status，并根据返回的service和accessibility状态说明是否可用。不要只回复“已保存”。

然后新建会话，分别测试“打开无线调试页面，不改变开关”“返回桌面”“读取当前界面控件”。手机本地OpenClaw工作区与AMOO自身环境可能不同，仅复制本地技能文件不保证原生AMOO加载它。

商店安装说明见 [appstore技能](../skills/appstore-install-app/SKILL.md)。

## 6. 重启、恢复出厂与升级

- 重启后先检查Root；临时Root及设置服务可能消失，恢复后重跑安装器。此项目没有开机自启。
- 无障碍UI工具不依赖Root，但系统可能关闭授权或断开服务，需要重新开启并验证。
- 恢复出厂会移除安装、授权与配对；先测试当前原生功能，再按缺口安装。
- 保留本地 `.local/phone-tools-signing.keystore` 用于原位升级；仓库不含私钥。换签名会导致更新失败，不应默认卸载已有应用。
- 可以用 `AMOO_SIGNING_KEY` 指定已有签名密钥文件。当前构建使用开发签名参数；对应私钥仍只保存在本地。
- 单独重装设置服务会轮换认证，之后重跑phone-tools安装器配对。

## 7. 日常使用与停用

安装后的AMOO→AppFunctions→本机工具调用不需要电脑在线。Mac UI桥需要ADB和手机前台。

停用GUI能力可在系统无障碍设置关闭“AMOO本机工具”。停止设置服务可在手机现有chroot环境调用：

```sh
"$AMOO_ADB_PATH" -s "$AMOO_SERIAL" shell 'su -c "chroot /mnt/openclaw/rootfs /usr/bin/python3 /root/.openclaw/workspace/phone-settings/phone_settings.py shutdown"'
```

不要输出或提交手机 `.broker.json`、应用私有 `broker.json`。
