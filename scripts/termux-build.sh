#!/usr/bin/env bash
#
# VcApp — build APK de debug direct pe telefon, din Termux (proot-distro Ubuntu).
#
# NU ai nevoie de root/Magisk: proot rulează 100% în userspace.
#
# Cum se folosește (detalii complete în BUILD.md):
#   1. În Termux (o singură dată):
#        pkg update && pkg install -y proot-distro
#        proot-distro install ubuntu
#   2. Intri în Ubuntu:
#        proot-distro login ubuntu
#   3. Clonezi proiectul și rulezi scriptul:
#        apt-get update && apt-get install -y git
#        git clone https://github.com/redglitchx001-dev/VcApp.git
#        cd VcApp && bash scripts/termux-build.sh
#
set -euo pipefail

# ---- configurabile (poți suprascrie din exterior) ------------------------
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="${CMDLINE_TOOLS_VERSION:-14742923}"   # ultima versiune: developer.android.com/studio
SDK_PACKAGES=("platform-tools" "platforms;android-34" "build-tools;34.0.0")
GRADLE_VERSION="${GRADLE_VERSION:-8.7}"
# ---------------------------------------------------------------------------

log() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }

if [ "$(id -u)" -eq 0 ]; then SUDO=""; else SUDO="sudo"; fi

# 1. Dependențe de sistem
log "Instalez JDK 17 + wget + unzip + git"
$SUDO apt-get update -y
$SUDO apt-get install -y openjdk-17-jdk wget unzip git

JAVA_BIN="$(readlink -f "$(command -v java)")"
export JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
log "JDK: $JAVA_HOME — $(java -version 2>&1 | head -1)"

# 2. Android SDK command-line tools
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
  log "Descarc Android command-line tools (build $CMDLINE_TOOLS_VERSION)"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  ZIP="/tmp/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
  wget -q --show-progress -O "$ZIP" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  unzip -q -o "$ZIP" -d "$ANDROID_HOME/cmdline-tools"
  if [ -d "$ANDROID_HOME/cmdline-tools/cmdline-tools" ]; then
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  fi
fi
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# 3. Licențe + pachete SDK
log "Accept licențele SDK"
yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true
log "Instalez platform-tools, platforms;android-34, build-tools;34.0.0 (~400 MB)"
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" "${SDK_PACKAGES[@]}"

# 4. local.properties
if ! grep -q '^sdk.dir=' local.properties 2>/dev/null; then
  log "Scriu local.properties"
  echo "sdk.dir=$ANDROID_HOME" > local.properties
fi

# 5. Wrapper Gradle (e commituit în repo — doar fallback de siguranță)
if [ ! -x ./gradlew ]; then
  log "Wrapper-ul lipsește — descarc Gradle $GRADLE_VERSION o dată ca să-l generez"
  GZ="/tmp/gradle-${GRADLE_VERSION}-bin.zip"
  wget -q --show-progress -O "$GZ" \
    "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  rm -rf "/tmp/gradle-${GRADLE_VERSION}"
  unzip -q -o "$GZ" -d /tmp
  /tmp/gradle-${GRADLE_VERSION}/bin/gradle wrapper --gradle-version "$GRADLE_VERSION" --no-daemon
fi

# 6. Build
log "Rulez ./gradlew assembleDebug (primul build durează: descarcă dependențele)"
./gradlew assembleDebug --stacktrace

APK="app/build/outputs/apk/debug/app-debug.apk"
log "GATA. APK: $(pwd)/$APK"
ls -lh "$APK"
