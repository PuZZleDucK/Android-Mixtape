# Paper edges, catalogue boxes, dice and reel motion

2026-09-27 — native Compose development update, reviewed on the requested Nothing A001T (Galaxian), Android 16/API 36, serial `00252359V002047`. No Nokia access, phone instrumentation, data clear or audio-file modifications.

## Delivered

- Single-line names retain their Left/Center/Right alignment but no longer clip at the internal text margins or symbol lane. The raster spans the physical paper; the sheet clip keeps all ink inside the plastic. Large handwriting can intentionally cross printed marks or doodles, like an overfilled Texta label.
- Six of the 38 bound paper styles opt into `catalogueBox: left/right`. These have a small, factory-printed one/two-character boxed code, using the saved `decorativeId`; the other 32 papers remain unbadged. The editor exposes the code for manual changes.
- Long-pressing any spine edits THAT stable tape key, whether loaded or not, without selecting the tape or replacing the playback queue. The editor has a top dice button and an App settings gear, so the idle library still needs no header.
- Dice generates a new name (explicitly confirmed by the user), handwriting font and seed, symbol, independent name/symbol inks, alignment, symbol placement, printed code, case, linked sleeve/spine paper and track ink. Enabled theme/font/symbol choices are respected. Shell, screws, sticker, deck, songs and grouping are not rerolled. New names avoid other tapes where choices permit.
- Dice changes only the editor draft; rotation preserves that draft. Save applies it, Cancel/back does not. The cassette label shares the tape's name/font/mark as before.
- Playback and fast-forward turn anticlockwise when viewed from the label face; rewind turns clockwise. Cue winding is nominally six times normal speed, adjusted for pack radius. An asymmetric moulded hub index helps reveal direction. Explicit cue state—not message parsing—drives reels while the five-second track cue has music paused. Stop/eject, Play, a new cue or selecting a different tape cancels stale cue jobs and clears motion state.
- Following the additional readability request, track handwriting increased from **24 to 32 sp** base (+33.3%). It remains smaller than the spine name and keeps the existing measured-ink rows, 1 dp padding and single-line title-only presentation. Font-preview cards now derive their height from those same row metrics, fitting both samples instead of cropping the second enlarged line.

Existing two-column landscape library, bounded shelf scrolling, linked paper/case optics and player proportions remain intact. This update changes the canonical Android project only, not the separate HTML studio checkout.

## Build and install

Canonical source stayed on Artigas at `/home/puzzleduck/x/android-mixtape`. Backup: `/home/puzzleduck/.local/state/mixtape-review/backups/20260927-182737-before-paper-edge-dice/source.tar.gz`.

Final command (Java 21 / Android SDK):

```sh
./gradlew --no-daemon --max-workers=2 assembleModernDebug testModernDebugUnitTest lintModernDebug --console=plain
```

- **368 tests passed**, zero failures/errors/skipped.
- Lint completed with **zero errors**, 44 warnings and 3 hints; not warning-free.
- [Final build log](verification.log), [build results](build-results.json), [lint details](lint-results.txt), [verified source hashes](verified-source-sha256.txt).
- APK: `dist/android-mixtape-native-demo-debug.apk`.
- SHA-256: `6246234b09556d2064c9fb9170eef6ccd01c9ecec12d57224edf342be4955616`.
- Kunlun APK: `/home/puzzleduck/x/remote-mixtape-testing/android-mixtape-paper-edge-dice.apk`.
- Updated with `adb install -r`; installed base.apk hash equals the final artifact. Only the correct `org.puzzleduck.mixtape` package is present.

A first compile caught a missing IconButton import, then a source-contract test needed to follow the new shared cue-cancellation helper. Functional cue tests were retained and expanded. The initial installed candidate exposed clipped font-gallery samples; the final build corrects their measured height. `verification-before-gallery-height.log` is the successful superseded candidate, not the final receipt. No push, commit, release-v0.1 replacement or publishing-storage/service change was performed.

## Physical and deterministic checks

[Layout checks](layout-checks.json) and [device checks](device-checks.json):

- An oversized name's measured ink reaches x=67, matching the paper boundary x=67.136; the plastic starts at x=43. There is no early text-lane cutoff and no ink on the surrounding plastic.
- Real track-row pitch changes from 86 to 112 px at the same Large/High settings: increased readable type, not added padding. Portrait/landscape player geometry remains unchanged.
- Long-pressed Halfway Into His Coat, rolled a different name/appearance, rotated to landscape and verified the draft survived; Cancel left all saved names and visual properties byte-equivalent as parsed preferences.
- Long-pressed an unloaded tape from the player shelf and saved without changing the loaded Static in Silk tape. On Fuzz Budget, changed printed code 21 to 22, saved, reopened and verified 22, then restored 21. Final saved name/visual maps exactly match the pre-test maps. The JVM tests also verify that a complete dice draft saves/reloads, preserves unrelated tapes/playback and respects singleton enabled options.
- The editor's settings gear opens native Settings. Large remains selected, High messiness retained, and all eight fonts stay enabled. Final gallery captures show the fitted sample rows.
- Final build was recorded on-device in play, forward cue and rewind cue. A polar-image correlation of the large left reel measured -82.39 deg/s playing, -538.88 forward and +539.97 reverse (clockwise positive). The sign reversal and fast winding are verified from pixels, not just state labels. The measurement approximates speed; the code specifies exactly 6x cue speed.
- No crash-buffer entries for the final app process. Original automatic rotation restored (`free`, auto=1, user_rotation=0), no volume changes. Final handoff shows the idle full-page library and Mixtape's media session is PAUSED at position 0.

**Playback-resume observation resolved:** the user confirmed they were also pressing Play during QA because they liked the song. No app defect was established, and no speculative playback fix was made. The interrupted paused-rotation captures are not claimed as a continuity test. Stop/eject and the media session's paused state were verified at handoff before returning control to the user.

Raw preferences remain private in `/home/puzzleduck/x/remote-mixtape-testing/paper-edge-dice-evidence/`, not in this project. `check_layout.py --private-prefs PATH` rechecks preservation when those snapshots are available; without them it checks public screen evidence only.

## Useful evidence

- Final [portrait library](final-24-handoff.png) / [landscape library](final-23-landscape-library.png).
- Final [larger track text and player](final-22-player-final-build.png).
- [Dice draft](final-03-editor-dice.png), [same draft after rotation](final-04-editor-dice-landscape.png), [unloaded tape editor](final-10-shelf-editor.png), [saved printed code](final-13-code-saved.png).
- Fitted gallery: [top](final-17-font-gallery-fitted.png), [next](final-18-font-gallery-fitted.png), [middle](final-19-font-gallery-fitted.png), [lower](final-20-font-gallery-fitted.png).
- Reel recordings: [play](reels-play.mp4), [fast-forward](reels-forward.mp4), [rewind](reels-rewind.mp4); [pixel measurements](reel-motion-checks.json) and [reproducible measurement script](check_reel_motion.py).

Captures 01–13 are the initial candidate with identical relevant name/editor/motion behavior. Captures 17–24 and all three reel videos are the final installed build. Superseded fixed-height gallery captures 14–16 were not copied into this evidence folder.
