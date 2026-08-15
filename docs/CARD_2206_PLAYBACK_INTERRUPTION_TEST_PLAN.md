# Card 2206 playback interruption test plan

Bug: turning the screen off or changing orientation interrupts playback.

## Regression coverage added

1. `MixtapeViewModelTest.repeatedGrantedPermissionDoesNotReloadOrInterruptActivePlayback`
   - Simulates modern `MainActivity.onCreate` calling the granted-permission startup path again after playback starts.
   - Expected behavior: no second repository load, selected track remains the same, and `isPlaying` remains true.
   - Current failure proves the recreation path rescans/reloads and clears active playback state.

2. `PlaybackContinuityContractTest.manifestAllowsPartialWakeLockForScreenOffPlayback`
   - Requires `android.permission.WAKE_LOCK` in `app/src/main/AndroidManifest.xml`.

3. `PlaybackContinuityContractTest.modernExoPlayerUsesLocalWakeMode`
   - Requires the modern ExoPlayer engine to enable local wake mode, e.g. `setWakeMode(C.WAKE_MODE_LOCAL)`.

4. `PlaybackContinuityContractTest.legacyMediaPlayerUsesPartialWakeLockMode`
   - Requires the legacy `MediaPlayer` engine to call `setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)` before prepare/start.

5. `PlaybackContinuityContractTest.legacyActivityDoesNotReleasePlaybackDuringConfigurationChange`
   - Requires the legacy activity not to release playback during orientation-driven destruction, either by guarding `controller.release()` with `!isChangingConfigurations` or by moving playback ownership to retained state outside the activity.

## Verification evidence

Command run from `/home/puzzleduck/x/android-mixtape`:

```sh
PATH="$HOME/.asdf/shims:$HOME/.asdf/bin:$PATH" \
ANDROID_HOME="$HOME/Android/Sdk" \
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
```

Result: expected TDD failure. `testLegacyDebugUnitTest` reported 56 tests, 5 failures. A follow-up modern-only run also reported 56 tests, 5 failures. The failing tests are the five regression checks listed above.

## Running-stage target

Make those five tests pass without regressing the existing unit/contract tests, then run:

```sh
PATH="$HOME/.asdf/shims:$HOME/.asdf/bin:$PATH" \
ANDROID_HOME="$HOME/Android/Sdk" \
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest assembleModernDebug assembleLegacyDebug
```

If a compatible emulator/device is available, perform a manual smoke check: start playback, rotate the device, confirm audio/state continue, then turn the screen off for 30-60 seconds and confirm playback continues after unlock.
