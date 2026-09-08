# Card 4831 implementation evidence

## Change

CassetteSpineRow now scales content padding, badge geometry and preferred title size with spine height. Title sizing uses physical label space rather than an independent sp size. The selected font, italic style, weight, seed and paper/plastic themes are unchanged. Track-list code is unchanged.

The opt-in spine path rasterizes the same Compose token layouts and jitter transforms onto a transparent bitmap. It measures nonzero alpha rows within the horizontal title viewport, shrinks uniformly if needed, re-renders when shrinking exposes more text, and centers the resulting visible ink. It reserves one physical pixel on each vertical edge. Empty/whitespace titles draw nothing. Long titles remain a single horizontally clipped run; no wrapping, ellipsis or whole-title width fitting was added. Other handwriting callers retain the original renderer.

## Executed checks

- Modern unit suite: 247 tests, zero failures/errors/skips, including all three planning source guards.
- x86_64 modern debug assembly passed. Final APK SHA-256: `2fe72cc33b6ad85365795cb5f3cd00e80c901bd9d9f6b02ebae797af0a16e7be`.
- Kunlun API 24 connectedModernDebugAndroidTest, filtered to SpineTitleRasterTest: one test passed, exercising 2,160 raster cases. All eight bundled fonts, widths 180/240/280/360/480dp, densities 1/2.75, zero/normal/triple jitter strength, seed 971, reference title, ascenders/descenders, accents, emoji fallback, empty/whitespace, single glyph, long unbroken and long spaced titles. Assertions check actual alpha bounds, vertical containment and placement within half a pixel of the title viewport center.
- lintModernDebug ran and failed with seven errors outside this change: WrongConstant in MixtapeMediaLibraryService and six Media3 opt-in errors in that service and AndroidAutoDiagnostics. No suppression or unrelated source fix was added. Full report is lint.txt.

The raster matrix uses fontScale 1 and one seed; it is not the entire larger planning matrix. It checks the raster helper, not independent pixel extraction from the final decorated Compose row. The attached production screenshots provide that separate visual check.

## Before and after captures

Files before-portrait.png, before-landscape.png, after-portrait.png and after-landscape.png are raw Kunlun emulator captures through kunlun-sync.sh. Device was emulator-5554, API 24, x86_64, 1080x1920, 420dpi, default font scale. Landscape is 1920x1080.

Fixture uses title `restless neon weekend`, ID 45, Kalam italic/ExtraBold, jitter seed 971, HotPinkClear case and BlueprintGrid paper. Kalam is an explicit test choice, not a claim about the Nothing phone's selected font. Fixture preferences were set on the test emulator only. Existing seeded audio supplies the track list.

Both baseline and updated APKs were assembled from the current working tree. For the baseline rebuild, only the CassetteSpineRow changes were temporarily reversed; the opt-in renderer remained unused. They were then restored, rebuilt, retested and deployed. Early exploratory captures used a stale outputs/apk file and were discarded. The actual fresh build is under app/build/intermediates/apk/modern/debug; deployment used explicit --apk and disabled the injected testOnly flag.

Landscape baseline clips the bottom of the title. Updated landscape has top/bottom clearance and centered visible lettering. Portrait uses the same proportional rules and centers the visible ink. Spine and track-list outer rectangles remain unchanged. Playback progressed across rotation with the same title, ID, font and skin. Track highlighting, reel positions and list scroll offsets differ because playback was active during capture.

Library spines were also visually inspected with the updated renderer. No phone, Automotive emulator or projected Android Auto session was accessed. The run-owned API 24 emulator, ADB tunnel and idle Gradle/Kotlin daemons were stopped and their process exits verified after capture.

## Commands

Build environment used JAVA_HOME=/home/puzzleduck/.asdf/installs/java/temurin-21.0.10+7.0.LTS and ANDROID_HOME=/home/puzzleduck/Android/Sdk.

```sh
./gradlew testModernDebugUnitTest assembleModernDebug -Pandroid.injected.build.abi=x86_64 -Pandroid.injected.testOnly=false
./gradlew lintModernDebug -Pandroid.injected.build.abi=x86_64
./kunlun-sync.sh --apk app/build/intermediates/apk/modern/debug/app-modern-debug.apk --install-app --start-app
# Verified SSH local forward 15037 -> Kunlun adb server 5037:
ANDROID_ADB_SERVER_PORT=15037 ANDROID_SERIAL=emulator-5554 ./gradlew connectedModernDebugAndroidTest -Pandroid.injected.androidTest.deviceSerial=emulator-5554 -Pandroid.testInstrumentationRunnerArguments.class=com.example.androidmixtape.ui.SpineTitleRasterTest -Pandroid.injected.build.abi=x86_64 -Pandroid.injected.testOnly=false
```
