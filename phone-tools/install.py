#!/usr/bin/env python3
import os
import argparse
import json
import pathlib
import re
import shlex
import subprocess
import sys
import time

BASE = pathlib.Path(__file__).resolve().parent
PACKAGE = 'com.amoo.executor'
VERSION = '0.2.0'
SERVICE = PACKAGE + '/.AmooAccessibilityService'


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--serial', required=True)
    p.add_argument('--adb', default=os.environ.get('AMOO_ADB_PATH', os.path.expanduser('~/.workbuddy/binaries/android/platform-tools/adb')))
    p.add_argument('--enable-accessibility', action='store_true', help='enable the bundled accessibility service after install, preserving existing services')
    args = p.parse_args()
    def adb(*argv):
        return subprocess.check_output([args.adb, '-s', args.serial, *argv], text=True, timeout=60)
    def root(command):
        return adb('shell', 'su -c ' + shlex.quote(command))
    if 'uid=0(' not in root('id'):
        raise RuntimeError('Shell Root is required to pair with the existing local executor')
    probe = 'chroot /mnt/openclaw/rootfs /usr/bin/python3 /root/.openclaw/workspace/phone-settings/phone_settings.py health'
    try:
        ready = json.loads(root(probe)).get('ok') is True
    except (subprocess.CalledProcessError, ValueError):
        ready = False
    if not ready:
        subprocess.run([sys.executable, str(BASE.parent/'phone-settings/install.py'), '--serial', args.serial, '--adb', args.adb, '--service-only'], check=True)
    subprocess.run([sys.executable, str(BASE/'build.py')], check=True)
    print(adb('install', '--no-incremental', '-r', str(BASE/('dist/Amoo-Phone-Tools-' + VERSION + '.apk'))))
    package_dump = adb('shell', 'dumpsys package ' + PACKAGE)
    match = re.search(r'\b(?:appId|userId)=(\d+)', package_dump)
    if not match:
        raise RuntimeError('Installed app UID not found')
    uid = match[1]
    private = '/data/user/0/' + PACKAGE + '/files'
    config = '/mnt/openclaw/rootfs/root/.openclaw/workspace/phone-settings/.broker.json'
    # Pair entirely on-device; the private bearer token never crosses host stdout.
    root('mkdir -p ' + private + ' && cp ' + config + ' ' + private + '/broker.json && chown -R ' + uid + ':' + uid + ' ' + private + ' && chmod 700 ' + private + ' && chmod 600 ' + private + '/broker.json && restorecon -RF ' + private)
    if args.enable_accessibility:
        current = adb('shell', 'settings get secure enabled_accessibility_services').strip()
        services = [] if current in ('', 'null') else [s for s in current.split(':') if s]
        if SERVICE not in services:
            services.append(SERVICE)
            root('settings put secure enabled_accessibility_services ' + shlex.quote(':'.join(services)))
        root('settings put secure accessibility_enabled 1')
        time.sleep(2)
        enabled = adb('shell', 'settings get secure enabled_accessibility_services').strip()
        if SERVICE not in enabled:
            raise RuntimeError('Accessibility service was not enabled')
        print('Accessibility enabled: ' + SERVICE)
        for line in adb('shell', 'dumpsys accessibility').splitlines():
            if PACKAGE in line:
                print('dumpsys: ' + line.strip())
    print(adb('shell', 'am start -n ' + PACKAGE + '/.MainActivity'))
    print('Installed and paired. AppFunction indexing is asynchronous; verify registration before testing calls.')


if __name__ == '__main__':
    main()
