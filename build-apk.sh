#!/usr/bin/env bash
#
# Builds a signed SecureNotes release APK (and a debug APK).
#
# Requirements: JDK 17 and the Android SDK (ANDROID_HOME or ANDROID_SDK_ROOT set,
# or Android Studio installed in the default location).
#
# Usage:  ./build-apk.sh
#
set -euo pipefail

cd "$(dirname "$0")"

# ---------------------------------------------------------------- Android SDK ---
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  for candidate in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk" "/usr/lib/android-sdk"; do
    if [[ -d "$candidate" ]]; then
      export ANDROID_HOME="$candidate"
      break
    fi
  done
fi

if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo "❌ Android SDK not found. Install Android Studio, or set ANDROID_HOME." >&2
  exit 1
fi

SDK_DIR="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
echo "📦 Using Android SDK: $SDK_DIR"
echo "sdk.dir=$SDK_DIR" > local.properties

# -------------------------------------------------------------------- Keystore ---
KEYSTORE="securenotes-release.jks"
STORE_PASS="securenotes123"
KEY_ALIAS="securenotes"

if [[ ! -f "$KEYSTORE" ]]; then
  echo "🔑 Generating release keystore…"
  keytool -genkeypair -v \
    -keystore "$KEYSTORE" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias "$KEY_ALIAS" \
    -storepass "$STORE_PASS" \
    -keypass "$STORE_PASS" \
    -dname "CN=SecureNotes, OU=Mobile, O=SecureNotes, L=Internet, C=US"
fi

cat > keystore.properties <<EOF
storeFile=$KEYSTORE
storePassword=$STORE_PASS
keyAlias=$KEY_ALIAS
keyPassword=$STORE_PASS
EOF

# ----------------------------------------------------------------------- Build ---
echo "🏗️  Building APKs (first run downloads Gradle + dependencies)…"
./gradlew --no-daemon assembleDebug assembleRelease

echo
echo "✅ Done! Your APKs:"
find app/build/outputs/apk -name "*.apk" -exec ls -lh {} \;
echo
echo "📲 Install with:  adb install -r app/build/outputs/apk/release/app-release.apk"
