# Live service transport regression

`CounterTransportServiceTest#externalTransportPublishesDiscontinuitiesAndStablePause` passed on Kunlun API 24, emulator-5554. It connects both the production MediaControllerPlayerEngine and a second MediaController to the real MixtapeMediaLibraryService. A generated 30-second silent WAV supplies two queue entries without touching the user's library.

Assertions cover ready state, playback, pause with stable position and no additional snapshots over 500ms, external seeks to 10000, 2000, 25000 and 0ms with discontinuity flags and no stale follow-up, track change, resume, stop and queue clearing. The test releases both clients, stops the service and deletes its audio in finally. `assertions.txt` contains device logcat checkpoints; `test.txt` contains the passing runner result.

Both modern APKs built and deployed through kunlun-sync.sh. `build.txt` and `deploy.txt` record the commands' output. Reproduce after deployment:

```sh
ssh kunlun.local '/home/puzzleduck/Android/Sdk/platform-tools/adb -s emulator-5554 shell am instrument -w -r -e class com.example.androidmixtape.playback.CounterTransportServiceTest org.puzzleduck.mixtape.test/androidx.test.runner.AndroidJUnitRunner'
```

The existing single-increment/carry pixel test also passed against this build. `wheels.txt` records that separate invocation, and `carry.png` is its actual emulator 099-to-100 middle frame. This image uses injected composable state, not live playback.

This closes the service-to-engine external transport boundary check. It does not check the full live ViewModel-to-deck path, local rewind/fast-forward controls, mixtape selection, or Activity rotation during playback. Those remain Running work before Review. No production source changed in this increment. Unrelated working-tree changes remain untouched.
