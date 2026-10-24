#!/bin/bash
# Source from the repository root. Set overrides before sourcing for a standard SDK.
export AMOO_ANDROID_SDK="${AMOO_ANDROID_SDK:-$HOME/.workbuddy/binaries/android}"
export AMOO_BUILD_TOOLS="${AMOO_BUILD_TOOLS:-$AMOO_ANDROID_SDK/android-37.0}"
export AMOO_JAVA_HOME="${AMOO_JAVA_HOME:-$HOME/.workbuddy/binaries/jdk-17.0.20.1+1/Contents/Home}"
export AMOO_ADB_PATH="${AMOO_ADB_PATH:-$AMOO_ANDROID_SDK/platform-tools/adb}"
for amoo_required in "$AMOO_ADB_PATH" "$AMOO_JAVA_HOME/bin/javac" "$AMOO_BUILD_TOOLS/aapt2" "$AMOO_ANDROID_SDK/platforms/android-37/android.jar"; do
  if [ ! -f "$amoo_required" ]; then
    echo "Missing dependency: $amoo_required; configure paths using docs/SETUP.md" >&2
    return 1 2>/dev/null || exit 1
  fi
done
unset amoo_required
