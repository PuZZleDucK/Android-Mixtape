# Activity and local transport regression

The expanded `CounterTransportServiceTest` passed on Kunlun API 24, emulator-5554. Both modern APKs built and deployed through `kunlun-sync.sh`. See `build.txt`, `deploy.txt`, `test.txt` and the final run's `assertions.txt`.

The test launches the production MainActivity, grants audio permission and waits for its initial library scan before supplying two 30-second silent WAV queue entries through a second MediaController. The entries include duration metadata so the counter measures the complete queue. No UI state is injected. The test enters the deck through the production `backFromTrackInfo` ViewModel action, rather than library selection.

It retains the external transport assertions and adds:

- The Activity ViewModel receives the live service queue.
- Landscape orientation and explicit Activity recreation during playback preserve playback and advancing position.
- An external pause and reset reach the recreated ViewModel and render `Tape counter 000`, still present after 500ms.
- Clicking the real Fast-forward button reaches track two. Pausing and seeking to that track's start displays `500`.
- Clicking Rewind reaches track one. Clicking Stop settles on `000`.

`recreated-reset.png` is the emulator framebuffer of the actual recreated deck after reset. This is settled live-service evidence, not a mid-roll frame. Earlier ordered carry frames remain under `../frames/`.

Reproduce after deploying app and test APKs:

```sh
ssh kunlun.local '/home/puzzleduck/Android/Sdk/platform-tools/adb -s emulator-5554 shell am instrument -w -r -e class com.example.androidmixtape.playback.CounterTransportServiceTest org.puzzleduck.mixtape.test/androidx.test.runner.AndroidJUnitRunner'
```

Initial test attempts exposed fixture mistakes: checking library `tracks` instead of `queueTracks`, and recreating without granting audio permission. The committed run corrects both. No production source changed.

Still needed before Review: live library/mixtape selection and eject/reselection smoke, including counter settling. Rotation here occurs during playback, not at a deterministic wheel-animation frame; deterministic composition disposal during carry is covered separately under `../remount/`.
