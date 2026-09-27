# Landscape library and bounded current-tape scrolling

Date: 2026-09-27. Native Compose development follow-up; canonical source stays on Artigas.

## Changes

- Restored a two-column `LazyVerticalGrid` for the full-page landscape library, using the same themed `DemoSpine` renderer and 6.5:1 spine ratio. Portrait remains one column. The narrow tape shelf beside the landscape player deliberately stays one column: its spines match the library cell scale rather than becoming half-sized.
- Reused the root's hoisted/saved list and grid states. Each orientation retains its scroll position. Selection still opens that tape's track list; no player or headers appear in the idle/ejected library.
- Removed symmetric half-viewport padding from the player shelf. `DemoShelfLayout.kt` computes a selected-item-centred target using actual rounded row pixels, then clamps it to the real content length. First/last and nearby tapes remain fully visible at an edge; middle tapes still centre. Lists shorter than the viewport stay top-aligned.
- The entry effect depends on current tape, viewport, density and item count, not playback ticks; ordinary manual scrolling is not overridden.

## Build and tests

`assembleModernDebug testModernDebugUnitTest lintModernDebug` completed successfully in 7m 9s. See `verification.log` and `test-results.json`.

- 345 unit tests passed; 0 failed, errored or skipped.
- Seven new numerical layout tests cover first/near-first, last/near-last, middle, short and empty lists, multiple viewport/row sizes, resizing, bounded scroll and full selected-spine visibility. The integration contracts cover the new grid route, saved state, native renderer and fixed-padding shelf.
- Lint: 0 errors, 44 warnings, 2 hints.
- `check_layout.py` passes 41 assertions against the captured physical-device UI hierarchy; detailed results are in `layout-checks.json`.
- No Gradle clients/builds or idle Gradle/Kotlin daemons remained after verification. `git diff --check` passed.

## Nothing phone verification

Pinned target: Nothing A001T, serial `00252359V002047`, Android 16/API 36, 1080 x 2392 at density 420. Updated `org.puzzleduck.mixtape` in place with `adb install -r`. No app-data clearing, instrumentation, library changes, theme changes, audio-file modifications or volume adjustments were performed. Only the correct Mixtape package was installed. The app process's crash buffer was empty after the checks.

- `before-full-landscape-library.png`: old full-width spines measured 2180 x 335 px. `after-01-landscape-library.png` and `after-12-landscape-library.png`: two columns, each spine 1080 x 166 px; eight fully visible tapes rather than two giant rows.
- `after-13-landscape-library-bottom.png`: scrolling reaches all 13 tapes, including the odd final grid item. `after-15-landscape-library-retained.png` verifies the same grid scroll position after a portrait/landscape round trip.
- `after-11-portrait-library.png` and `after-14-portrait-library-retained.png`: unchanged one-column portrait layout and retained scroll position, without a player.
- `before-first-tape-shelf.png`: first tape started 346 px below the list top. `after-02-first-landscape-playing.png`: the first tape starts only 22 px below the list top during real playback. Other tapes fill the space below it. Playback was paused promptly after checking.
- `after-03-first-portrait.png`: the first tape also starts at the portrait shelf's real top (22 px inset).
- `after-07-last-portrait.png` and `after-08-last-landscape.png`: the last tape ends 22 px above the list bottom, with preceding tapes filling the viewport instead of a blank lower half.
- `after-09-middle-landscape.png` and `after-10-middle-portrait.png`: middle-tape centre exactly matches the list viewport centre in both orientations, with no lost edge space.
- `after-04-manual-shelf-scroll.png` and `after-05-manual-scroll-retained.png`: manual scrolling remains in place. `after-06-last-tracklist.png`: selecting a tape returns to its tracks.
- Auto-rotation restored to enabled; user rotation restored to 0. Handoff is the full-page library with playback paused, and its orientation follows the physical phone (`after-16-handoff.png`).

Short-library and near-edge combinations beyond the actual 13-tape phone library were exercised numerically, not by modifying the user's library. No emulator or destructive connected test suite was run for this targeted physical-phone review.

## Artifact

Development APK: `dist/android-mixtape-native-demo-debug.apk`.

SHA-256, matched across the build output, local review APK and the phone's installed `base.apk`:

`79e3aac40eca63593f756a8959d4d6ddca93617b3dfe7d4611e5a34a50d7b81a`

The local review copy is `/home/puzzleduck/x/remote-mixtape-testing/android-mixtape-tape-shelf-layout.apk`. Changed source/test hashes are in `verified-source-sha256.txt`. Before-edit source backup stays on Artigas at `/home/puzzleduck/.local/state/mixtape-review/backups/20260927-170110-before-tape-shelf-layout/source.tar.gz`. No release/store publishing or Git push was performed.
