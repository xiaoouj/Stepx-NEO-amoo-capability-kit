#!/usr/bin/env python3
"""Build and install the reversible, session-only local Settings broker over ADB."""
import argparse
import json
import os
import pathlib
import secrets
import shlex
import subprocess
import time

BASE = pathlib.Path(__file__).resolve().parent
ROOTFS = '/mnt/openclaw/rootfs'
WORKSPACE = ROOTFS + '/root/.openclaw/workspace'
DEST = WORKSPACE + '/phone-settings'
STAGE = '/data/local/tmp/amoo-settings-install'
MARKER = '<!-- phone-settings-direct -->'
NOTE = '''
<!-- phone-settings-direct -->
## 本机设置直达与返回桌面
打开开发者选项、无线调试、输入法设置、电池健康度，或返回桌面/上一页时，使用工作区 skills/phone-settings-direct/SKILL.md 的本机工具。这些已验证入口不依赖 gui_agent。
运行 python3 /root/.openclaw/workspace/phone-settings/phone_settings.py routes 查看入口，search 查询设置索引，open developer / open wireless-debugging / open keyboard / open battery-health 打开页面，home 返回桌面，back 返回上一页。
工具只打开页面，不自动开启开关。以实际执行结果为准；服务退出时说明需要恢复服务，不声称已完成。不要输出 .broker.json。
<!-- /phone-settings-direct -->
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--serial', required=True)
    parser.add_argument('--adb', default=os.environ.get('AMOO_ADB_PATH', os.path.expanduser('~/.workbuddy/binaries/android/platform-tools/adb')))
    parser.add_argument('--sdk', default=os.environ.get('AMOO_ANDROID_SDK', os.path.expanduser('~/.workbuddy/binaries/android')))
    parser.add_argument('--jdk', default=os.environ.get('AMOO_JAVA_HOME', os.path.expanduser('~/.workbuddy/binaries/jdk-17.0.20.1+1/Contents/Home')))
    parser.add_argument('--service-only', action='store_true',
                        help='Install the broker without adding workspace skills or editing TOOLS.md')
    args = parser.parse_args()
    def adb(*argv):
        return subprocess.check_output([args.adb, '-s', args.serial, *argv], text=True, timeout=35)
    def root(command):
        return adb('shell', 'su -c ' + shlex.quote(command))
    if 'uid=0(' not in root('id'):
        raise RuntimeError('Shell Root is unavailable')
    # Do not rotate credentials while an earlier instance is still using the port.
    old = root('if [ -f ' + shlex.quote(DEST + '/.broker.json') + ' ]; then chroot ' + ROOTFS + ' /usr/bin/python3 /root/.openclaw/workspace/phone-settings/phone_settings.py shutdown; fi')
    if old:
        print('Previous service shutdown requested.')
        time.sleep(.5)
    build = BASE / 'build'; classes = build / 'classes'; dex = build / 'dex'
    classes.mkdir(parents=True, exist_ok=True); dex.mkdir(exist_ok=True)
    jdk = pathlib.Path(args.jdk); sdk = pathlib.Path(args.sdk)
    android = sdk / 'platforms/android-37/android.jar'
    subprocess.run([str(jdk/'bin/javac'), '-encoding', 'UTF-8', '--release', '8', '-classpath', str(android), '-d', str(classes), str(BASE/'android/SettingsBroker.java')], check=True)
    subprocess.run([str(jdk/'bin/jar'), 'cf', str(build/'classes.jar'), '-C', str(classes), '.'], check=True)
    subprocess.run([str(jdk/'bin/java'), '-cp', str(pathlib.Path(os.environ.get('AMOO_BUILD_TOOLS', str(sdk/'android-37.0')))/'lib/d8.jar'), 'com.android.tools.r8.D8', '--min-api', '34', '--lib', str(android), '--output', str(dex), str(build/'classes.jar')], check=True)
    jar = build/'settings-broker.jar'
    subprocess.run([str(jdk/'bin/jar'), 'cf', str(jar), '-C', str(dex), 'classes.dex'], check=True)
    cfg = build/'.broker.json'
    cfg.write_text(json.dumps({'port': 18819, 'token': secrets.token_hex(32), 'routes': DEST + '/routes.json'}))
    cfg.chmod(0o600)
    (build/'tools-note.txt').write_text(NOTE)
    adb('shell', 'mkdir -p ' + STAGE + ' && chmod 700 ' + STAGE)
    files = [(jar, 'settings-broker.jar'), (cfg, '.broker.json'), (BASE/'phone_settings.py', 'phone_settings.py'),
             (BASE/'data/routes.json', 'routes.json'), (BASE/'data/settings-index.json', 'settings-index.json'),
             (BASE/'skill/SKILL.md', 'SKILL.md'), (build/'tools-note.txt', 'tools-note.txt')]
    for source, name in files:
        adb('push', str(source), STAGE + '/' + name)
    commands = ['mkdir -p ' + DEST]
    for _, name in files[:5]:
        commands.append('cp ' + STAGE + '/' + name + ' ' + DEST + '/' + name)
    commands += ['chown -R 1000:1000 ' + DEST,
                 'chmod 750 ' + DEST, 'chmod 600 ' + DEST + '/.broker.json']
    if not args.service_only:
        commands += ['mkdir -p ' + WORKSPACE + '/skills/phone-settings-direct ' + WORKSPACE + '/.phone-settings-backups',
                     'cp ' + STAGE + '/SKILL.md ' + WORKSPACE + '/skills/phone-settings-direct/SKILL.md',
                     'chown -R 1000:1000 ' + WORKSPACE + '/skills/phone-settings-direct']
    root(' && '.join(commands))
    if not args.service_only:
        tools = WORKSPACE + '/TOOLS.md'
        # Preserve the current file byte-for-byte before adding one bounded section.
        text = root('if [ -f ' + tools + ' ]; then cat ' + tools + '; fi')
        if MARKER not in text:
            backup = WORKSPACE + '/.phone-settings-backups/TOOLS.' + time.strftime('%Y%m%d-%H%M%S') + '.md'
            root('if [ -f ' + tools + ' ]; then cp -p ' + tools + ' ' + backup + '; chmod 600 ' + backup + '; fi; cat ' + STAGE + '/tools-note.txt >> ' + tools + '; chown 1000:1000 ' + tools)
    command = 'CLASSPATH=' + DEST + '/settings-broker.jar /system/bin/app_process / com.amoo.settings.SettingsBroker ' + DEST + '/.broker.json'
    root('(' + command + ' </dev/null >' + DEST + '/broker.log 2>&1 & echo $! >' + DEST + '/broker.pid)')
    cfg.unlink()
    # Remove the staging credential; preserve reproducible installed service files.
    root('rm -f ' + STAGE + '/.broker.json')
    time.sleep(1)
    status = root('chroot ' + ROOTFS + ' /usr/bin/setpriv --reuid=1000 --regid=1000 --clear-groups /usr/bin/python3 /root/.openclaw/workspace/phone-settings/phone_settings.py health')
    print(status)


if __name__ == '__main__':
    main()
