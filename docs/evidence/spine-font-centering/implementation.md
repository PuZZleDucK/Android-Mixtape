# Card 4833 implementation and font audit

## Change

`SpineTitleRaster.kt` now measures horizontal ink in padded scratch space before clipping it. This retains negative bearings and rotated handwriting overhang. Complete runs whose ink fits with one pixel of clearance on each side receive an integer horizontal translation into the title-only viewport. Scratch width depends on the viewport and font height, not the full title length.

Overflow keeps the original cursor-zero crop. The existing vertical fitting loop then measures the visible result, shrinks only for height and centers its alpha bounds. There is no second resize path, family-offset table, font replacement or horizontal shrink-to-fit. Font sizes, families, weights, italic style, jitter samples, proportional row geometry, badge and material drawing remain unchanged. Track-list writing still uses the original non-spine renderer. Spines previously clipped long titles rather than scrolling them; that behavior remains.

## Per-font audit

The controlled production-screen fixture uses `Agjpqy` and `long spaced title `.repeat(8), ID 45, seed 971, default Low messiness, BlankWhite paper and CrystalClear case. All eight fonts use those same values. It exercises a capital and descenders together, then intentional overflow. The production font settings previews retain their existing `Night Drive`, ID A7, ruled paper and font-derived seed. They are a separate context check, not a controlled seed comparison.

`comparison-1.png` and `comparison-2.png` show portrait and landscape list/Now Playing spines before and after. `comparison-previews.png` covers font previews in both orientations. `comparison-overflow.png` shows the unchanged long-title prefix. These are labeled crops of the raw emulator screenshots, not synthetic renders. The Ruby script records the crop rectangles and regenerates the sheets.

All eight families were left-biased for the fitting fixture. None needed another vertical family correction after card 4831. The narrower IndieFlower, GloriaHallelujah and ShadowsIntoLight runs made the horizontal bias especially obvious. Their narrow lettering remains narrow after centering.

Measured portrait list errors below are physical pixels relative to the title viewport, excluding the badge and reserved margins. Negative x means left. The image audit detects dark ink below RGB 130, so it is not a substitute for the connected tests' exact alpha bounds.

| Font | Before x error | After x error | After y error |
| --- | ---: | ---: | ---: |
| Kalam | -159 | -1 | -0.5 |
| PatrickHand | -179 | -1 | 0.5 |
| Caveat | -183.5 | -0.5 | -0.5 |
| NanumPenScript | -158.5 | -0.5 | 0.5 |
| IndieFlower | -209.5 | -0.5 | 0 |
| GloriaHallelujah | -208.5 | -0.5 | -0.5 |
| ArchitectsDaughter | -167 | -1 | 0 |
| ShadowsIntoLight | -221 | -1 | -0.5 |

Across both orientations and both measured contexts, after-image x/y errors are at most one pixel. Full measurements are in `screen-ink-bounds.csv`. Thin strokes, tall loops and deep descenders still give the families different visual character. No adjustment attempts to erase those differences. The NanumPenScript `Night Drive` preview overflows at its existing size; it correctly retains its left-anchored clip rather than shrinking or centering a truncated prefix.

`unchanged-regions.csv` contains 64 passing RGB-byte comparisons. Badge regions in list and Now Playing, the complete overflowing Now Playing spine, and track-list writing are identical before/after for every font in both orientations. This also checks the reserved badge gap and unchanged case drawing independently of the helper tests.

## Executed verification

- Modern unit suite passed, 272 tests, zero failures, errors or skips. `implementation-build.txt` records the executed unit task and APK builds. The final check reused that up-to-date unit result after whitespace-only formatting of the raster source.
- Modern x86_64 debug and instrumentation APK assembly passed. Final app APK SHA-256 is `713f826b0b772bd857f07a845f4a0c88f9515a1e74478271a9322eb56b156309`.
- `connectedModernDebugAndroidTest` passed three test methods on Kunlun API 24. `implementation-checks.txt` and `implementation-connected.xml` contain the final receipt.
- Existing `SpineTitleRasterTest` passed its 2,160-case vertical matrix again because the raster implementation changed. No duplicate vertical test matrix was added.
- Expanded `SpineTitleHorizontalPlacementTest` passed 192 fitting-title cases across all eight fonts, three proportional spine widths, two seeds and four titles, including accented capitals and descenders.
- New `SpineTitleOverhangTest` passed 1,342 pixel comparisons against an independent padded reference. It covers all eight fonts, fontScale 1 and 1.3, two seeds, triple jitter, the planned title corpus, blank input, long spaced and unbroken runs, and just-fit/just-overflow widths. One reference has ink left of its original cursor. All expected pixels match, including that overhang. The roomy-height cases prove horizontal placement does not change the chosen glyph size or weight.
- `SpineFontVisualAuditTest` passed five opt-in capture runs: before/after portrait and landscape at fontScale 1, plus after landscape at fontScale 1.3. Each run captures all eight fonts in list, short/long Now Playing and font settings. The captures render the real `MixtapeApp` with injected state, not a reconstructed row. Raw files and device configurations are under `raw/`.
- Final app deployment and launch used `kunlun-sync.sh`. `deployed-library.png` is the installed app with the emulator's seeded media. `playback-landscape.png` and `playback-portrait.png` show tape 80 retained across rotation. Media-session dumps show state 3, playing, before and after. The one-second seed files advanced tracks during the capture; this is a playback/rotation smoke check, not a claim of sample-accurate or gapless continuity.

Device was Kunlun emulator-5554, Android 7/API 24, x86_64, 420dpi. Portrait is 1080x1920 and landscape 1920x1080. Measured row rectangles range from 572x139 pixels in landscape Now Playing to 964x234 in portrait list. Font previews add the smaller grid context. Screenshots at fontScale 1.3 are supplemental; no claim is made about every accessibility scale. Those captures also show clipped deck labels and transport glyphs outside this spine-title change. No physical device or Automotive target was accessed.

Lint was not rerun. Card 4831's unrelated service/Media3 lint findings are not claimed fixed by this font change.

## Reproduction and handoff

Use the configured Java 21 and Android SDK. This shell had no `java` command, so builds used the existing Java 21 installation recorded by card 4831 through `JAVA_HOME`, plus `ANDROID_HOME=/home/puzzleduck/Android/Sdk`.

```sh
./gradlew testModernDebugUnitTest assembleModernDebug assembleModernDebugAndroidTest \
  -Pandroid.injected.build.abi=x86_64 -Pandroid.injected.testOnly=false
./kunlun-sync.sh --apk app/build/intermediates/apk/modern/debug/app-modern-debug.apk \
  --launch-emulator --install-app --start-app
# With the verified SSH ADB forward on port 15037:
ANDROID_ADB_SERVER_PORT=15037 ANDROID_SERIAL=emulator-5554 \
./gradlew connectedModernDebugAndroidTest \
  -Pandroid.injected.androidTest.deviceSerial=emulator-5554 \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.androidmixtape.ui.SpineTitleRasterTest,com.example.androidmixtape.ui.SpineTitleHorizontalPlacementTest,com.example.androidmixtape.ui.SpineTitleOverhangTest \
  -Pandroid.injected.build.abi=x86_64 -Pandroid.injected.testOnly=false
ruby docs/evidence/spine-font-centering/audit_captures.rb
```

For the opt-in captures, install the fresh instrumentation APK under `app/build/intermediates/apk/androidTest/modern/debug/` through the forwarded ADB server, then run `am instrument -w -r -e class com.example.androidmixtape.ui.SpineFontVisualAuditTest -e spineAuditPhase after org.puzzleduck.mixtape.test/androidx.test.runner.AndroidJUnitRunner`. Pull the target app's external `files/spine-audit/` directory before Gradle connected-test cleanup uninstalls the app. No source was copied to Kunlun.

Early audit attempts caught a stale test APK path, framebuffer presentation lag and virtualized preview items. Those attempts are not acceptance evidence. The checked-in test uses the fresh intermediate APK, waits for presentation and scrolls the settings grid by index. The five retained capture runs passed. Portrait ArchitectsDaughter's full preview is taken from the preceding GloriaHallelujah capture because the last grid items cannot all scroll to the top.

Only this card's raster, tests, audit files and LFS rule are committed. Builds and screenshots used the existing dirty working tree, not an isolated release checkout. Existing unrelated working-tree edits remain untouched. The app was reinstalled and relaunched after the final connected run. The run-owned emulator, verified SSH forward and idle Gradle/Kotlin daemons were stopped and their exits checked. No push.
