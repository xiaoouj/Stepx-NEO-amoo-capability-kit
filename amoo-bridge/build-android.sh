#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK="${AMOO_ANDROID_SDK:-${HOME}/.workbuddy/binaries/android}"
JDK="${AMOO_JAVA_HOME:-${HOME}/.workbuddy/binaries/jdk-17.0.20.1+1/Contents/Home}"
BT="${AMOO_BUILD_TOOLS:-$SDK/android-37.0}"
mkdir -p "$ROOT/build/android/classes" "$ROOT/build/android/dex"
"$JDK/bin/javac" -encoding UTF-8 --release 8 -classpath "$SDK/platforms/android-37/android.jar" -d "$ROOT/build/android/classes" "$ROOT/android/Bridge.java"
"$JDK/bin/jar" cf "$ROOT/build/android/classes.jar" -C "$ROOT/build/android/classes" .
"$JDK/bin/java" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --min-api 34 --lib "$SDK/platforms/android-37/android.jar" --output "$ROOT/build/android/dex" "$ROOT/build/android/classes.jar"
(cd "$ROOT/build/android/dex" && "$JDK/bin/jar" cf "$ROOT/build/amoo-shell.jar" classes.dex)
echo "$ROOT/build/amoo-shell.jar"
