# Card 2451 — Cassette spine skins reference set B plan

## Goal

Add 10 additional first-class `MixtapeSpineSkin` variants inspired by the colorful stacked cassette-spine reference set B, while keeping the app's generated spines generic: no copied artist/title text, brand marks, numbers, catalog IDs, or tape names.

Reference: https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT8hj72H7UbHARP9wE9pe0I0396ZodM52Jw1NY3Rck5o0t7Drf8ue7DVQA&s=10
Local reference copy for implementation QA: `docs/evidence/card2451-reference-set-b.jpg`

## Current touchpoints

- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt`
  - `MixtapeSpineSkin` currently has 5 entries: `CreamRed`, `BlushBlue`, `SkyGreen`, `MintAmber`, `LemonNavy`.
  - Per-mixtape visual properties persist spine skins by enum `name`, so new entries should be appended/renamed carefully.
  - `MixtapeSpineSkinSettings.allSpineSkins()` and enabled-skin parsing already use `MixtapeSpineSkin.entries`.
- `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `SpineSkinSettingsScreen` already iterates `MixtapeSpineSkin.entries`, so new enum entries automatically appear in settings previews once the palette/rendering `when` is exhaustive.
  - `CassetteSpineRow(...)` currently renders one shared layout with only two per-skin colors: `paper` and `strip`.
  - `MixtapeSpineSkin.palette()` is the main required modern-UI change; adding richer palette fields and simple pattern metadata is the cleanest way to capture the reference styles.

## Proposed 10 new enum entries

Append these after the existing entries to minimize churn. Names are generic color/material language, not copied from visible cassette text.

1. `PinkZineBorder`
   - Pale off-white/pink paper, hot-pink uneven edge rail, small black/gray end-cap detail.
   - Captures hand-labeled punk/zine energy without copying lettering.
2. `VioletLibraryStripe`
   - Lavender paper, navy/violet side stripe, thin cream label strip, cool gray caps.
   - Inspired by the blue-purple spines and library-label feel.
3. `MintCollageTab`
   - Mint/seafoam paper, coral/pink accent tab, soft green rule lines, layered-label look.
   - Captures collage-like pastel blocks.
4. `AmberIndexBlock`
   - Aged cream paper with amber/tan left index block and warm brown borders.
   - Inspired by TD-style side blocks, but without letters or numbers.
5. `PowderBlueMarker`
   - Powder-blue label region, teal accent rail, pale gray body, darker blue rule lines.
   - Good contrast with existing dark-blue handwritten title text.
6. `CoralStickerRail`
   - Cream body, coral/red vertical sticker rail, light salmon inner label, subtle dot/tick marks.
   - Evokes bright red label chunks in the reference without catalog text.
7. `EmeraldNotebookLines`
   - Cream/green paper, emerald strip, multiple faint ruled notebook lines.
   - Captures green marker and ruled-card spines.
8. `SepiaNewsprintFrame`
   - Warm newsprint paper, sepia outer frame, muted gray/brown end caps, faint paper grain marks.
   - Inspired by aged/yellowed spines.
9. `BlackPhotoNegative`
   - Dark charcoal end caps/frame, pale central label, electric blue/magenta tiny accents.
   - Represents the few high-contrast black-backed spines.
10. `RainbowCutout`
   - Cream base with small mismatched pastel blocks/stripes at edges: teal, pink, yellow, green.
   - Captures the reference's stacked, handmade multi-color variety.

## Implementation plan

1. Add the 10 enum entries in `MixtapeVisualProperties.kt`.
   - Do not rename existing entries; saved preferences depend on `name`.
   - Keep `MixtapeVisualProperties.spineSkin` default as `CreamRed`.
2. Upgrade spine palette data in `MixtapeApp.kt`.
   - Expand `CassetteSpineSkinPalette` beyond `paper`/`strip`, e.g. `paper`, `label`, `strip`, `border`, `endCap`, `rule`, `accent`, and optional `motif`.
   - Introduce a small private `CassetteSpineSkinMotif` enum if useful: `Plain`, `DoubleRail`, `NotebookRules`, `StickerTab`, `CollageBlocks`, `DarkFrame`.
   - Add exhaustive palette cases for all 15 spine skins.
3. Make `CassetteSpineRow(...)` use the richer palette.
   - Keep existing row height, click semantics, screw positions, and text layout stable.
   - Replace hard-coded cap/border/rule colors with palette values.
   - Add lightweight background motifs before drawing title text: narrow vertical rails, ruled lines, small edge blocks, or a dark frame.
   - Do not render any text-like marks, logos, numbers, or copied symbols from the reference.
4. Settings/previews.
   - Confirm `SpineSkinSettingsScreen` still uses `MixtapeSpineSkin.entries.forEach`, so all new skins appear automatically.
   - Confirm `SpineSkinPreview` passes each enum through `CassetteSpineRow`, exercising the same renderer.
5. Persistence/migration safety.
   - Per-mixtape saved `spine_skin` values should still load by enum name; unknown/old bad names should continue to default to `CreamRed`.
   - Enabled-skin settings should still normalize empty/invalid sets to all entries.
   - Decide during implementation whether saved legacy “all five enabled” should migrate to all 15 enabled; if so, add an explicit contract test for the legacy-all set. Otherwise document that existing users can enable new skins from settings and new installs get all entries by default.
6. Tests.
   - Add/extend a viewmodel contract asserting `MixtapeSpineSkin.entries.size >= 15` and the 10 named entries exist.
   - Add/extend persistence tests so each new enum name round-trips through `SharedPreferencesMixtapeVisualPropertiesStore` and unknown names fall back safely.
   - Add/extend UI source/contract tests to require `MixtapeSpineSkin.palette()` handles every enum entry and that settings previews iterate entries rather than a hard-coded list.

## Success criteria

- Exactly 10 new first-class `MixtapeSpineSkin` enum entries are added, for 15 total.
- All 10 new skins are visually distinct in the spine settings preview and in assigned mixtape rows.
- The new designs are inspired by the reference palettes/layouts but contain no copied text, brands, numbers, or tape names.
- Existing saved spine skins continue to load; missing/unknown saved values do not crash.
- New skins are discoverable through `MixtapeSpineSkin.entries` without a separate hard-coded preview list.
- Existing cassette spine click/semantics, selected/current highlighting, handwriting, and embellishment rendering remain unchanged.
- Modern and legacy unit tests/builds pass.

## Verification commands

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```

If a compatible emulator/device is available, also run a modern manual smoke pass:

1. Open Settings → Spine skin settings.
2. Confirm all 15 skins appear with previews.
3. Confirm the 10 new skins have varied rails/borders/labels/background accents.
4. Generate/select multiple mixtapes and confirm assigned spine skins persist across navigation/restart.
5. Confirm title text and embellishments remain readable on every new skin.

## Creative directions / prompt ideas

- “Generic hand-labeled cassette spine UI, pale paper label, neon marker rail, small sticker blocks, no words or numbers.”
- “1980s cassette storage shelf spine with pastel collage strips, ruled notebook label, worn end caps, no brand marks.”
- “Punk zine cassette spine background: pink rail, cream paper, black edge frame, handmade but text-free.”
- “Aged library cassette spine: sepia paper, muted border, simple label region, no catalog codes.”
- “Dark high-contrast cassette spine with pale central label and tiny blue/magenta accents, no logos.”

## Search terms / references

- `Jetpack Compose Canvas cassette spine label stripes`
- `Kotlin enum SharedPreferences name migration`
- `Compose DrawScope ruled paper lines rounded rectangles`
- `generic cassette spine design no text no logo`
- `1980s cassette label color palette pastel marker`

## Risks and guardrails

- IP/trademark risk: copy layout/color energy only; never copy visible words, brand blocks, catalog numbers, logos, or album/tape names.
- Readability risk: busy patterns can fight title handwriting; keep motifs low-alpha and outside the main text lane when possible.
- Persistence risk: enum names are saved strings; avoid renaming and keep unknown-value fallback.
- Settings migration risk: adding entries can interact with previously saved enabled-skin sets; cover intended behavior in tests.
- Scope risk: do not rework the cassette row layout or playback behavior; this is a visual skin expansion.
