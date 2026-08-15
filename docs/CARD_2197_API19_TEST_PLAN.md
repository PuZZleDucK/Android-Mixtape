# Card 2197 API 19 Legacy Compatibility Test Plan

## Running verification after legacy instrumentation split — 2026-06-30

Implemented the remaining test-packaging cleanup so Compose UI instrumentation lives under `src/androidTestModern` with Compose test dependencies scoped to `androidTestModernImplementation`. Added a `src/androidTestLegacy` launcher smoke test with only API-19-safe test dependencies; `connectedLegacyDebugAndroidTest` now packages and runs without pulling Compose runtime into the legacy test APK.

Verified on the available Sony F3115 / Android 7.0 / API 24 device because the user allowed any available device for testing:

```sh
./gradlew --no-daemon testModernDebugUnitTest testLegacyDebugUnitTest assembleModernDebug assembleLegacyDebug lintModernDebug lintLegacyDebug connectedModernDebugAndroidTest connectedLegacyDebugAndroidTest
```

Results:

- Local modern and legacy unit/build/lint gates passed.
- `connectedModernDebugAndroidTest` passed 10/10 on F3115, covering the previous mix tape selection, cassette preview, now-playing transport/J-card, and settings reset regressions.
- `connectedLegacyDebugAndroidTest` passed 1/1 on F3115 with the new legacy launcher smoke.
- Manual `adb install -r dist/android-mixtape-v0.1.0-modern-debug.apk` and `adb install -r dist/android-mixtape-v0.1.0-legacy-debug.apk` both succeeded on F3115, and both launchers started with live PIDs.
- `aapt dump badging` still reports legacy `sdkVersion:'19'` and modern `sdkVersion:'24'`.

Remaining acceptance note: the true Samsung SM-G360G / Android 4.4.4 hardware was not attached, so F3115 is an available-device proxy. Final SM-G360G install confirmation still requires that physical phone.

## Planning refresh after F3115 review rollback — 2026-06-30

Current repo state: clean at `f359a05` (`Fit mixtape UI on API24 device`) after the review rollback noted four failing modern connected UI tests on the available Sony F3115 / Android 7.0 / API 24 device. The post-fix local test report at `app/build/outputs/androidTest-results/connected/debug/flavors/modern/TEST-F3115 - 7.0-_app-modern.xml` shows the modern connected suite passing 10/10 on F3115 at 2026-06-30 20:59.

### Rework focus for the next stage

1. Preserve the existing API 19 compatibility architecture:
   - `legacyDebug` stays `minSdk 19` with the plain Android Views + platform `MediaPlayer` path.
   - `modernDebug` stays `minSdk 24` with Compose + Media3.
   - Keep Compose/Media3 dependencies out of shared/legacy runtime dependency scopes.
2. Re-run the full local variant gate from a clean checkout:
   ```sh
   export PATH="$HOME/.asdf/bin:$HOME/.asdf/shims:$PATH"
   ./gradlew --no-daemon testModernDebugUnitTest testLegacyDebugUnitTest
   ./gradlew --no-daemon assembleModernDebug assembleLegacyDebug
   ./gradlew --no-daemon lintModernDebug lintLegacyDebug
   ```
3. Re-run available-device gates on the attached F3115 API 24 because the user explicitly allowed any available device for testing:
   ```sh
   adb devices -l
   ./gradlew --no-daemon connectedModernDebugAndroidTest
   adb install -r dist/android-mixtape-v0.1.0-modern-debug.apk
   adb install -r dist/android-mixtape-v0.1.0-legacy-debug.apk
   ```
   The important regression check is that `MixtapeAppTest` remains green 10/10 on F3115 after the compact-layout fix.
4. Clarify legacy instrumentation coverage before Review:
   - If `connectedLegacyDebugAndroidTest` is unavailable or fails because the shared `androidTest` source set depends on Compose-only modern classes, document that as a test-packaging limitation, not an API 19 install blocker.
   - Prefer either moving Compose UI instrumentation under `src/modernAndroidTest` or adding a tiny `src/legacyAndroidTest` launch/install smoke that does not import Compose. Do not let legacy test packaging pull Compose runtime into the legacy APK.
5. Refresh `dist/` artifacts and SHA-256 files after any rebuild, then verify badging:
   ```sh
   aapt dump badging dist/android-mixtape-v0.1.0-legacy-debug.apk | grep sdkVersion
   aapt dump badging dist/android-mixtape-v0.1.0-modern-debug.apk | grep sdkVersion
   ```

### Success criteria for next Review

- Local modern and legacy unit/build/lint gates pass.
- `connectedModernDebugAndroidTest` passes on F3115 API 24, specifically including the previously failing mix tape selection, cassette preview, now-playing transport/J-card, and settings reset tests.
- Both modern and legacy debug APKs install and launch on the available F3115 device.
- APK metadata still reports legacy `sdkVersion:'19'` and modern `sdkVersion:'24'`.
- If true API 19 hardware remains unavailable, the card evidence explicitly states that F3115 is only an available-device proxy and that final SM-G360G acceptance still requires the Samsung phone.

## TDD contracts added in this stage

These checks intentionally fail against the current single-variant Compose/Media3 app and should turn green during Running:

- `AudioPermissionPolicyTest.sdk19To22UsesInstallTimeStoragePermissionWithoutRuntimePrompt`
  - Add `AudioPermissionPolicy.requiredRuntimePermission(sdkInt): String?`.
  - Expected: API 19/22 return `null`; API 23-32 return `READ_EXTERNAL_STORAGE`; API 33+ returns `READ_MEDIA_AUDIO`.
- `Api19VariantContractTest.gradleDeclaresModernAndLegacyDebugVariants`
  - Add Gradle flavors/source sets with `modernDebug` preserving minSdk 24 and `legacyDebug` declaring minSdk 19.
- `Api19VariantContractTest.api21PlusUiAndPlaybackDependenciesAreModernOnly`
  - Move Compose, activity-compose, lifecycle-compose, and Media3 dependencies out of shared `implementation` into `modernImplementation`.
- `Api19VariantContractTest.sourceSetsSeparateModernComposeFromLegacyViews`
  - Move the existing Compose entry point/playback to `src/modern`.
  - Add `src/legacy` plain Android Views UI and API-19-safe platform `MediaPlayer` playback.

## Build gates for Running

Run these before moving to Review:

```sh
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
./gradlew lintModernDebug lintLegacyDebug
```

AGP names unit-test tasks per flavor after this implementation; there is no unflavored `testDebugUnitTest` task once `modern` and `legacy` flavors are declared.

## Device gates for acceptance

Samsung SM-G360G / Android 4.4.4 / API 19:

```sh
adb install -r app/build/outputs/apk/legacy/debug/app-legacy-debug.apk
adb shell getprop ro.build.version.sdk  # expect 19
adb push sample-audio.mp3 /sdcard/Music/mixtape-smoke.mp3
```

Smoke expectations:

1. APK installs without `INSTALL_FAILED_OLDER_SDK`.
2. App launches without Compose/AndroidX API-level crashes.
3. API 19 path does not show a runtime permission prompt; storage access is install-time.
4. Local `/sdcard/Music` audio appears via `MediaStore`.
5. Selecting the track starts foreground playback through the legacy player.
6. Stop/pause and track selection remain stable after rotation or relaunch.

Nokia G60 / Android 14:

```sh
adb install -r app/build/outputs/apk/modern/debug/app-modern-debug.apk
adb shell getprop ro.build.version.sdk  # expect 34 or device value
```

Smoke expectations:

1. Existing Compose UI still launches.
2. Runtime permission is `READ_MEDIA_AUDIO`.
3. Scanning, selection, playback controls, seek, and refresh keep current behavior.

## Documentation gate

Update `README.md`/dist notes with both APK names, install commands, and any known legacy limitations. Acceptable legacy limitations can include a simpler plain-Views UI and foreground-only playback, but scanning and local playback must work on API 19.
