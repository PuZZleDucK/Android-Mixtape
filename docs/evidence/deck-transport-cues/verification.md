# Deck transport sound effects

`MainActivity.kt` now connects the player's previous/next callbacks to `previousWithCue()` and `nextWithCue()` in `MixtapeViewModel.kt`. These validate queue boundaries and reuse `jumpToTrackWithCue()`, including its five-second directional effect, delayed target playback and stop/eject cancellation. Immediate navigation and automatic song transitions are unchanged.

Verification:
- 283 unit tests passed, including seven new transport regression tests.
- Modern debug app and instrumentation APKs built successfully.
- `DeckTransportCueUiTest` passed through `connectedModernDebugAndroidTest` on Kunlun API 24. It clicks both production deck buttons, checks actual Android music-stream activity from `AudioTrackTransportCuePlayer`, and verifies each song starts only after its cue. Song playback uses a silent test engine to distinguish cue output from music. This is a platform audio-output check, not a listening-quality assessment.
- Deployed using `kunlun-sync.sh --apk app/build/intermediates/apk/modern/debug/app-modern-debug.apk --install-app --start-app`.
- The temporary emulator and verified SSH ADB forward were stopped after testing. No phone was accessed; no commit or push was made.
