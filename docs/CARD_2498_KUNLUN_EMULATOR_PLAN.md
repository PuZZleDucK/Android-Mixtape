# Card 2498 — Kunlun Android emulator setup plan

## Goal

Run Android Mixtape on the Kunlun Android emulator, install the latest modern debug APK, seed the emulator with short Chatterbox-generated audio files, and leave clear verification evidence for the card.

## Current environment facts observed during Planning

- Kunlun is reachable over SSH as `kunlun.local`.
- Android SDK is installed at `/home/puzzleduck/Android/Sdk` on Kunlun.
- Useful tools are present under that SDK:
  - `/home/puzzleduck/Android/Sdk/platform-tools/adb`
  - `/home/puzzleduck/Android/Sdk/emulator/emulator`
- A target AVD already exists: `api24-mixtape-portrait`.
- The AVD config is suitable for the modern flavor:
  - API/system image: `system-images/android-24/google_apis/x86_64/`
  - ABI: `x86_64`
  - RAM: `1536M`
  - data partition: `6442450944` bytes (~6 GiB)
  - Play Store disabled
- At planning time, `adb devices -l` on Kunlun showed both a physical Samsung device and `emulator-5554`; all emulator work should target `-s emulator-5554` explicitly.
- The current newest local APK artifact is `/home/puzzleduck/x/android-mixtape/app/build/outputs/apk/modern/debug/app-modern-debug.apk`.
- The modern package id is `com.example.androidmixtape`; API 24 runtime permission is `android.permission.READ_EXTERNAL_STORAGE`.

## Recommended Running-stage plan

1. **Prepare shell environment on Kunlun**

   ```sh
   export ANDROID_HOME="$HOME/Android/Sdk"
   export ANDROID_SDK_ROOT="$ANDROID_HOME"
   export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
   adb devices -l
   ```

2. **Start or reuse the API 24 emulator**

   If `emulator-5554` is already listed as `device`, reuse it. Otherwise start the existing AVD headlessly:

   ```sh
   mkdir -p "$HOME/logs"
   nohup "$ANDROID_HOME/emulator/emulator" \
     -avd api24-mixtape-portrait \
     -no-window -no-audio -no-boot-anim \
     -gpu swiftshader_indirect \
     -no-snapshot -port 5554 \
     > "$HOME/logs/android-mixtape-emulator.log" 2>&1 &

   adb -s emulator-5554 wait-for-device
   until [ "$(adb -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 3; done
   adb -s emulator-5554 shell getprop ro.build.version.sdk
   ```

3. **Build or choose the latest APK on Artigas**

   From `/home/puzzleduck/x/android-mixtape`, either use the existing newest output or rebuild if source freshness is uncertain:

   ```sh
   ./gradlew assembleModernDebug
   APK=/home/puzzleduck/x/android-mixtape/app/build/outputs/apk/modern/debug/app-modern-debug.apk
   ls -lh "$APK"
   ```

4. **Copy the APK to Kunlun and install it on the emulator**

   ```sh
   rsync -av "$APK" kunlun.local:/tmp/android-mixtape-modern-debug.apk
   ssh kunlun.local 'export ANDROID_HOME="$HOME/Android/Sdk"; export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"; adb -s emulator-5554 install -r /tmp/android-mixtape-modern-debug.apk'
   ```

   If installation fails with storage exhaustion, follow `AGENTS.md`: document emulator `/data` free space, APK size, and command output; then retry on a clean/wiped compatible emulator rather than treating storage as an app failure.

5. **Generate short Chatterbox sample tracks**

   On Artigas, use the Chatterbox helper to create brief WAVs without autoplay. Example prompt set:

   ```sh
   mkdir -p /tmp/android-mixtape-chatterbox
   /home/puzzleduck/pi/skills/chatterbox-tts/scripts/chatterbox_api.rb generate anime.wav \
     'Mixtape sample track one. A short synthetic voice clip for media library testing.' \
     -o /tmp/android-mixtape-chatterbox/mixtape-sample-01.wav --no-play
   /home/puzzleduck/pi/skills/chatterbox-tts/scripts/chatterbox_api.rb generate australian.wav \
     'Mixtape sample track two. This clip checks that multiple audio files appear in the app.' \
     -o /tmp/android-mixtape-chatterbox/mixtape-sample-02.wav --no-play
   ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 /tmp/android-mixtape-chatterbox/*.wav
   rsync -av /tmp/android-mixtape-chatterbox/ kunlun.local:/tmp/android-mixtape-chatterbox/
   ```

6. **Push sample tracks into emulator music storage and trigger scan**

   ```sh
   ssh kunlun.local 'export ANDROID_HOME="$HOME/Android/Sdk"; export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"; set -e
     adb -s emulator-5554 shell mkdir -p /sdcard/Music/AndroidMixtapeSmoke
     adb -s emulator-5554 push /tmp/android-mixtape-chatterbox/mixtape-sample-01.wav /sdcard/Music/AndroidMixtapeSmoke/mixtape-sample-01.wav
     adb -s emulator-5554 push /tmp/android-mixtape-chatterbox/mixtape-sample-02.wav /sdcard/Music/AndroidMixtapeSmoke/mixtape-sample-02.wav
     adb -s emulator-5554 shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/AndroidMixtapeSmoke/mixtape-sample-01.wav
     adb -s emulator-5554 shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/AndroidMixtapeSmoke/mixtape-sample-02.wav
     adb -s emulator-5554 shell content query --uri content://media/external/audio/media | grep -i "mixtape-sample" || true
   '
   ```

7. **Grant permission, launch, and smoke check**

   ```sh
   ssh kunlun.local 'export ANDROID_HOME="$HOME/Android/Sdk"; export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"; set -e
     adb -s emulator-5554 shell pm grant com.example.androidmixtape android.permission.READ_EXTERNAL_STORAGE || true
     adb -s emulator-5554 shell monkey -p com.example.androidmixtape -c android.intent.category.LAUNCHER 1
     adb -s emulator-5554 shell pidof com.example.androidmixtape
   '
   ```

   If UI verification is required from a headless emulator, capture evidence with:

   ```sh
   adb -s emulator-5554 exec-out screencap -p > /tmp/android-mixtape-emulator.png
   ```

## Success criteria

- Kunlun has a booted compatible emulator and `adb -s emulator-5554 shell getprop sys.boot_completed` returns `1`.
- Latest modern debug APK installs successfully on `emulator-5554`.
- At least two short Chatterbox-generated audio files are present under `/sdcard/Music/AndroidMixtapeSmoke/`.
- MediaStore can see the sample tracks, or the app visibly lists them after refresh/launch.
- `com.example.androidmixtape` launches on the emulator with no immediate crash.
- Card comment documents exact APK path/name, emulator id/API level, sample track paths, and any limitations.

## Risks and mitigations

- **Multiple adb targets:** Kunlun also has a physical Samsung device attached; always use `-s emulator-5554`.
- **Emulator storage pressure:** The modern APK can be large. Storage failures should be documented as environment limitations and retried with a clean/wiped emulator or larger data partition.
- **Media indexing delay:** If pushed WAVs do not appear immediately, trigger `MEDIA_SCANNER_SCAN_FILE`, relaunch the app, and use the app refresh action.
- **No GUI window:** The current emulator is headless. Prefer command-line evidence (`adb`, MediaStore query, screencap) unless a visible session is explicitly needed.
