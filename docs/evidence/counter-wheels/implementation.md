# Counter wheels implementation, card 4835

## Implemented

- `CounterWheels` draws the existing measured monospace string through three fixed clips. Changed digits share a 160ms downward tween. Unchanged digits draw once. The counter window also clips its contents. Existing 14sp type, 2sp spacing, colors, leading zeros and progress calculation remain unchanged.
- `CounterWheelState` cancels competing updates, pause, disabled motion, jumps and revision changes. It never queues skipped numbers. Compose coroutine cancellation and the reducer generation protect against stale completions.
- `counterRevision` travels from `PlayerUiState` through `MixtapeUiState` to both deck layouts. Local seeks, stop, selection, queue changes and engine position discontinuities invalidate wheel motion. MediaController now publishes `EVENT_POSITION_DISCONTINUITY`, including external seeks within the same track.
- Android's animator-duration-scale setting has a lifecycle-owned observer. Scale zero settles immediately. Nonzero motion uses Compose's duration-scale-aware `Animatable`.
- The deck toggle now allocates 48dp instead of 64dp. Its weighted spine sibling receives the released space. Click descriptions and actions are unchanged. Case materials and spine typography were not edited.

## Real checks

Final unit/build command, with Java 21 and the Android SDK configured:

```sh
./gradlew testModernDebugUnitTest assembleModernDebug assembleModernDebugAndroidTest --console=plain
```

267 unit tests passed, no failures or errors. This includes eight reducer tests, five new transport-revision tests, two source integration guards and existing controller/timeline tests. Both APKs built successfully. See `final-build.txt`. An initial incremental build had a stale constructor call and failed with `NoSuchMethodError`; rerunning with `-Pkotlin.incremental=false` fixed the generated test bytecode. Subsequent full-suite runs passed.

`./gradlew lintModernDebug` still fails on seven errors outside this change: one `WrongConstant` in `MixtapeMediaLibraryService.kt`, plus six Media3 opt-in errors in that file and `AndroidAutoDiagnostics.kt`. No counter-file error appears. See `final-lint.txt` and `lint.txt`. Those files were not changed for this card.

The modern APK includes x86_64. Both app and instrumentation APKs were deployed with `kunlun-sync.sh` to Kunlun's API 24 `emulator-5554`. See `final-deploy.txt`. No physical device was accessed.

A real `connectedModernDebugAndroidTest` attempt through an owned, verified SSH forward failed before running tests in UTP's split APK installer. `/data` had 5.5G free of 5.8G; the app APK was 16M and test APK 1.1M. This was not storage exhaustion. The listener was stopped. `connected.txt` records the failure. Direct installations through `kunlun-sync.sh` succeeded, followed by actual AndroidJUnitRunner executions on Kunlun:

```sh
# App deployment
./kunlun-sync.sh --apk app/build/outputs/apk/modern/debug/app-modern-debug.apk --install-app
# Instrumentation deployment, separate disposable remote directory
KUNLUN_TEST_DIR=/home/puzzleduck/x/remote-mixtape-testing/card-4835-tests \
  ./kunlun-sync.sh --apk app/build/outputs/apk/androidTest/modern/debug/app-modern-debug-androidTest.apk --install-app

# On Kunlun, always use this explicit emulator serial.
ADB=/home/puzzleduck/Android/Sdk/platform-tools/adb
$ADB -s emulator-5554 shell am instrument -w -r -e class \
com.example.androidmixtape.ui.CounterWheelsTest#singleIncrementAndCarriesKeepFixedCellsAndFinishCorrectly,com.example.androidmixtape.ui.CounterWheelsTest#interruptionsAndRepeatedUpdatesNeverRestoreStaleDigits \
org.puzzleduck.mixtape.test/androidx.test.runner.AndroidJUnitRunner

$ADB -s emulator-5554 shell am instrument -w -r -e class \
com.example.androidmixtape.ui.CounterDeckSkinsTest \
org.puzzleduck.mixtape.test/androidx.test.runner.AndroidJUnitRunner
```

- `final-clock.txt`: two controlled-clock pixel tests passed. Covers 008 to 009, 009 to 010, 099 to 100, unchanged-cell pixels, downward incoming/outgoing pixel bounds, fixed dimensions, final/reference pixel equality, cancellation, pause, disabled motion, adjacent seek and reset.
- `final-landscape-skins.txt` and `final-portrait.txt`: the seven-skin test passed separately in each physical emulator orientation. It captures settled 099 and carry frames, asserts unchanged counter bounds, verifies the 48dp toggle bounds, and clicks both toggle actions for every skin.
- `zero-scale.txt`: the real platform-scale-zero test passed. Set `settings put global animator_duration_scale 0`, run only `CounterWheelsTest#platformZeroScaleSettlesWithoutWheelMotion`, then restore the original setting. The original value was absent, so this run restored it with `settings delete global animator_duration_scale`. Rotation settings were also restored to their original values, accelerometer_rotation 0 and user_rotation 1.

API 24 SurfaceFlinger capture needs a draw-pass wait independent of the frozen Compose clock. The first pixel run captured an activity-window fade and stale submitted pixels. Tests now let that window transition finish and wait 100ms for the Android draw submission without advancing the animation clock. Pixel tests pass after that correction.

One combined multi-class run later failed to find the next test activity's Compose hierarchy. The wheel tests passed in that run; running the skin class independently passed in both orientations. `final-landscape.txt` preserves that failure rather than reporting the batch as green. The independent invocations above are the verified reproduction path.

## Visual evidence

- `ordered-rolls.png`: left to right, before, 32ms, 64ms, finished. Three rows show single-digit and both carry transitions. These are crops of actual emulator frames, not simulated renders. The easing is nonlinear, so clock fractions are not vertical-position fractions.
- `frames/`: original full-screen wheel-test captures.
- `portrait-skins.png`, `landscape-skins.png`: full-deck surveys. Original settled and carry screenshots are in `skins/`.
- `platform-zero-009.png`: disabled-animation rendering.
- `make-contact-sheets.rb`: rebuilds the sheets with ImageMagick. Temporary crops are deleted on exit.

Screenshots use the production composables with deterministic injected UI state. They do not claim a live audio-playback or external media-session smoke test. No new counter timer or counting-rate change was introduced.

## Remaining Running work

The implementation and focused visual checks are committed, but the complete planned live transport smoke and recreation checks have not been performed. Before Review, check pause/resume, seek in both directions, stop, rewind/fast-forward, track/mixtape change and rotation during motion against the live service. Changing platform animation scale to zero during motion now passes the additional test below. Reducer tests cover these state rules; they do not replace live integration evidence. Investigate or clearly retain the API 24 multi-class activity-launch limitation when expanding instrumentation coverage.

## Platform scale change during carry

`CounterWheelsTest#disablingPlatformMotionDuringCarrySettlesImmediately` passed on Kunlun API 24. It starts a production 099 to 100 carry at scale 1, captures the moving frame at 32ms, changes the actual global animator scale to zero through UiAutomation, and checks pixel equality against a settled 100 after one Compose frame. It advances another 500ms and checks that stale glyphs do not return. The test restores the original platform setting in `finally`.

Both modern APKs rebuilt successfully and were installed through `kunlun-sync.sh`. Reproduce with the direct instrumentation command above, replacing the class filter with `com.example.androidmixtape.ui.CounterWheelsTest#disablingPlatformMotionDuringCarrySettlesImmediately`. Build, deploy and test output plus five full emulator frames are in `scale-change/`. This closes only the mid-roll scale check. Live transport and recreation checks remain open.

Keep the pre-existing lint failures separate from this card. Preserve the unrelated dirty working tree. Do not push without the user's explicit request.
