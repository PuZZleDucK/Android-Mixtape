# Card 4831: spine title fit and centering

## Scope and findings

Planning only. No renderer implementation changes. Preserve the extensive unrelated working tree, including MixtapeApp.kt. Do not change track-list sizing, cassette artwork, font selection, decorative ID, or navigation.

Inspected both Nothing reference PNGs in docs/evidence/nothing-landscape-spine. Portrait title is visibly low; landscape clips its bottom. These are older-package reference images, not proof that the current modern build has identical pixels.

Current source confirms the scaling mismatch independently:

- MixtapeApp.kt, CassetteSpineRow at line 1815, sets width/height to 70/17. Painted paper uses proportional insets of 8/280 horizontally and 8/68 vertically.
- Its content Row instead uses fixed 18.dp horizontal and 12.dp vertical padding. Title sizes stay at 48–54.sp regardless of available height. Badge stays 28.dp. The title applies a separate font-specific vertical offset.
- HandwrittenCassetteFonts.kt maps all eight fonts and offsets only Gloria Hallelujah by -6.dp and Shadows Into Light by -3.dp. Those offsets cannot center each actual title's ink.
- JitteredHandwritingTextRenderer.kt requests height 1.45 times font size. Parent constraints can reduce that height without reducing the glyphs. It centers layout rectangles, clamps negative baseY to zero, and then adds dy and rotation. It neither measures transformed ink bounds nor reserves their vertical extent. Font padding and descenders therefore matter even without the fixed offsets.
- maxLines=2 does NOT wrap in this renderer. It disables the one-line truncation branch and draws successive tokens horizontally. Preserve this existing horizontal-overflow behavior; do not silently add wrapping, ellipsis, or shrink-to-full-title logic. Verify the actual surrounding gesture/scroll behavior before editing it.

At width 280.dp, spine height is 68.dp and Row height is 44.dp, while a 52.sp title requests 75.4.dp at fontScale 1. At width 240.dp the Row has only 34.3.dp. This proves the current geometry is inconsistent; exact ink clipping still needs current-build raster verification.

## Implementation steps

1. Capture current-build Kunlun portrait and landscape before changing production code. Use a deterministic fixture with decorative ID 45, title `restless neon weekend`, the purple reference appearance, selected font and jitter seed saved. Do not infer the selected font from the photograph alone. Record density, fontScale and measured spine bounds.
2. Introduce a spine-specific layout helper. Share the paper rectangle calculation with the drawing layer. Derive a safe title rectangle inside it, reserving badge width, gap and proportional padding. Retain badge identity and styling; only adjust its geometry if needed to stay inside the smaller label.
3. Measure the selected font, effective weight and italic style. Compute visible glyph ink bounds relative to a shared baseline, including fallback glyphs. Compose layout height or getBoundingBox alone is not guaranteed to be tight ink bounds. Validate with Android font/path bounds or a transparent offscreen raster fixture using the exact production typeface and style.
4. Apply the same deterministic jitter samples as drawing. Union the transformed glyph corners after dx, dy and rotation about the actual draw pivot. Exclude whitespace from the ink union but retain its advance. Include ink for the horizontally visible portion of long titles. Handle no-ink text without division by zero.
5. Fit preferred font size against usable vertical space, not full title width. Solve for the largest permitted size with transformed ink contained. Recompute samples/measurements consistently. Translate by `labelCenterY - (inkTop + inkBottom)/2`. Avoid the old baseY clamp and per-font dp nudge on the spine path.
6. Keep this behavior opt-in for the spine or use a dedicated renderer. JitteredHandwritingText also draws track lists and tape labels; changing its default globally risks unrelated layout regressions. Cache geometry by title, font, style, seed, messiness, density, fontScale and available size, not playback color.

## Expected behavior and regression matrix

For identical content/settings, identical available dimensions must produce identical geometry regardless of orientation. Smaller label height must not produce larger title ink. A physically scaled label must have comparable title-height ratio, allowing font raster rounding.

Add pure layout tests for 180, 240, 280, 360 and 480.dp widths at 70/17, density 1 and 2.75, fontScale 1, 1.3 and 2. Include tiny/zero bounds. Every result must be finite, with nonnegative size and no top/bottom overflow.

Add renderer tests with all eight packaged fonts, especially Kalam, Caveat, Gloria Hallelujah and Shadows Into Light. Exercise Off, Low and highest messiness with deterministic seeds, including seeds yielding extreme vertical displacement and rotation. Titles: reference title; `Agjpqy`; `ÉÅgj`; whitespace; empty; a single glyph; a long unbroken title; a long spaced title; emoji/fallback glyphs.

For actual raster ink within the title viewport, assert top and bottom remain inside the safe label rectangle with a 1-pixel tolerance and the ink-union center differs from label center by at most 1 physical pixel. Use a transparent test-only title render to isolate text from paper, badge and plastic gloss. Keep production-overlay screenshots as separate visual evidence. Check both a leading and horizontally shifted segment of long titles if scrolling exists in the current caller. Preserve title semantics and horizontal behavior.

Record matching before/after portrait and landscape full-screen captures. Show badge 45, unchanged purple style and unchanged track-list geometry. Exercise rotation during playback and confirm selected font, skin and jitter seed survive it. Verify library/preview spine callers too, because CassetteSpineRow is shared.

## Red tests and commands

Added app/src/test/java/com/example/androidmixtape/ui/CassetteSpineTitleSizingContractTest.kt. Three source-level guards reject the current unconstrained font-size call, font-specific dp offset and fixed vertical padding. These deliberately narrow guards are not a substitute for the behavior/raster tests above; deleting those strings alone does not satisfy acceptance.

Executed testModernDebugUnitTest filtered to that class: 3 tests ran, 3 assertion failures. Durable output: docs/evidence/cassette-spine-title/planning-red-tests.txt. Java and SDK were absent from the worker environment, so the successful test invocation used:

```sh
JAVA_HOME=/home/puzzleduck/.asdf/installs/java/temurin-21.0.10+7.0.LTS \
ANDROID_HOME=/home/puzzleduck/Android/Sdk \
./gradlew testModernDebugUnitTest --tests '*CassetteSpineTitleSizingContractTest'
```

Implementation gate: run the complete modern unit suite, lintModernDebug and assembleModernDebug with x86_64 compatibility, deploy with ./kunlun-sync.sh, then run targeted connected renderer tests on Kunlun and capture both orientations. Read the android-emulator skill before deployment. No phone access is required. No updated app exists in this planning stage, so deployment and after-images remain implementation work, not claimed evidence.

Keep disposable files in ignored .work/. Track any new durable binary screenshots through Git LFS. Existing phone references are already attached to the card; do not duplicate them. Do not push or commit unrelated work.
