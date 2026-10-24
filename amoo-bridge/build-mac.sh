#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
ADB="${AMOO_ADB_PATH:-${HOME}/.workbuddy/binaries/android/platform-tools/adb}"
if [ "${AMOO_SKIP_ANDROID_BUILD:-0}" != "1" ]; then
  /bin/bash "$ROOT/build-android.sh"
fi
APP="$ROOT/dist/Amoo Bridge.app"
mkdir -p "$ROOT/build/mac" "$APP/Contents/MacOS" "$APP/Contents/Resources"
test -f "$ROOT/build/amoo-shell.jar"
test -x "$ADB"
/usr/bin/swiftc -swift-version 5 -parse-as-library -O -target arm64-apple-macosx13.0 -framework SwiftUI -framework AppKit -framework Foundation -framework Security "$ROOT/mac/AmooBridge.swift" -o "$ROOT/build/mac/AmooBridge"
cp "$ROOT/build/mac/AmooBridge" "$APP/Contents/MacOS/AmooBridge"
cp "$ROOT/build/amoo-shell.jar" "$APP/Contents/Resources/amoo-shell.jar"
cp "$ADB" "$APP/Contents/Resources/adb"
chmod 755 "$APP/Contents/MacOS/AmooBridge" "$APP/Contents/Resources/adb"
cat > "$APP/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleExecutable</key><string>AmooBridge</string>
<key>CFBundleIdentifier</key><string>com.amoo.macbridge</string>
<key>CFBundleName</key><string>Amoo Bridge</string>
<key>CFBundleDisplayName</key><string>Amoo Bridge</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>CFBundleShortVersionString</key><string>0.1.0</string>
<key>CFBundleVersion</key><string>1</string>
<key>LSMinimumSystemVersion</key><string>13.0</string>
<key>NSHighResolutionCapable</key><true/>
<key>NSAppTransportSecurity</key><dict><key>NSAllowsLocalNetworking</key><true/></dict>
</dict></plist>
PLIST
/usr/bin/plutil -lint "$APP/Contents/Info.plist"
/usr/bin/codesign --force --sign - "$APP/Contents/Resources/adb"
/usr/bin/codesign --force --sign - "$APP"
/usr/bin/codesign --verify --deep --strict "$APP"
echo "$APP"
