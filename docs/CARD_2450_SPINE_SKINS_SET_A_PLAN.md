# Card 2450 — Cassette spine skins reference set A plan

## Goal

Add 10 additional first-class `MixtapeSpineSkin` variants inspired by the colorful cassette-spine reference set A while keeping all app-rendered skins generic: no copied artist/title text, brand marks, catalog numbers, tape names, or reference-specific lettering.

## Current project facts to use

- `MixtapeSpineSkin` is defined in `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt` and is persisted by enum `name`.
- Existing saved enum names must not be renamed or reordered. Append new entries after current entries.
- Spine skin settings already iterate `MixtapeSpineSkin.entries`, so appended entries should appear automatically once palette/rendering branches are exhaustive.
- Persistence already has safe fallback behavior through `MixtapeSpineSkinSettings.fromNames(...)` and per-mixtape `MixtapeSpineSkin.CreamRed` fallback for unknown saved values.
- Spine rendering/palette code is in `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`; add distinct palette branches there.
- There is nearby set-B work in the tree. Avoid overwriting or renaming set-B entries; set A should be a second appended group of 10.

## Suggested set-A enum names

Use generic visual names only, for example:

1. `SunburstCreamRail`
2. `AquaLibraryTab`
3. `CandyStripePink`
4. `GoldenMemoBlock`
5. `TomatoBorderLabel`
6. `TealNotebookRail`
7. `VioletIndexPanel`
8. `LimeCutoutStripe`
9. `PeachGridSticker`
10. `MidnightRainbowFrame`

These names intentionally describe reusable color/layout treatments, not any reference text, brand, catalog number, or tape title.

## Visual direction

Capture reusable background/style elements from the reference set:

- bold horizontal cassette-spine striping and contrasting end caps;
- cream or pale label regions framed by saturated borders;
- blank sticker-tab/index-card blocks;
- thin notebook-rule lines and double rails;
- collage-like colored paper blocks;
- high-contrast dark frame variant with bright accent rails;
- warm retro palettes: tomato red, sunflower yellow, aqua, teal, lime, peach, violet, cream, charcoal.

Do not render words, numbers, barcodes, logos, catalog IDs, brand strips, or tape names. Keep any label areas blank except for the app's existing user/mixtape text overlay if already part of the normal renderer.

## Implementation steps

1. Append the 10 set-A `MixtapeSpineSkin` entries in `MixtapeVisualProperties.kt` after the existing entries.
2. Extend or reuse the existing `CassetteSpineSkinPalette` / `CassetteSpineSkinMotif` model in `MixtapeApp.kt` so the new skins can vary paper, label, strip, border/end-cap, rule, accent, and motif colors.
3. Add exhaustive `MixtapeSpineSkin.palette()` branches for each new enum name.
4. If the existing motif set cannot express set-A details, add small generic motif variants only (for example ruled label, split end cap, collage blocks, framed label). Keep renderer geometry text-free.
5. Confirm `SpineSkinSettingsScreen` still uses `MixtapeSpineSkin.entries` and does not need a separate hard-coded preview list.
6. Add/extend unit tests analogous to the set-B tests:
   - enum contains previous names first plus exactly 10 new set-A names appended;
   - settings default/migration includes all entries;
   - palette has distinct branches for every set-A entry;
   - renderer uses palette-driven labels/borders/rules/accents and does not include banned reference text.
7. Run targeted tests, then the broader modern debug unit test task/build used by this project.

## Verification commands

Use asdf-managed Java/Android paths where available:

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS" \
ANDROID_HOME="$HOME/Android/Sdk" \
./gradlew :app:testModernDebugUnitTest \
  --tests 'com.example.androidmixtape.viewmodel.MixtapeSpineSkinSetAContractTest' \
  --tests 'com.example.androidmixtape.ui.MixtapeSpineSkinSetAUiContractTest'

JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS" \
ANDROID_HOME="$HOME/Android/Sdk" \
./gradlew :app:testModernDebugUnitTest
```

If install/manual verification is attempted and device/emulator storage blocks install, follow `AGENTS.md`: document environment facts and do not treat storage exhaustion as product failure.

## Success criteria

- Exactly 10 new set-A `MixtapeSpineSkin` enum entries are added as first-class variants without renaming existing entries.
- New skins visually evoke the colorful cassette-spine reference through palette, striping, borders, label regions, and background elements only.
- No copied reference text, brand names, numbers, catalog IDs, or tape names are introduced.
- Settings previews discover all new skins through `MixtapeSpineSkin.entries`.
- Existing saved spine-skin values continue to load; unknown/missing values still fall back safely.
- Targeted set-A tests and the modern debug unit test task pass.

## Risks / watch-outs

- Enum order/name changes can break persisted saved settings; append only.
- Exhaustive Kotlin `when` branches must be updated for every new enum entry.
- Existing set-B changes may already have expanded palette/motif structures; build on them rather than duplicating incompatible models.
- Avoid overfitting the reference image: reusable layout/color inspiration is allowed, copied identifiers/text are not.
