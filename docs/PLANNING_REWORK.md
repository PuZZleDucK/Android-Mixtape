# Planning rework — Kanban #2111

Date: 2026-06-26

## Decision

Resolve the Review blocker by revising the app compatibility floor to the available test hardware instead of waiting for a new emulator/device.

- Set `minSdk` to **24** (Android 7.0) so the attached Sony F3115 device (`ro.build.version.sdk=24`) can install and run the app.
- Keep `compileSdk`/`targetSdk` at 35.
- Keep the current permission split:
  - API 33+: `READ_MEDIA_AUDIO`
  - API 24–32: `READ_EXTERNAL_STORAGE` with `android:maxSdkVersion="32"`
- Preserve Media3/ExoPlayer + Compose architecture; the current dependencies support this lower floor.

## New user requirement

The deliverable must include a sideloadable APK.

Running should produce a debug-signed universal APK and copy it to a stable artifact path, for example:

```text
/home/puzzleduck/x/android-mixtape/dist/android-mixtape-v0.1.0-debug.apk
/home/puzzleduck/x/android-mixtape/dist/android-mixtape-v0.1.0-debug.apk.sha256
```

A debug APK is acceptable for this MVP because it is signed by the Android debug key and can be installed with `adb install` or normal Android sideload flows after enabling "install unknown apps". If a release/shareable APK is needed later, add a separate signing-card/keystore task rather than blocking this MVP.

## Running-stage tasks

1. Change `app/build.gradle.kts` `defaultConfig.minSdk` from 26 to 24.
2. Update README/docs wording from Android 8+ to Android 7+ where applicable.
3. Add or update tests for permission policy to explicitly cover SDK 24, SDK 32, and SDK 33+.
4. Build and verify command-line gates:
   ```sh
   ./gradlew --no-daemon testDebugUnitTest
   ./gradlew --no-daemon assembleDebug
   ./gradlew --no-daemon lintDebug
   ```
5. Create/copy the sideload artifact under `dist/` and generate a SHA-256 checksum:
   ```sh
   mkdir -p dist
   cp app/build/outputs/apk/debug/app-debug.apk dist/android-mixtape-v0.1.0-debug.apk
   sha256sum dist/android-mixtape-v0.1.0-debug.apk > dist/android-mixtape-v0.1.0-debug.apk.sha256
   ```
6. Verify the APK is installable on the attached API 24 device:
   ```sh
   adb install -r dist/android-mixtape-v0.1.0-debug.apk
   ./gradlew --no-daemon connectedDebugAndroidTest
   ```
7. Perform manual smoke on the API 24 device:
   - Push or otherwise place a small audio file in `/sdcard/Music/`.
   - Launch Mixtape.
   - Grant audio/storage permission.
   - Confirm the file appears.
   - Play the file and exercise pause/resume, seek, next/previous, and refresh.
   - Capture screenshot/log evidence for Review.

## Acceptance criteria for Review

- Project builds cleanly from `/home/puzzleduck/x/android-mixtape`.
- A sideloadable APK exists at the documented `dist/` path and has a checksum.
- The APK installs on the available F3115 API 24 device.
- Connected tests run on the API 24 device, or any failure is a real test/app failure with logs rather than a minSdk incompatibility.
- Manual smoke verifies local audio discovery and playback on the API 24 device.
- README documents both Gradle build/install and direct sideload APK usage.

## Risks and mitigations

- **Older-platform behavior:** API 24 storage permissions use `READ_EXTERNAL_STORAGE`; keep the manifest maxSdk and runtime request path covered by unit tests.
- **Audio seed availability:** emulators/devices may start with no music; generate or copy a short smoke audio file before testing.
- **Debug APK expectations:** debug signing is installable but not a polished app-store release; document that clearly.
