# Android Mixtape

A small native Android app that scans local audio files with `MediaStore`, lists them, and plays selected tracks. The `modern` build keeps the Compose + AndroidX Media3/ExoPlayer experience for Android 7.0+; the `legacy` build installs on Android 4.4/API 19 with a plain Views UI and platform `MediaPlayer` playback.

## Features

- Requests the correct audio permission:
  - Android 13+ (`SDK 33+`): runtime `READ_MEDIA_AUDIO`
  - Android 6–12L (`SDK 23–32`): runtime `READ_EXTERNAL_STORAGE`
  - Android 4.4–5.1 (`SDK 19–22`): install-time `READ_EXTERNAL_STORAGE` with no runtime prompt
- Scans device audio through `MediaStore` and maps tracks to stable `content://media/external/audio/media/{id}` URIs.
- Shows explicit permission-required, loading, empty-library, error, and ready states.
- Modern build: Compose screens with track list, refresh, current track, play/pause, previous/next, elapsed/duration, and seek slider.
- Modern build: Android Auto media-app metadata plus a Cars App Library templated Mixtape UI that opens to the mixtape list, drills into side-of-tape track lists, and shares playback with the phone through the Media3 service.
- Legacy build: plain Android Views list and foreground transport controls for API 19 devices.
- Uses a Media3 session/service-backed ExoPlayer in the modern build and platform `MediaPlayer` in the legacy build.

## Build and test

This repo expects Java 21 from asdf (`.tool-versions` is included) and an Android SDK with platform 35/build-tools installed. The app has two debug variants: `modernDebug` supports Android 7.0+ (`minSdk 24`) and `legacyDebug` supports Android 4.4+ (`minSdk 19`). Both target SDK 35.

```sh
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
./gradlew lintModernDebug lintLegacyDebug
```

If an emulator or device is connected:

```sh
./gradlew connectedModernDebugAndroidTest
./gradlew connectedLegacyDebugAndroidTest
```

The modern connected suite exercises the Compose UI. The legacy connected suite is a launcher smoke test that keeps Compose-only test/runtime dependencies out of the API 19 APK.

## Sideloadable APK

The deliverable includes debug-signed APKs that can be installed with `adb install` or sideloaded after enabling Android's "install unknown apps" flow:

```text
dist/android-mixtape-v0.1.0-modern-debug.apk
dist/android-mixtape-v0.1.0-modern-debug.apk.sha256
dist/android-mixtape-v0.1.0-legacy-debug.apk
dist/android-mixtape-v0.1.0-legacy-debug.apk.sha256
```

Regenerate them after code changes with:

```sh
./gradlew assembleModernDebug assembleLegacyDebug
mkdir -p dist
cp app/build/outputs/apk/modern/debug/app-modern-debug.apk dist/android-mixtape-v0.1.0-modern-debug.apk
cp app/build/outputs/apk/legacy/debug/app-legacy-debug.apk dist/android-mixtape-v0.1.0-legacy-debug.apk
sha256sum dist/android-mixtape-v0.1.0-modern-debug.apk > dist/android-mixtape-v0.1.0-modern-debug.apk.sha256
sha256sum dist/android-mixtape-v0.1.0-legacy-debug.apk > dist/android-mixtape-v0.1.0-legacy-debug.apk.sha256
```

Install the API 19 legacy build on the Samsung SM-G360G:

```sh
adb install -r dist/android-mixtape-v0.1.0-legacy-debug.apk
```

Install the modern build on the Nokia G60 / Android 14 device:

```sh
adb install -r dist/android-mixtape-v0.1.0-modern-debug.apk
```

## Manual smoke test

1. Build and install the appropriate debug APK:
   ```sh
   ./gradlew assembleModernDebug assembleLegacyDebug
   adb install -r dist/android-mixtape-v0.1.0-legacy-debug.apk   # Samsung SM-G360G / API 19
   adb install -r dist/android-mixtape-v0.1.0-modern-debug.apk   # Nokia G60 / Android 14
   ```
2. Seed one or more audio files:
   ```sh
   adb push sample-audio.mp3 /sdcard/Music/mixtape-smoke.mp3
   ```
3. Launch **Mixtape**. On API 19 the storage permission is install-time; on API 23+ grant the requested runtime audio/storage permission.
4. Confirm the seeded file appears in the list using metadata or filename fallback.
5. Tap the track and verify playback starts.
6. Rotate the device and verify the selected track and playback state continue without restarting the scan.
7. Turn the screen off for 30–60 seconds, unlock, and verify playback continued and transport state is coherent.
8. Exercise pause/resume, seek, next/previous, and refresh.
9. Revoke audio permission in Android settings and verify the permission prompt returns.
10. Remove/rename files and verify the empty-library state.

## Known limitations

- Android Auto support is modern-flavor only; grant the phone audio/media permission in the app before opening the custom Mixtape car UI from a car/head unit.
- The legacy API 19 build intentionally has a simpler plain-Views UI and platform `MediaPlayer` playback rather than Compose/Media3 parity.
- Progress is updated by user seek and selection state in this first version; continuous progress polling can be added later.
- Emulators normally need audio files pushed before the ready state can be tested manually.
