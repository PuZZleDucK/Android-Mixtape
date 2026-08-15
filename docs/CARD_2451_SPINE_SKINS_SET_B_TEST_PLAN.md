# Card 2451 — Spine skins set B testing plan

## Expected red tests added in Testing

- `MixtapeSpineSkinSetBContractTest`
  - Requires `MixtapeSpineSkin` to keep the five existing saved enum names first and append exactly 10 new first-class set-B names:
    `PinkZineBorder`, `VioletLibraryStripe`, `MintCollageTab`, `AmberIndexBlock`, `PowderBlueMarker`, `CoralStickerRail`, `EmeraldNotebookLines`, `SepiaNewsprintFrame`, `BlackPhotoNegative`, `RainbowCutout`.
  - Confirms `MixtapeVisualProperties.spineSkin` remains persisted as the enum type.
  - Confirms settings defaults/missing/unknown saved settings normalize safely to all entries, while explicit saved subsets remain subsets.
  - Confirms `SharedPreferencesMixtapeVisualPropertiesStore` persists the stable `spine_skin` string and keeps the existing `CreamRed` fallback for missing/unknown per-mixtape values.

- `MixtapeSpineSkinSetBUiContractTest`
  - Confirms the Spine skin settings page uses `MixtapeSpineSkin.entries`, renders `SpineSkinPreview`, and keeps the last-enabled guard.
  - Requires a richer `CassetteSpineSkinPalette` than the current two-color `paper`/`strip` model so new skins can vary labels, borders/end caps, rules, and accents.
  - Requires `MixtapeSpineSkin.palette()` to include distinct branches for all 10 new set-B entries.
  - Requires `CassetteSpineRow` to use palette-driven label/border/end-cap/rule/accent styling and avoid copied reference text/brands/numbers.

## Red-check command

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk \
  ./gradlew :app:testModernDebugUnitTest \
  --tests 'com.example.androidmixtape.viewmodel.MixtapeSpineSkinSetBContractTest' \
  --tests 'com.example.androidmixtape.ui.MixtapeSpineSkinSetBUiContractTest'
```

Expected before implementation: these tests fail because only the original 5 enum entries exist, the palette has only two fields, and the row renderer does not yet use the richer palette/motif data.

## Running-stage acceptance checks

After implementation, run the targeted command above, then the broader card verification:

```sh
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```
