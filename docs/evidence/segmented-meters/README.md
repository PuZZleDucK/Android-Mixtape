# Segmented meters, card 4836

Blackout Portable and Sunset Boombox draw ten bottom-up cells from the existing independent audio levels. Other skins retain continuous fills. The mapping clamps inputs, clears inactive/nonfinite/missing levels and uses 0.02 falling hysteresis. Bounds, controls and labels are unchanged.

The monitor now clears stale data after 500 ms without PCM publication. Each publication renews that deadline, including identical levels that StateFlow does not re-emit. The timeout only clears levels; it never generates a signal. Processor flush/reset already clears both channels. Paused/stopped UI ignores the last level immediately.

## Verification on 2026-09-08

- All 252 modern unit tests passed with no failures, errors or skips. Focused tests cover opt-in skins, every rising threshold, falling hysteresis, clamping, nonfinite/missing/inactive inputs and independent stereo state.
- Built modern debug and its instrumentation APK. Installed both through `kunlun-sync.sh` on Kunlun API 24, `emulator-5554`, x86_64. No physical device was accessed.
- `SegmentedMeterUiTest` passed both tests in portrait and again in landscape. Logs are `compose-portrait.txt` and `compose-landscape.txt`.
- The PCM/Compose test covers both skins under light and dark Material color schemes. It verifies 2/5, 9/2 and 2/9 cells, falling response, pause, resumed playback, silence, flush and missing PCM. Sustained identical PCM for more than 500 ms remains active; 700 ms without PCM clears both channels.
- The playback test generates a stereo 48 kHz WAV and plays it through a real ExoPlayer with the production MeteringRenderersFactory and AudioLevelMonitor. Its three-second phases request scaled levels 0.25/0.55, 0.95/0.25, 0.25/0.95 and silence. These produce 2/5, 9/2, 2/9 and 0/0 cells. It pauses/resumes, seeks, replaces loud playback with the silent tail and stops during loud playback. The test uses Player.isPlaying callbacks, not a fabricated playback timer. This is an instrumented player fixture, not the application's library/navigation flow; the earlier existing-track captures cover that flow.
- Reviewed both playback sequence sheets and `appearance-matrix.png`. Cells have visible gaps and distinct unlit states; L/R labels and deck controls fit in both orientations. Sunset has subtler unlit contrast than Blackout but the cells remain distinguishable. Deck colors are fixed skin palettes, so light/dark Material schemes intentionally do not recolor them. The app currently supplies a light Material scheme itself.
- Full screenshots are in `frames/`, with `1-` for portrait and `2-` for landscape. Playback sheets crop only the deck from those originals. Phase labels 0, 1, 2 and 3 mean low/mid, high/low, low/high and silence. Pause, replacement and stop are separate captures. Screenshots wait 150 ms after Compose settles so the presented framebuffer matches the semantics assertions.

## Known limits outside this card

`lintModernDebug` still reports the same seven errors in MixtapeMediaLibraryService and AndroidAutoDiagnostics, plus 31 warnings. The errors concern SessionResult constants and Media3 opt-in annotations, not meter code. `lint.txt` preserves the report. This card does not change those files or suppress their findings.

The repository had unrelated working-tree changes before this work, including the existing AudioMeterScale extraction and scaling adjustment. They were present in the tested APK and remain uncommitted by this card. Only this card's stale-data reset hunk was staged in AudioLevelMonitor.

Earlier stills remain as actual existing-track playback and Silverface continuous-meter comparisons. All PNG evidence uses Git LFS. Disposable logs and intermediate captures were kept in `.work/card-4836/` and obsolete copies removed before handoff.
