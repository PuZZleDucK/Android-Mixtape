# Card 2309 transport layout TDD plan

## Expected behavior

- Now Playing transport controls in a 320dp portrait surface are ordered left-to-right as: Eject, divider, Stop, Play, Pause, Rewind, Fast-forward.
- Eject is isolated on the far left by a visible divider with content description `Transport control divider`.
- Stop is to the left of Play.
- Existing transport callbacks still fire once for Rewind, Play/Pause, Fast-forward, Stop, and Eject.

## Spec added

- `MixtapeAppTest.nowPlayingTransportControlsPlaceEjectAtFarLeftAndStopBeforePlay`
  - Uses button content-description bounds.
  - Asserts `Eject.left < divider.left < divider.right < Stop.left < Play.left`.
  - Asserts Play/Pause/Rewind/Fast-forward retain a stable order after Stop.
  - Asserts at least a 12dp gap between Eject and Stop on the 320dp Now Playing layout.

## Verification performed

```sh
export PATH="$HOME/.asdf/shims:$PATH"
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew :app:compileModernDebugAndroidTestKotlin
./gradlew :app:connectedModernDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.androidmixtape.ui.MixtapeAppTest#nowPlayingTransportControlsPlaceEjectAtFarLeftAndStopBeforePlay
```

- Android test compilation passes.
- The new connected test currently fails on F3115 / Android 7.0 because the divider is not present yet. This is the intended failing TDD spec for the Running implementation.
