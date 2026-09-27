# Spine lettering and compact tracks — Nothing review

Date: 2026-09-27 (Australia/Melbourne). Development update, native Compose/Canvas; no WebView.

## Requested and delivered

- Each tape persists Left/Center/Right name justification, one of six side/corner symbol positions, and independently chosen name/symbol inks. Stable seed-derived initial variations do not reroll during scrolling or rotation. The current-tape customizer includes live previews and explicit controls.
- Existing default Navy/Navy pairs receive varied inks once. Non-default customised pairs, names, fonts, messiness, material themes, membership and audio files are preserved. The Nothing's 13 tapes have all three alignments, five initial symbol positions, eight name inks and eight symbol inks. Every initial pair differs. The sixth position remains selectable and was exercised through the customizer.
- Large names fill most of the printable spine height using measured glyph ink, with proportional Medium/Small settings. Cassette label lettering is larger too.
- **All tape names remain one line.** The final renderer sizes by height only: it does not wrap, ellipsize, or shrink names to fit the width. Left/center/right origins apply even when the line overruns. Intentional horizontal clipping preserves the hand-lettered, misjudged-space character requested by the user; names cannot overlap the separate symbol lane. Full names remain in accessibility semantics and the customizer.
- Track rows use larger 24 sp base handwriting (previously 21 sp), actual ink metrics rather than padded font line boxes, 1 dp vertical padding and a 26 dp outer minimum. Tracks remain title-only; existing cue/playback and long-press actions are retained.
- Handwriting settings previews now use the same native spine and track components as the listening screen rather than a legacy preview.

The previous two-column landscape library, single-column portrait library, bounded current-tape scrolling, headerless listening screen, fixed player aspect ratios, case optics and integrated controls remain unchanged.

## Build and automated verification

Canonical source: `/home/puzzleduck/x/android-mixtape` on Artigas. No source tree was copied to Kunlun. No commit/push or store publishing was performed.

```sh
JAVA_HOME=/home/puzzleduck/.asdf/installs/java/temurin-21.0.10+7.0.LTS
ANDROID_HOME=/home/puzzleduck/Android/Sdk
PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon --max-workers=2 assembleModernDebug testModernDebugUnitTest lintModernDebug --console=plain
```

- Final build: **successful**. Log: [verification-single-line.log](verification-single-line.log).
- **356 unit tests passed**, zero failures/errors/skipped. New tests cover stable variations, migration preservation, persistence, customization, overflow alignment and the single-line renderer contract.
- Lint: **zero errors**, 44 warnings and 2 hints. See [lint-results.txt](lint-results.txt); no claim of warning-free lint.
- [build-results.json](build-results.json) and [verified-source-sha256.txt](verified-source-sha256.txt) record the final verified artifact/source.
- APK: `dist/android-mixtape-native-demo-debug.apk`.
- SHA-256: `513d13b8a1953379e964dfe2d7fbac9315e6b1a84dd2e094ed8e0526f56c31c6`.
- Kunlun copy: `/home/puzzleduck/x/remote-mixtape-testing/android-mixtape-spine-lettering.apk`.

The older `verification.log` is a superseded draft run. It failed lint while the source was edited during analysis; the stable final source was subsequently rebuilt and linted successfully. That draft used wrapping names and larger track rows and is **not** the delivered artifact.

Pre-edit source backup remains at `/home/puzzleduck/.local/state/mixtape-review/backups/20260927-171945-before-spine-lettering/source.tar.gz` on Artigas. Release v0.1 artifact and publishing storage/service were not changed.

## Physical-device verification

Only the requested Nothing A001T / Galaxian, Android 16/API 36, serial `00252359V002047`, was used. Every ADB command was pinned to it. Display: 1080 × 2392 at 420 dpi. No Nokia access and no phone instrumentation or app-data reset.

- Installed with `adb install -r`; installed `base.apk` hash matches the final build. Only `org.puzzleduck.mixtape` is installed, not the retired duplicate ID.
- Cold launch opens the idle full-page tape library. Inspected single-line overflow across all 13 names in both orientations, with visible independent inks and varied symbol positions. The landscape library still uses two columns with normal-size spines.
- Selected a tape, checked player/tracklist in both orientations, and paused it. No crash-buffer entries for the installed app's process.
- Customizer: changed the current tape from Right/Upper right to Left/Right, saved and reopened to confirm persistence, then restored its original values. Those controls/model callbacks were unchanged by the final single-line raster-only refinement. Fresh final preferences exactly match the post-migration snapshot.
- Checked all eight font families in the native gallery; compared Large/Medium/Small on the same three top preview tapes. Large was restored. The user's High messiness, chosen deck and tape themes remain unchanged.
- At handoff: idle library visible, audio not playing; original automatic rotation restored via `cmd window user-rotation free`, with `accelerometer_rotation=1` and `user_rotation=0`. No volume changes.

### Measurements

[layout-checks.json](layout-checks.json) is generated by [check_lettering.py](check_lettering.py) from the UI hierarchy/screen captures. Raw SharedPreferences snapshots stay on Kunlun; only aggregate preservation/variation checks are included here.

| Check | Before | Final |
| --- | ---: | ---: |
| Current tape track-row pitch, same Large + High settings | 95 px | 86 px |
| Portrait/landscape final row pitch | — | 86 / 86 px |
| Base track font size | 21 sp | 24 sp |
| Native spine preview height | — | 135 px |
| Large visible name ink, Kalam / Patrick Hand / Caveat | — | 98 / 98 / 98 px |
| Medium visible name ink | — | 83 / 84 / 83 px |
| Small visible name ink | — | 68 / 69 / 68 px |

Rows are 9.47% tighter despite 14.3% larger nominal type. Large ink occupies about 73% of the entire plastic spine height, and most of its smaller printable label region. No vertical clipping was observed. Horizontal overrun is intentional, not a fit defect.

### Final screen captures

- [Portrait library](final-01-large-portrait-library.png) / [Landscape library](final-02-large-landscape-library.png).
- [Landscape tracks](final-03-large-landscape-tracks.png) / [Portrait tracks](final-04-large-portrait-tracks.png).
- [Large](final-05-font-gallery-large.png), [Medium](final-06-font-gallery-medium.png), [Small](final-07-font-gallery-small.png), [Large restored](final-08-font-gallery-restored.png).
- Remaining font families: [gallery middle](final-09-font-gallery-middle.png), [gallery bottom](final-10-font-gallery-bottom.png).
- Full library after eject: [top](final-11-landscape-library.png), [bottom](final-12-landscape-library-bottom.png).
- [Idle portrait library](final-13-restored-idle-library.png).

The latest follow-up was physically reviewed rather than sent through another scoring-agent cycle; the initial demo's capped review cycle is already complete.
