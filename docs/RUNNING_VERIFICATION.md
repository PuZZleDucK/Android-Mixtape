# Running verification — Kanban #2111

Date: 2026-06-26

Environment used by this worker:

- Java: Temurin 21.0.10 via asdf
- Android SDK: `$HOME/Android/Sdk`
- Compile/target SDK: 35
- Min SDK: 24
- Connected device: Sony F3115, Android 7.0 / API 24 (`RQ3002PT6X`)

Rework completed:

- Lowered `defaultConfig.minSdk` from 26 to 24 so the F3115 can install and run the app.
- Kept API-aware permission behavior:
  - SDK 33+: `READ_MEDIA_AUDIO`
  - SDK 24–32: `READ_EXTERNAL_STORAGE`
- Added explicit SDK 24 unit-test coverage for the permission policy.
- Adjusted the mini-player layout so playback controls and seek are visible on the F3115 landscape screen.
- Generated the debug-signed sideload APK and checksum under `dist/`.

Passing command-line checks:

```sh
./gradlew --no-daemon testDebugUnitTest
./gradlew --no-daemon assembleDebug
./gradlew --no-daemon lintDebug
./gradlew --no-daemon connectedDebugAndroidTest
```

Connected-device result:

- `connectedDebugAndroidTest` ran on `F3115 - 7.0` and passed 2/2 tests.
- This is no longer a minSdk compatibility skip; the APK installs on API 24.

Sideload artifact:

```text
dist/android-mixtape-v0.1.0-debug.apk
dist/android-mixtape-v0.1.0-debug.apk.sha256
```

Current SHA-256:

```text
b1336d6084621a34286a504adeb45a1d4c78b3ba5dfdfcaaaebbc61b0a467264  dist/android-mixtape-v0.1.0-debug.apk
```

Manual smoke on F3115 API 24:

1. Installed the generated APK with `adb install -r dist/android-mixtape-v0.1.0-debug.apk`.
2. Generated a short sine-wave MP3 and pushed it to `/sdcard/Music/mixtape-smoke.mp3`.
3. Launched Mixtape, used the runtime permission prompt, and granted legacy storage/media access.
4. Verified the app scanned the device and displayed one local track, `Mixtape Smoke 440Hz`.
5. Verified the mini-player rendered the track, duration, seek control, and Previous/Play/Next controls on the API 24 device.
6. Tapped Play and verified the UI switched to Pause without crashing; then exercised pause/resume, seek, and refresh.
7. Revoked `READ_EXTERNAL_STORAGE`, relaunched, and verified the permission-required state returned.

Evidence screenshots are in `docs/evidence/`:

- `f3115-api24-mixtape-compact-ready.png`
- `f3115-api24-mixtape-playing.png`
- `f3115-api24-mixtape-after-controls.png`
- `f3115-api24-mixtape-permission-required.png`
