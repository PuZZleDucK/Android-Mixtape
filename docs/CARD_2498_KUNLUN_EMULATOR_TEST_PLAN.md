# Card 2498 — Kunlun emulator testing contract

## Scope

This Testing-stage contract defines the checks the Running agent must satisfy while setting up the Kunlun emulator, installing Android Mixtape, and seeding synthetic music tracks. It is intentionally focused on environment/app smoke verification, not product source changes.

Planning context: `docs/CARD_2498_KUNLUN_EMULATOR_PLAN.md`.

Preflight evidence captured during Testing: `docs/evidence/card2498-testing-preflight.txt`.

## Current preflight facts

- Local APK candidate exists: `app/build/outputs/apk/modern/debug/app-modern-debug.apk`.
  - Size observed: `816802772` bytes.
  - SHA-256 observed: `d2e5bac0bb3d8393f5d78ec618125e856a5ed931b0ee5155bb055e14662329cc`.
- Kunlun has Android SDK tools at `$HOME/Android/Sdk`.
- Kunlun has AVD `api24-mixtape-portrait`.
- Kunlun currently lists both a physical Samsung device and `emulator-5554`; every adb command for this card must include `-s emulator-5554`.
- `emulator-5554` is already booted as Android API 24 / Android 7.0 in the preflight snapshot.
- `/data` free space in preflight was about `5.5G`; per project `AGENTS.md`, storage exhaustion is an environment limitation to document and work around, not a product failure.

## Test matrix

### 1. Emulator target check

Command:

```sh
ssh kunlun.local 'export ANDROID_HOME="$HOME/Android/Sdk"; export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"; \
  adb devices -l; \
  test "$(adb -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | tr -d "\r")" = "1"; \
  test "$(adb -s emulator-5554 shell getprop ro.build.version.sdk 2>/dev/null | tr -d "\r")" = "24"'
```

Expected pass condition:

- `emulator-5554` is present as `device`.
- `sys.boot_completed=1`.
- `ro.build.version.sdk=24`.

### 2. APK install check

Command:

```sh
APK=/home/puzzleduck/x/android-mixtape/app/build/outputs/apk/modern/debug/app-modern-debug.apk
stat -c '%n %s bytes %y' "$APK"
sha256sum "$APK"
rsync -av "$APK" kunlun.local:/tmp/android-mixtape-modern-debug.apk
ssh kunlun.local 'export ANDROID_HOME="$HOME/Android/Sdk"; export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"; \
  adb -s emulator-5554 install -r /tmp/android-mixtape-modern-debug.apk; \
  adb -s emulator-5554 shell pm path com.example.androidmixtape'
```

Expected pass condition:

- Install exits successfully.
- `pm path com.example.androidmixtape` returns at least one APK path.
- If install fails due to storage, collect `adb -s emulator-5554 shell df -h /data`, APK size, and exact install output, then retry on a clean/wiped compatible emulator.

### 3. Chatterbox sample-track generation check

Generate two short WAV files on Artigas without playback:

```sh
mkdir -p /tmp/android-mixtape-chatterbox
/home/puzzleduck/pi/skills/chatterbox-tts/scripts/chatterbox_api.rb generate anime.wav \
  'Mixtape sample track one. A short synthetic voice clip for media library testing.' \
  -o /tmp/android-mixtape-chatterbox/mixtape-sample-01.wav --no-play
/home/puzzleduck/pi/skills/chatterbox-tts/scripts/chatterbox_api.rb generate australian.wav \
  'Mixtape sample track two. This clip checks that multiple audio files appear in the app.' \
  -o /tmp/android-mixtape-chatterbox/mixtape-sample-02.wav --no-play
ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 /tmp/android-mixtape-chatterbox/*.wav
```

Expected pass condition:

- Both files exist and are non-empty.
- `ffprobe` reports positive durations for both files.
- Running evidence records exact paths and durations.

### 4. Emulator media seeding and MediaStore check

Command:

```sh
rsync -av /tmp/android-mixtape-chatterbox/ kunlun.local:/tmp/android-mixtape-chatterbox/
ssh kunlun.local 'export ANDROID_HOME="$HOME/Android/Sdk"; export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"; set -e
  adb -s emulator-5554 shell mkdir -p /sdcard/Music/AndroidMixtapeSmoke
  adb -s emulator-5554 push /tmp/android-mixtape-chatterbox/mixtape-sample-01.wav /sdcard/Music/AndroidMixtapeSmoke/mixtape-sample-01.wav
  adb -s emulator-5554 push /tmp/android-mixtape-chatterbox/mixtape-sample-02.wav /sdcard/Music/AndroidMixtapeSmoke/mixtape-sample-02.wav
  adb -s emulator-5554 shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/AndroidMixtapeSmoke/mixtape-sample-01.wav
  adb -s emulator-5554 shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/AndroidMixtapeSmoke/mixtape-sample-02.wav
  adb -s emulator-5554 shell ls -l /sdcard/Music/AndroidMixtapeSmoke
  adb -s emulator-5554 shell content query --uri content://media/external/audio/media | grep -i "mixtape-sample"
'
```

Expected pass condition:

- Both WAV files are present under `/sdcard/Music/AndroidMixtapeSmoke/`.
- MediaStore query shows both `mixtape-sample-01` and `mixtape-sample-02`, or the card documents any scanner delay and captures app-visible proof instead.

### 5. Permission, launch, and crash smoke check

Command:

```sh
ssh kunlun.local 'export ANDROID_HOME="$HOME/Android/Sdk"; export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"; set -e
  adb -s emulator-5554 shell pm grant com.example.androidmixtape android.permission.READ_EXTERNAL_STORAGE || true
  adb -s emulator-5554 logcat -c
  adb -s emulator-5554 shell monkey -p com.example.androidmixtape -c android.intent.category.LAUNCHER 1
  sleep 5
  adb -s emulator-5554 shell pidof com.example.androidmixtape
  adb -s emulator-5554 logcat -d -t 300 | grep -E "FATAL EXCEPTION|AndroidRuntime" && exit 1 || true
  adb -s emulator-5554 exec-out screencap -p > /tmp/android-mixtape-emulator-card2498.png
'
scp kunlun.local:/tmp/android-mixtape-emulator-card2498.png docs/evidence/card2498-running-emulator.png
```

Expected pass condition:

- Permission grant is either successful or harmlessly already granted/not needed.
- Launch command starts `com.example.androidmixtape`.
- `pidof com.example.androidmixtape` returns a PID after launch.
- Recent logcat has no `FATAL EXCEPTION` / `AndroidRuntime` crash for the app.
- A screenshot is saved as `docs/evidence/card2498-running-emulator.png`.

## Running-stage evidence to leave on the card

The Running comment should include:

- APK path, size, and SHA-256 installed.
- Emulator serial, API level, and boot status.
- Sample WAV filenames, durations, and emulator destination paths.
- MediaStore/app visibility result.
- Launch/crash smoke result.
- Evidence file paths, preferably:
  - `docs/evidence/card2498-running-verification.txt`
  - `docs/evidence/card2498-running-emulator.png`

## Move-forward rule

Move to Review only after the emulator/APK/sample-track smoke passes or after an explicitly documented environment blocker has been worked around as far as practical. Do not move backward solely for emulator storage exhaustion; document environment facts and retry on a clean compatible target instead.
