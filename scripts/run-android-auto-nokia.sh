#!/usr/bin/env bash
set -euo pipefail

# Run this on Kunlun, where the Nokia and visible desktop session are attached.
# If invoked through SSH, these defaults put the DHU window on Kunlun's desktop.
export DISPLAY="${DISPLAY:-:0}"
export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}"

serial="${ANDROID_AUTO_SERIAL:-AQ7505H032N92400458}"
sdk="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
runtime="$HOME/.local/lib/android-auto-dhu/usr/lib/x86_64-linux-gnu"
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
config="${ANDROID_AUTO_DHU_CONFIG:-$script_dir/android-auto-1280x600.ini}"
dhu="$sdk/extras/google/auto/desktop-head-unit"

if [[ "$(adb -s "$serial" get-state 2>/dev/null || true)" != "device" ]]; then
  echo "Nokia G60 5G ($serial) is not connected and authorized in ADB." >&2
  exit 1
fi

adb -s "$serial" forward tcp:5277 tcp:5277 >/dev/null
export LD_LIBRARY_PATH="$runtime${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"

exec "$dhu" -c "$config" --adb=5277 "$@"
