#!/usr/bin/env bash
# Builds tv.apk with the Android SDK command-line tools (no Gradle).
# Usage: android/build.sh <version-code>
set -euo pipefail

VERSION_CODE="${1:-1}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$SDK" ] || { echo "ANDROID_HOME is not set" >&2; exit 1; }

PLATFORM="$(ls -d "$SDK"/platforms/android-* | grep -E 'android-[0-9]+$' | sort -V | tail -1)"
TOOLS="$(ls -d "$SDK"/build-tools/* | grep -E '/[0-9.]+$' | sort -V | tail -1)"
echo "Platform: $PLATFORM"
echo "Build tools: $TOOLS"

OUT="$ROOT/build"
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex"

"$TOOLS/aapt2" compile --dir "$ROOT/android/res" -o "$OUT/res.zip"
"$TOOLS/aapt2" link -o "$OUT/unsigned.apk" \
  -I "$PLATFORM/android.jar" \
  --manifest "$ROOT/android/AndroidManifest.xml" \
  --min-sdk-version 22 --target-sdk-version 33 \
  --version-code "$VERSION_CODE" --version-name "1.$VERSION_CODE" \
  "$OUT/res.zip"

javac -source 8 -target 8 -nowarn -Xlint:-options \
  -bootclasspath "$PLATFORM/android.jar" \
  -d "$OUT/classes" $(find "$ROOT/android/src" -name '*.java')

"$TOOLS/d8" --lib "$PLATFORM/android.jar" --min-api 22 \
  --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')

(cd "$OUT/dex" && zip -q "$OUT/unsigned.apk" classes.dex)
"$TOOLS/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$TOOLS/apksigner" sign \
  --ks "$ROOT/android/signing.keystore" --ks-pass pass:delaomedia \
  --ks-key-alias media --key-pass pass:delaomedia \
  --out "$ROOT/tv.apk" "$OUT/aligned.apk"
"$TOOLS/apksigner" verify "$ROOT/tv.apk"
"$TOOLS/aapt2" dump badging "$ROOT/tv.apk" | head -5
ls -l "$ROOT/tv.apk"
