# Card 2198 portrait verification plan

Card: `[android-mixtape] Restyle mix list as retro cassette tape case collection`
Date: 2026-06-30

## Why this plan exists

Review has already accepted the non-connected code direction but rejected the card because the required portrait evidence is missing. The current implementation includes the readability rework (`heightIn(min = 184.dp)`, smaller handwritten text, and three preview lines), but Artigas still has no attached/authorized Android device and no `/dev/kvm`. Review must not receive this card again with only JVM/lint/build evidence.

The actionable connected path is now the prepared API 24 emulator on `kunlun.local` (details below). The physical phone currently attached to Kunlun remains unusable for this app because it is an `SM-G360G` on SDK 19 while the app declares `minSdk = 24`.

## Required evidence before the next Review

Collect one of these, in priority order:

1. **Preferred:** Sony F3115 physical device, Android 7.0 / API 24, portrait orientation.
2. **Currently actionable substitute:** `api24-mixtape-portrait` API 24 emulator on `kunlun.local`, portrait orientation, clearly labelled as emulator evidence rather than target-device evidence.

Do not send this card back to Review with only JVM/lint/build evidence. Review needs connected/manual portrait smoke evidence attached or explicitly documented from one of the two paths above.

## Hardware/access coordination checkpoint

Current planning refresh on 2026-06-30 confirms the blocker is environmental, not implementation:

- Artigas still reports no attached/authorized `adb` devices and no `/dev/kvm`.
- Kunlun has a physical `SM-G360G` device, but it is SDK `19` and cannot install/run the app's `minSdk = 24` build.
- Kunlun now has usable KVM and a prepared API 24 emulator, so Testing can proceed through that emulator substitute path.

Kunlun physical-device check from the earlier refresh:

```sh
ssh kunlun.local 'hostname && command -v adb && adb devices -l && adb shell getprop ro.product.model && adb shell getprop ro.build.version.sdk && adb shell wm size && adb shell wm density'
```

Observed device: `21e098e8`, model `SM-G360G`, SDK `19`, `480x800`, density `240`. This is a real portrait-capable Android device, but it is **not usable for this app build** because `app/build.gradle.kts` declares `minSdk = 24`. Do not send the card to Testing for connected validation against that device unless the product decision changes the minSdk requirement.

## Prepared Kunlun API 24 emulator path

Planning prepared the emulator route on 2026-06-30 06:23 UTC. Verified facts on `kunlun.local`:

- `/dev/kvm` exists and is read/write usable by `puzzleduck`.
- Android command-line tools, platform-tools, emulator, `platforms;android-24`, and `system-images;android-24;google_apis;x86_64` are installed under `/home/puzzleduck/Android/Sdk`.
- AVD `api24-mixtape-portrait` exists at `/home/puzzleduck/.android/avd/api24-mixtape-portrait.avd`.
- `emulator -accel-check` reports `KVM (version 12) is installed and usable`.
- A boot smoke reached `boot_completed=1`, SDK `24`, physical size `1080x1920`; the emulator was then stopped.

Remote environment for all Kunlun emulator commands:

```sh
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

Start the prepared emulator on Kunlun:

```sh
ssh kunlun.local 'bash -s' <<'REMOTE'
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
nohup emulator -avd api24-mixtape-portrait \
  -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect \
  -no-snapshot -port 5554 >/tmp/api24-mixtape-portrait-emulator.log 2>&1 &
REMOTE
```

Wait for it to boot from Artigas:

```sh
ssh kunlun.local 'bash -s' <<'REMOTE'
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
adb -s emulator-5554 wait-for-device
until [ "$(adb -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | tr -d "\r")" = "1" ]; do sleep 2; done
adb -s emulator-5554 shell getprop ro.build.version.sdk
adb -s emulator-5554 shell wm size
REMOTE
```

Expose the remote emulator to the local Artigas build through SSH in a second terminal/session:

```sh
ssh -N -L 5555:127.0.0.1:5555 kunlun.local
```

Then, from `/home/puzzleduck/x/android-mixtape` on Artigas, connect local adb/Gradle to the forwarded emulator:

```sh
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb connect 127.0.0.1:5555
adb devices -l
export ANDROID_SERIAL=127.0.0.1:5555
```

If the SSH-forwarded `adb connect` path is flaky, run the same smoke commands directly over SSH on Kunlun against `emulator-5554`, or copy/check out the project on Kunlun and run Gradle there with the same `JAVA_HOME`/`ANDROID_HOME` values.

Stop the emulator after evidence capture:

```sh
ssh kunlun.local 'bash -s' <<'REMOTE'
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb -s emulator-5554 emu kill
REMOTE
```

## Physical F3115 path

Preflight:

```sh
cd /home/puzzleduck/x/android-mixtape
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb kill-server
adb start-server
adb devices -l
adb shell getprop ro.product.model
adb shell getprop ro.build.version.sdk
```

Pass condition: `adb devices -l` shows an authorized device, model is F3115 or another approved target device, and SDK is `24` or newer. The currently observed Kunlun `SM-G360G` / SDK `19` fails this pass condition because the app minSdk is 24.

If the valid device is on `kunlun.local`, run the same checks/build/test steps from Kunlun or establish an adb-over-TCP/SSH path from the build host. The quickest remote preflight is:

```sh
ssh kunlun.local 'adb devices -l; adb shell getprop ro.product.model; adb shell getprop ro.build.version.sdk'
```

Force portrait for the smoke:

```sh
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
```

Build/install and run connected tests:

```sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug compileDebugAndroidTestKotlin
./gradlew --no-daemon connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Seed at least 13 short audio files so the default MixTapes screen renders two cassette cases:

```sh
mkdir -p /tmp/android-mixtape-smoke-audio
for n in $(seq 1 13); do
  ffmpeg -y -f lavfi -i "sine=frequency=$((330+n*15)):duration=0.4" \
    -metadata title="Smoke Track $n" \
    -metadata artist="Mixtape Smoke" \
    "/tmp/android-mixtape-smoke-audio/Smoke Track $n.mp3"
done
adb shell mkdir -p /sdcard/Music/mixtape-smoke
adb push /tmp/android-mixtape-smoke-audio/. /sdcard/Music/mixtape-smoke/
adb shell am force-stop com.example.androidmixtape
adb shell monkey -p com.example.androidmixtape 1
```

Manual portrait checklist:

- Fresh launch after permission grant opens directly to the MixTapes list, not the track library.
- The first two cassette cases are visible/reachable in portrait with no horizontal scroll.
- Each case reads as a clear plastic cassette storage case with J-card/retro treatment.
- Handwritten-style mix titles are readable and do not overlap the track-count line.
- Preview song names are readable liner-note style; only three previews are expected per case.
- Tapping a cassette case opens Now Playing for that mix and still allows returning to MixTapes.

Capture evidence:

```sh
mkdir -p docs/evidence
adb exec-out screencap -p > docs/evidence/card2198-api24-emulator-portrait-mixtapes.png
adb exec-out screencap -p > docs/evidence/card2198-api24-emulator-portrait-now-playing.png
```

For physical F3115 evidence, use `card2198-f3115-portrait-*.png` names instead. Attach the MixTapes screenshot to the Kanban card comment when moving forward to Review.

## API 24 emulator substitute path from scratch

The prepared Kunlun path above is the preferred emulator route. Use this generic setup only on another host where `/dev/kvm` exists and the user has KVM access.

```sh
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
sdkmanager "platform-tools" "platforms;android-24" "system-images;android-24;google_apis;x86_64" "emulator"
avdmanager create avd -n api24-mixtape-portrait -k "system-images;android-24;google_apis;x86_64" --device "pixel"
emulator -avd api24-mixtape-portrait -no-window -no-snapshot -gpu swiftshader_indirect -no-audio
adb wait-for-device
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
```

Then run the same build, connected test, audio seeding, manual checklist, and screenshot capture steps as the physical-device path. Label screenshots/comments as `API 24 emulator portrait`, not F3115.

## Testing stage success criteria

Testing may move the card forward only when it has done all of the following:

- Confirmed the regression specs still cover the default MixTapes route and cassette readability constraints.
- Run the non-connected gate: `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `compileDebugAndroidTestKotlin`.
- Run `connectedDebugAndroidTest` on F3115/API24 or the prepared Kunlun API24 emulator.
- Performed the manual portrait checklist with 13+ seeded tracks.
- Attached or clearly referenced portrait screenshot evidence showing the cassette case list.

If no authorized API24+ physical device is attached and the prepared Kunlun emulator cannot be booted/reached, Testing should not pass the card forward to Review; it should comment that the verification environment is still blocked and return the card for access coordination rather than repeating non-connected-only evidence.
