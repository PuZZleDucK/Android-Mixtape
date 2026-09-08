# Segmented meters, card 4836

Implementation checkpoint, not complete acceptance evidence.

Blackout Portable and Sunset Boombox now draw ten bottom-up cells from the existing independent audio levels. Other skins retain the continuous fill. The count helper clamps inputs, clears inactive/nonfinite/missing levels and uses 0.02 falling hysteresis. Bounds, controls and labels are unchanged.

## Checks on 2026-09-08

- Focused mapping and stereo source-contract tests passed.
- Full modern unit execution passed, 252 tests, zero failures/errors/skips.
- `assembleModernDebug` produced the APK installed on Kunlun API 24, emulator-5554, using `./kunlun-sync.sh --apk app/build/outputs/apk/modern/debug/app-modern-debug.apk --all ...`. Install reported Success. The initial capture showed the launcher during boot; a second script launch showed the app.
- `lintModernDebug` failed with seven errors outside the changed meter code: WrongConstant and UnstableApi opt-in errors in MixtapeMediaLibraryService and AndroidAutoDiagnostics. These were not changed by this card.
- Captured actual existing audio playback in both selected skins, Silverface continuous comparison, Blackout paused with zero lights, and Sunset portrait. Ten separate cells, gaps and L/R labels are visible without clipping. Sunset unlit contrast is weaker than Blackout and deserves another look in dark mode.
- Skin selection for these captures used the debug app's shared preference through run-as, with force-stop/relaunch between skins. No fake meter signal was injected. Existing emulator tracks were used, not a calibrated amplitude fixture.

## Remaining before Review

Add Compose wiring/reset coverage, audit monitor reset on track replacement and absent PCM, capture calibrated low/medium/high and independent-channel rising/falling playback, stop/resume and missing-data behavior. Complete both-skin portrait/landscape and light/dark review. Current stills prove drawing and pause, not the full response contract. Resolve or clearly disposition the unrelated lint findings without taking over unrelated working-tree changes.

Disposable build/deploy logs are under `.work/card-4836/`. Durable screenshots in this directory use Git LFS.
