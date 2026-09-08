# Counter composition remount regression

Card 4835. `CounterWheelsTest#remountDuringCarryStartsAtLatestValueWithoutStaleCompletion` passed on Kunlun API 24, one test, no failures.

The test captures a 099 to 100 carry at 32ms, removes the counter from composition for a frame, changes its retained input to 321, and mounts a fresh counter. It verifies the current accessibility value, immediate settled pixels, and identical pixels after another 500ms. The disposed animation cannot restore 100. Five full emulator frames accompany the instrumentation log.

This checks composition disposal and remount, not Activity recreation or a physical rotation. Live-service transport smoke and Activity rotation during playback remain open. No production behavior changed in this follow-up.

Both modern APKs built and deployed through `kunlun-sync.sh`. `deployed.png` records the app deployment. The first build invocation failed because Java was absent from PATH; the successful invocation used the installed Java 21 path and Android SDK explicitly. Both outputs are retained in `build.txt`.

Reproduce after building and deploying the app and test APKs as described in `../implementation.md`:

```sh
ssh kunlun.local '/home/puzzleduck/Android/Sdk/platform-tools/adb -s emulator-5554 shell am instrument -w -r -e class com.example.androidmixtape.ui.CounterWheelsTest#remountDuringCarryStartsAtLatestValueWithoutStaleCompletion org.puzzleduck.mixtape.test/androidx.test.runner.AndroidJUnitRunner'
```
