# Card 2212 — Launcher icon larger cassette + transparent background plan

## Goal

Update the Android Mixtape launcher icon so the cassette reads larger in the icon tile and the exported launcher artwork has transparent background rather than the current dark square fill.

## Review rework update — generate a fresh transparent logo graphic

Latest user direction: **"just generate a new transparent logo graphic"**. Treat the next pass as a source-art generation task first, not another crop/mask pass over the old opaque square.

Actionable direction for Testing/Running:

- Create or regenerate one high-resolution transparent PNG logo source, preferably `app/icon-source/ic_launcher_cassette_1024.png`, with an optional preserved copy such as `app/icon-source/android_mixtape_logo_transparent_1024.png` if Running wants to compare alternatives before replacing the launcher source.
- The graphic should be a clean front-facing retro cassette for Android Mixtape, on a fully transparent canvas, not a dark rounded-square tile with transparency cut around it.
- Use the existing cassette only as visual reference/brand direction if useful; the final source should read as newly generated logo art.
- Keep the cassette dominant: target roughly 82–90% of canvas width with transparent padding on all sides so launcher masks do not clip it.
- Produce checkerboard preview evidence for Review showing that empty space is transparent.

Useful prompt seed for image generation:

> transparent background PNG app icon logo, single retro audio cassette tape, front-facing, centered, large on canvas, warm cream plastic shell with orange/red label accents, dark tape reels, crisp vector-like edges, subtle vintage texture, no square background, no drop shadow, no text except tiny abstract label lines, object fills about 85 percent of image width, transparent empty space around object

Negative prompt terms: opaque background, rounded square tile, black square, colored backdrop, phone mockup, perspective skew, heavy shadow, excessive text, clipped edges, watermark.

Current quick inspection:

- `app/icon-source/ic_launcher_cassette_1024.png` is 1024x1024 but appears stored as opaque RGB (`identify` reports `srgb` and full-canvas bbox).
- Legacy launcher bitmaps `app/src/main/res/mipmap-*/ic_launcher.png` are opaque RGB, so they carry the dark square background.
- Adaptive foreground `ic_launcher_foreground.png` has alpha but the visible cassette currently occupies only the central safe-zone-sized area (`xxxhdpi` bbox about 288x288 inside a 432x432 canvas).
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` uses `@color/ic_launcher_background`, currently `#1B1B24`.

## Desired visual direction

- Keep the approved retro cassette identity from the existing icon: front-facing cassette, warm tape/plastic details, readable reels/label area.
- Scale/crop the cassette upward so it feels intentionally dominant at launcher sizes. Target the cassette body to occupy roughly 82–90% of the square canvas width for bitmap fallbacks, with enough outer alpha padding to avoid clipping rounded/circle launcher masks.
- Make all visible empty area transparent. There should be no opaque square, dark tile, or matte behind the cassette in the PNG fallbacks.
- For adaptive icons, prefer a transparent background layer plus a larger foreground cassette. Keep critical features (reels, label, cassette outline) within the adaptive safe zone; if anything must extend outside, only low-risk decorative shell edges should be outside.

## Implementation steps for Running

1. Preserve the current source asset before regeneration, e.g. copy it to `app/icon-source/archive/` or keep it recoverable via git.
2. Create/update a reproducible icon-generation step. If no generator exists, add a small script such as `scripts/generate_launcher_icons.rb` or `scripts/generate_launcher_icons.sh` using ImageMagick:
   - start from a transparent 1024x1024 canvas,
   - isolate/place the cassette artwork enlarged and centered,
   - export the master to `app/icon-source/ic_launcher_cassette_1024.png`,
   - export legacy `ic_launcher.png` density bitmaps at 48/72/96/144/192 px,
   - export adaptive `ic_launcher_foreground.png` density bitmaps at existing foreground sizes 108/162/216/324/432 px.
3. Change the adaptive background from the opaque color to transparent, for example `#00000000`, or a transparent drawable if that proves more reliable with Android resources.
4. Ensure generated PNGs retain alpha (`srgba`) and transparent corners.
5. Update/add TDD contract coverage in `LauncherIconResourceContractTest`:
   - master and legacy bitmap icons have alpha transparency and transparent corners,
   - non-transparent cassette bbox is larger than the current small rendering, e.g. at least ~80% of legacy bitmap width but less than full-canvas to prove no clipped edges,
   - adaptive foreground still exists at expected density sizes and has transparent padding/corners,
   - adaptive XML/background does not point at an opaque dark color.
6. Regenerate icon assets and run formatting/test/build checks.

## Test plan

Run from `/home/puzzleduck/x/android-mixtape` with the usual Android environment:

```sh
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/35.0.0:$PATH"
./gradlew testModernDebugUnitTest --tests com.example.androidmixtape.icons.LauncherIconResourceContractTest
./gradlew testLegacyDebugUnitTest --tests com.example.androidmixtape.icons.LauncherIconResourceContractTest
./gradlew assembleModernDebug assembleLegacyDebug
```

Additional static checks:

```sh
identify -format '%f %wx%h %[channels] bbox=%@\n' \
  app/icon-source/ic_launcher_cassette_1024.png \
  app/src/main/res/mipmap-*/ic_launcher.png \
  app/src/main/res/mipmap-*/ic_launcher_foreground.png
```

Expected: alpha-capable channels on exported icons, transparent corners, and cassette bbox noticeably larger than the old adaptive 288/432 foreground footprint.

## Review evidence to capture

- `identify`/contract-test output proving transparent alpha and larger bbox.
- `git diff -- app/icon-source app/src/main/res/mipmap-* app/src/main/res/values/colors.xml app/src/test/...` or equivalent summary.
- If a device/emulator is available, install one modern and one legacy/debug APK and capture launcher/app-drawer screenshots showing the cassette appears larger and no square background is visible. Do not block solely on unavailable named hardware; static APK/resource evidence is acceptable if runtime targets are unavailable.

## Risks / cautions

- Adaptive icon masks can crop artwork that exceeds the safe zone. Make the cassette larger, but keep important features inside the central safe area and use device/emulator screenshots when possible.
- Transparent adaptive backgrounds may be displayed over launchers with different wallpapers/themes; check legibility on both light and dark contexts if visually testing.
- Avoid turning the icon into a tiny cropped object at mdpi; verify the 48px bitmap still reads as a cassette.
- Do not remove API 19 bitmap fallbacks; existing legacy compatibility depends on them.

## Success criteria

- A fresh high-resolution transparent cassette logo graphic exists as source art; it is not merely the old opaque square with the background masked away.
- Launcher icon PNG fallbacks and the 1024 source have transparent background/corners.
- Cassette visual footprint is materially larger than before without obvious clipping, ideally 82–90% of canvas width for the master/legacy source.
- Adaptive icon no longer uses the opaque dark background color if the new graphic is wired into launcher resources in the same pass.
- Existing launcher-icon resource contracts pass after being expanded for transparency/scale/source-art expectations.
- Modern and legacy debug APKs build successfully when launcher resources are regenerated.
- Review evidence includes a checkerboard preview of the generated transparent logo graphic.
