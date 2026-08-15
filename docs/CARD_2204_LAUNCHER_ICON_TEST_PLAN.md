# Card 2204 launcher icon test plan

Goal: prove both APK variants use the approved card #2193 cassette logo as the launcher icon while preserving API 19 bitmap compatibility.

## Automated contract checks

Run before implementation to see the expected TDD failures, then keep green after implementation. In worker shells that do not load asdf shims, export Java/Android SDK paths first:

```sh
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/35.0.0:$PATH"
./gradlew testModernDebugUnitTest --tests com.example.androidmixtape.icons.LauncherIconResourceContractTest
./gradlew testLegacyDebugUnitTest --tests com.example.androidmixtape.icons.LauncherIconResourceContractTest
```

The contract requires:

- `app/icon-source/ic_launcher_cassette_1024.png` exists and is a readable 1024x1024 PNG source master.
- Legacy bitmap launcher resources exist as `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` at 48/72/96/144/192 px.
- Every manifest launcher icon ref (`android:icon` and `android:roundIcon`) has an Android 8+ adaptive resource in `app/src/main/res/mipmap-anydpi-v26/` with background and foreground layers.

## Build checks after implementation

```sh
./gradlew assembleModernDebug assembleLegacyDebug
mkdir -p dist
cp app/build/outputs/apk/modern/debug/app-modern-debug.apk dist/android-mixtape-v0.1.0-modern-debug.apk
cp app/build/outputs/apk/legacy/debug/app-legacy-debug.apk dist/android-mixtape-v0.1.0-legacy-debug.apk
sha256sum dist/android-mixtape-v0.1.0-modern-debug.apk > dist/android-mixtape-v0.1.0-modern-debug.apk.sha256
sha256sum dist/android-mixtape-v0.1.0-legacy-debug.apk > dist/android-mixtape-v0.1.0-legacy-debug.apk.sha256
```

Inspect both APKs with `zipinfo`/`unzip -l` and ImageMagick extraction as needed:

- Modern APK includes adaptive `res/mipmap-anydpi-v26*/ic_launcher*.xml` plus cassette bitmap fallbacks.
- Legacy APK includes cassette bitmap `ic_launcher.png` resources and does not rely only on adaptive XML.
- No packaged launcher icon is the old 32x32 `mipmap-hdpi/ic_launcher.png` asset.

## Device acceptance checks

Named physical devices are preferred real-world coverage only. If the Nokia G60 or Samsung SM-G360G/API 19 are unavailable, do not block the card solely on that basis; use APK resource inspection plus any available compatible physical device or emulator, and clearly document which hardware was and was not checked.

1. Record `adb devices -l` evidence for whatever compatible device or emulator is available before install.
2. Reinstall `dist/android-mixtape-v0.1.0-modern-debug.apk` on a compatible modern Android device/emulator; use the Nokia G60 if it is available.
3. Reinstall `dist/android-mixtape-v0.1.0-legacy-debug.apk` on a compatible API 19+ legacy-capable device/emulator; use the Samsung SM-G360G/API 19 if it is available.
4. Refresh launcher state if needed by clearing launcher cache or rebooting.
5. Confirm the Mixtape launcher icon is the generated retro cassette logo on checked devices/emulators.
6. Capture Review evidence: screenshot/photo per checked target showing the launcher icon, plus install command output or package info where practical.
7. If Android 8+ adaptive masking crops the cassette on a modern device, regenerate the foreground with more safe-zone padding and repeat the checks.
8. If no compatible runtime target is available, proceed with automated contract checks, APK inspection evidence, and a clear note that device visual verification remains unperformed rather than making named hardware a blocker.
