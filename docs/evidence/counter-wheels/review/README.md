# Card 4835 review

Review passed against the Planning handoff. No production changes in this stage.

- Inspected the reducer, clipped renderer, transport revisions, shared layout integration and 48dp toggle. The counter still uses the existing mixtape progress value. Only adjacent forward updates animate, with no queued increments.
- Built both modern APK targets and deployed through `kunlun-sync.sh` to Kunlun API 24. Build targets were up to date.
- Independently executed all 267 unit tests with `./gradlew testModernDebugUnitTest --rerun --console=plain`. Zero failures or errors. See `unit-final.txt` and `unit-results.txt`. An earlier full prerequisite rerun exceeded the tool's 200-second limit; retry logs are retained.
- Independently ran `CounterWheelsTest`. Mid-roll platform-scale cancellation, remount and compact clipping passed. The platform-zero-only test skipped at normal scale. Two later tests hit the previously documented test Activity launch problem, with no Compose hierarchy. Running each affected method separately passed: `carry.txt` and `interruptions.txt`. This is not an all-green combined instrumentation run.
- Independently ran `CounterTransportServiceTest`, passing live external transport, Activity recreation, library selection, Stop, Eject and reselection coverage.
- Independently ran `CounterDeckSkinsTest` in landscape and portrait. Both passed for seven skins and both toggle actions. Inspected the committed updated seven-skin orientation images. Counter windows contain readable settled digits.
- Captured fresh emulator carry evidence. `ordered-carry.png` reads left to right: settled 099, 32ms, 64ms, settled 100. Incoming digits enter above, outgoing digits leave below. `carry-32ms.png` preserves full emulator context. This uses the production wheel composable with controlled test values, not live audio.
- Existing unrelated lint errors remain documented in `../implementation.md`. They do not concern this counter change.

## Reproduction

Use the Java 21 and Android SDK environment documented in the Planning handoff. Build and deploy with:

```sh
./gradlew testModernDebugUnitTest --rerun --console=plain
./gradlew assembleModernDebug assembleModernDebugAndroidTest
./kunlun-sync.sh --launch-emulator --install-app
./kunlun-sync.sh --apk app/build/outputs/apk/androidTest/modern/debug/app-modern-debug-androidTest.apk --install-app
```

For each instrumentation class above, run:

```sh
ssh kunlun.local '/home/puzzleduck/Android/Sdk/platform-tools/adb -s emulator-5554 shell am instrument -w -r -e class com.example.androidmixtape.ui.CounterWheelsTest org.puzzleduck.mixtape.test/androidx.test.runner.AndroidJUnitRunner'
```

Use `ClassName#methodName` to isolate the affected methods. The transport class is in the `playback` package. See logs for exact test names and outcomes.

Rotation settings were restored. The owned emulator and idle Gradle/Kotlin daemons were stopped. Remote test screenshots were removed after copying evidence. Disposable review files remain under ignored `.work/card-4835/review/`. Binary evidence pointers were checked in the Git index, recorded in `lfs.txt`. Unrelated working-tree changes were preserved. No push.
