#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
source "$ROOT/../tools/env.sh"
mkdir -p "$ROOT/build/classes" "$ROOT/build/dex"
"$AMOO_JAVA_HOME/bin/javac" -encoding UTF-8 --release 8 -classpath "$AMOO_ANDROID_SDK/platforms/android-37/android.jar" -d "$ROOT/build/classes" "$ROOT/android/SettingsBroker.java"
"$AMOO_JAVA_HOME/bin/jar" cf "$ROOT/build/classes.jar" -C "$ROOT/build/classes" .
"$AMOO_JAVA_HOME/bin/java" -cp "$AMOO_BUILD_TOOLS/lib/d8.jar" com.android.tools.r8.D8 --min-api 34 --lib "$AMOO_ANDROID_SDK/platforms/android-37/android.jar" --output "$ROOT/build/dex" "$ROOT/build/classes.jar"
"$AMOO_JAVA_HOME/bin/jar" cf "$ROOT/build/settings-broker.jar" -C "$ROOT/build/dex" classes.dex
echo "$ROOT/build/settings-broker.jar"
