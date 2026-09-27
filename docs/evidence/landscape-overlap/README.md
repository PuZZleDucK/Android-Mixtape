# Landscape footer overlap fix

The landscape player chose its left pane width from screen width alone. The deck then consumed that width divided by its 560:400 aspect ratio, without reserving height for the footer. On short screens the remaining height could not fit the spine or the 64dp toggle. Compose's aspect-ratio measurement overflow and centering let the footer overlap the deck and extend below the panel.

`MixtapeApp.kt` now reserves the taller of the spine and toggle, plus the 4dp gap, before capping the deck width by available height. Taller layouts keep their previous width. Track-list typography and horizontal scrolling are unchanged.

## Verification

- Kunlun API 24 emulator only. No user device accessed.
- `NowPlayingLayoutTest` checks both footer modes, containment, deck separation, and the toggle's full height. Resize coverage includes 360x640 portrait, 900x480 and 900x360 landscape, and 700x280 compact landscape, in dp.
- Before the fix, the unchanged track-preview regression failed: its bottom was 358dp in a 360dp viewport with 8dp bottom padding. See `baseline-test.txt`.
- After deployment through `kunlun-sync.sh`, all 9 modern instrumentation tests passed, including the 3 new layout tests. See `ui-tests.txt`.
- Modern unit tests: 229 passed. Legacy unit tests: 224 passed.
- Both debug APKs assembled. Modern APK installed and launched on the emulator; distributable APKs and checksums refreshed under `dist/`.
- Legacy lint passed in the initial verification run. Modern lint reports 7 errors in unchanged Android Auto diagnostics/media-service code: 6 unstable API opt-ins and 1 session-result constant. No errors in the changed layout or test. See `build-and-lint.txt`.

`before-preview.png` shows the old track-preview layout. `after-spine.png` and `after-preview.png` show the fixed layout with synthetic test tracks. `fixed-player.png` crops the fixed spine screenshot to the app viewport, omitting unused emulator/test-window space.

The emulator was started for this task and stopped after verification. Local Gradle invocations used single-use daemons.
