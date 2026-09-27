#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REMOTE_HOST=${KUNLUN_HOST:-kunlun.local}
REMOTE_DIR=${KUNLUN_TEST_DIR:-/home/puzzleduck/x/remote-mixtape-testing}
AVD=${KUNLUN_AVD:-api24-mixtape-portrait}
EMULATOR_SERIAL=${KUNLUN_EMULATOR_SERIAL:-emulator-5554}
PACKAGE_NAME=${KUNLUN_PACKAGE_NAME:-org.puzzleduck.mixtape}
MAIN_ACTIVITY=com.example.androidmixtape.MainActivity
REMOTE_APK="$REMOTE_DIR/android-mixtape-modern-debug.apk"

launch_emulator=false
install_app=false
start_app=false
screenshot_path=""
apk_path=""

usage() {
    cat <<'EOF'
Usage: ./kunlun-sync.sh [options]

Syncs the latest modern debug APK to Kunlun. Source code is not copied because
Kunlun only needs the APK for emulator testing.

Options:
  --apk PATH           Sync this APK instead of the newest standard build output
  --launch-emulator    Sound a three-note warning, then launch the visible emulator
  --install-app        Install/update the synced APK on the emulator
  --start-app          Launch Android Mixtape on the emulator
  --screenshot PATH    Save an emulator screenshot to this local path
  --all PATH           Launch, install, start, and save a screenshot to PATH
  -h, --help           Show this help

Environment overrides:
  KUNLUN_HOST, KUNLUN_TEST_DIR, KUNLUN_AVD, KUNLUN_EMULATOR_SERIAL,
  KUNLUN_PACKAGE_NAME, KUNLUN_DISPLAY_ID (optional physical display for screenshots)
EOF
}

while (($#)); do
    case "$1" in
        --apk)
            [[ $# -ge 2 ]] || { echo "--apk requires a path" >&2; exit 2; }
            apk_path=$2
            shift 2
            ;;
        --launch-emulator)
            launch_emulator=true
            shift
            ;;
        --install-app)
            install_app=true
            shift
            ;;
        --start-app)
            start_app=true
            shift
            ;;
        --screenshot)
            [[ $# -ge 2 ]] || { echo "--screenshot requires a local output path" >&2; exit 2; }
            screenshot_path=$2
            shift 2
            ;;
        --all)
            [[ $# -ge 2 ]] || { echo "--all requires a local screenshot path" >&2; exit 2; }
            launch_emulator=true
            install_app=true
            start_app=true
            screenshot_path=$2
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

if [[ -z "$apk_path" ]]; then
    candidates=(
        "$ROOT_DIR/dist/android-mixtape-v0.1.0-modern-debug.apk"
        "$ROOT_DIR/app/build/outputs/apk/modern/debug/app-modern-debug.apk"
    )
    for candidate in "${candidates[@]}"; do
        [[ -f "$candidate" ]] || continue
        if [[ -z "$apk_path" || "$candidate" -nt "$apk_path" ]]; then
            apk_path=$candidate
        fi
    done
fi

[[ -n "$apk_path" && -f "$apk_path" ]] || {
    echo "No modern debug APK found. Build it first or pass --apk PATH." >&2
    exit 1
}
apk_path=$(realpath "$apk_path")

ssh "$REMOTE_HOST" "mkdir -p '$REMOTE_DIR'"
rsync -a --checksum "$apk_path" "$REMOTE_HOST:$REMOTE_APK"
local_sha=$(sha256sum "$apk_path" | awk '{print $1}')
remote_sha=$(ssh "$REMOTE_HOST" "sha256sum '$REMOTE_APK'" | awk '{print $1}')
[[ "$local_sha" == "$remote_sha" ]] || { echo "APK checksum mismatch after sync" >&2; exit 1; }
echo "Synced APK: $apk_path -> $REMOTE_HOST:$REMOTE_APK"

if $launch_emulator; then
    if ssh "$REMOTE_HOST" "/home/puzzleduck/Android/Sdk/platform-tools/adb -s '$EMULATOR_SERIAL' get-state >/dev/null 2>&1"; then
        echo "Emulator $EMULATOR_SERIAL is already running."
    else
        chime_file=$(mktemp --suffix=.wav)
        trap 'rm -f "${chime_file:-}"' EXIT
        ruby - "$chime_file" <<'RUBY'
path = ARGV.fetch(0)
sample_rate = 44_100
segments = [[0.00, 0.14, 660.0], [0.22, 0.36, 830.0], [0.44, 0.62, 1040.0]]
duration = 0.66
samples = Array.new((sample_rate * duration).to_i, 0)
segments.each do |from, to, frequency|
  first = (from * sample_rate).to_i
  last = (to * sample_rate).to_i
  (first...last).each do |index|
    elapsed = (index - first).fdiv(sample_rate)
    remaining = (last - index).fdiv(sample_rate)
    envelope = [elapsed / 0.008, remaining / 0.018, 1.0].min.clamp(0.0, 1.0)
    samples[index] = (Math.sin(2.0 * Math::PI * frequency * elapsed) * envelope * 8_000).to_i
  end
end
pcm = samples.pack('s<*')
header = ['RIFF', 36 + pcm.bytesize, 'WAVE', 'fmt ', 16, 1, 1, sample_rate,
          sample_rate * 2, 2, 16, 'data', pcm.bytesize].pack('A4VA4A4VvvVVvvA4V')
File.binwrite(path, header + pcm)
RUBY
        rsync -a "$chime_file" "$REMOTE_HOST:$REMOTE_DIR/launch-chime.wav"
        ssh "$REMOTE_HOST" bash -s -- "$REMOTE_DIR" "$AVD" "$EMULATOR_SERIAL" <<'REMOTE'
set -euo pipefail
remote_dir=$1
avd=$2
serial=$3
sdk=/home/puzzleduck/Android/Sdk
adb=$sdk/platform-tools/adb
emulator=$sdk/emulator/emulator

# Audible warning must succeed before a remote process opens on the user's desktop.
XDG_RUNTIME_DIR=/run/user/$(id -u) pw-play "$remote_dir/launch-chime.wav"

xauth=$(ps -ef | awk '/Xwayland :0/ {for (i=1;i<=NF;i++) if ($i=="-auth") print $(i+1)}' | head -1)
export DISPLAY=:0
export XDG_RUNTIME_DIR=/run/user/$(id -u)
[[ -n "$xauth" ]] && export XAUTHORITY=$xauth
[[ "$serial" =~ ^emulator-([0-9]+)$ ]] || { echo "Expected emulator serial" >&2; exit 2; }
port=${BASH_REMATCH[1]}
nohup "$emulator" -avd "$avd" -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot -port "$port" \
    >"$remote_dir/emulator.log" 2>&1 &

for _ in $(seq 1 120); do
    if "$adb" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | grep -q 1; then
        echo "Emulator boot completed: $serial"
        exit 0
    fi
    sleep 2
done
echo "Timed out waiting for emulator $serial" >&2
exit 1
REMOTE
    fi
fi

if $install_app; then
    ssh "$REMOTE_HOST" "/home/puzzleduck/Android/Sdk/platform-tools/adb -s '$EMULATOR_SERIAL' install -r '$REMOTE_APK'"
fi

if $start_app; then
    remote_adb=/home/puzzleduck/Android/Sdk/platform-tools/adb
    ssh "$REMOTE_HOST" "$remote_adb -s '$EMULATOR_SERIAL' shell input keyevent WAKEUP >/dev/null && $remote_adb -s '$EMULATOR_SERIAL' shell am start -n '$PACKAGE_NAME/$MAIN_ACTIVITY' >/dev/null"
    stable_checks=0
    for _ in $(seq 1 15); do
        resumed=$(ssh "$REMOTE_HOST" "$remote_adb -s '$EMULATOR_SERIAL' shell dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity' | head -1" || true)
        if [[ "$resumed" == *"$PACKAGE_NAME"* ]]; then
            stable_checks=$((stable_checks + 1))
            if ((stable_checks >= 2)); then
                echo "$resumed"
                break
            fi
        else
            stable_checks=0
        fi
        sleep 1
    done
    if ((stable_checks < 2)); then
        echo "App did not remain the resumed activity: $PACKAGE_NAME" >&2
        exit 1
    fi
fi

if [[ -n "$screenshot_path" ]]; then
    mkdir -p "$(dirname "$screenshot_path")"
    display_option=""
    if [[ -n "${KUNLUN_DISPLAY_ID:-}" ]]; then
        [[ "$KUNLUN_DISPLAY_ID" =~ ^[0-9]+$ ]] || { echo "Invalid display id" >&2; exit 2; }
        display_option="-d $KUNLUN_DISPLAY_ID"
    fi
    ssh "$REMOTE_HOST" "/home/puzzleduck/Android/Sdk/platform-tools/adb -s '$EMULATOR_SERIAL' exec-out screencap -p $display_option" > "$screenshot_path"
    file "$screenshot_path" | grep -q 'PNG image data' || {
        rm -f "$screenshot_path"
        echo "Screenshot capture did not produce a PNG" >&2
        exit 1
    }
    echo "Saved screenshot: $screenshot_path"
fi
