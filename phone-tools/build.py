#!/usr/bin/env python3
import os
import pathlib
import shutil
import subprocess
import sys
import zipfile

BASE = pathlib.Path(__file__).resolve().parent
VERSION = '0.2.0'


def main():
    sdk = pathlib.Path(os.environ.get('AMOO_ANDROID_SDK', os.path.expanduser('~/.workbuddy/binaries/android')))
    jdk = pathlib.Path(os.environ.get('AMOO_JAVA_HOME', os.path.expanduser('~/.workbuddy/binaries/jdk-17.0.20.1+1/Contents/Home')))
    bt = pathlib.Path(os.environ.get('AMOO_BUILD_TOOLS', str(sdk/'android-37.0'))); platform = sdk/'platforms/android-37/android.jar'
    build = BASE/'build'; dist = BASE/'dist'
    for sub in ['gen', 'classes', 'dex', 'res']:
        shutil.rmtree(build/sub, ignore_errors=True)
        (build/sub).mkdir(parents=True, exist_ok=True)
    dist.mkdir(exist_ok=True)
    def run(*args):
        subprocess.run([str(a) for a in args], check=True)
    run(sys.executable, BASE/'generate_metadata.py')
    for name in ['routes.json', 'settings-index.json']:
        shutil.copyfile(BASE.parent/'phone-settings/data'/name, BASE/'app/assets'/name)
    run(bt/'aapt2', 'compile', '--dir', BASE/'app/res', '-o', build/'res')
    flat = sorted((build/'res').rglob('*.flat'))
    if not flat:
        raise RuntimeError('No compiled resources')
    unsigned = build/'unsigned.apk'
    run(bt/'aapt2', 'link', '-o', unsigned, '-I', platform, '--manifest', BASE/'app/AndroidManifest.xml',
        '--java', build/'gen', '-A', BASE/'app/assets', *flat)
    java = sorted((BASE/'app/java').rglob('*.java')) + sorted((build/'gen').rglob('*.java'))
    run(jdk/'bin/javac', '-encoding', 'UTF-8', '--release', '8', '-classpath', platform, '-d', build/'classes', *java)
    run(jdk/'bin/jar', 'cf', build/'classes.jar', '-C', build/'classes', '.')
    run(jdk/'bin/java', '-cp', bt/'lib/d8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '36', '--lib', platform, '--output', build/'dex', build/'classes.jar')
    with zipfile.ZipFile(unsigned, 'a', zipfile.ZIP_DEFLATED) as z:
        z.write(build/'dex/classes.dex', 'classes.dex')
    run(bt/'zipalign', '-f', '4', unsigned, build/'aligned.apk')
    key = pathlib.Path(os.environ.get('AMOO_SIGNING_KEY', str(BASE.parent/'.local/phone-tools-signing.keystore')))
    key.parent.mkdir(parents=True, exist_ok=True)
    if not key.exists():
        run(jdk/'bin/keytool', '-genkeypair', '-keystore', key, '-storepass', 'android', '-keypass', 'android', '-alias', 'phone-tools', '-dname', 'CN=AMOO Local Phone Tools', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000')
        key.chmod(0o600)
    apk = dist/('Amoo-Phone-Tools-' + VERSION + '.apk')
    run(jdk/'bin/java', '-jar', bt/'lib/apksigner.jar', 'sign', '--ks', key, '--ks-key-alias', 'phone-tools', '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', apk, build/'aligned.apk')
    run(jdk/'bin/java', '-jar', bt/'lib/apksigner.jar', 'verify', apk)
    print(apk)


if __name__ == '__main__':
    main()
