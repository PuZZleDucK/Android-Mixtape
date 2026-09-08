# Live library selection and compact counter fix

The final Kunlun API 24 run passed `CounterTransportServiceTest`, including two visible library spine selections, playback through the real service, Stop, Eject and reselection. The library comes from the production MediaStore scan; no UI state is injected. Existing emulator audio was used. A temporary 30-second silent WAV was also scanned, but the selected group used existing short tracks.

Stop resets the current track, not the entire queue. Those short tracks can advance before Stop is clicked. The first attempt wrongly expected 000. The corrected test calculates the expected counter from the production queue, current index and position, then checks that rendered value immediately and after 500ms. Both final selections settled on 020. `initial-test.txt` records the fixture assertion mistake; `test.txt` and `assertions.txt` record the passing final run.

The screenshot revealed bottom-clipped numerals in the compact landscape deck. `CounterWheels` now centers its measured text line when the parent constrains its height. Font, digit width, counter window and playback semantics are unchanged. A new 16dp-window pixel regression checks that settled numerals leave space at both vertical edges.

## Verification

- `testModernDebugUnitTest`: 267 tests, zero failures or errors.
- Modern app and test APKs built and installed through `kunlun-sync.sh`.
- `CounterTransportServiceTest`: one passing test, including the earlier external transport and Activity recreation assertions.
- `CounterWheelsTest`: five passed, one assumption skip. The platform-zero-only test skips at normal scale. The mid-roll scale-zero test passed and restored the original scale. Single/carry pixels, repeated updates, remount and compact clipping passed.
- `CounterDeckSkinsTest`: passed separately in portrait and landscape for all seven skins. Updated settled/carry images are in `skins/`.
- `library-selection-0.png` and `library-selection-1.png` show the live deck after Stop. These are settled screenshots, not animation frames. Earlier ordered downward roll evidence remains in `../ordered-rolls.png`.

Reproduce with a scanned local audio library and both APKs deployed:

```sh
./gradlew testModernDebugUnitTest assembleModernDebug assembleModernDebugAndroidTest
./kunlun-sync.sh --launch-emulator --install-app
./kunlun-sync.sh --apk app/build/outputs/apk/androidTest/modern/debug/app-modern-debug-androidTest.apk --install-app
ssh kunlun.local '/home/puzzleduck/Android/Sdk/platform-tools/adb -s emulator-5554 shell am instrument -w -r -e class com.example.androidmixtape.playback.CounterTransportServiceTest org.puzzleduck.mixtape.test/androidx.test.runner.AndroidJUnitRunner'
```

Run `com.example.androidmixtape.ui.CounterWheelsTest` and `com.example.androidmixtape.ui.CounterDeckSkinsTest` separately with the same instrumentation command. For skins, disable accelerometer rotation and run once with system `user_rotation` 0 and once with 1, restoring original values afterward.

The initial shell lacked Java and SDK environment variables. Successful builds used the installed Java 21 and `$HOME/Android/Sdk`. One deployment attempt timed out; its retry installed successfully. Logs retain these attempts.

The owned emulator and idle Gradle/Kotlin daemons were stopped and verified exited. Rotation settings were restored. Temporary audio and remote evidence copies were removed. Deleting the temporary audio's MediaStore row failed due to shell quoting, so that stale metadata row may remain until the next media scan. No physical device was accessed. Prior unrelated lint errors remain documented in `../implementation.md`.
