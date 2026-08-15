# Card 2300 — Hand-drawn embellishments test plan

## Expected behavior

- Define one shared `MixtapeEmbellishment` enum with exactly 10 stable persisted names: `Star`, `Heart`, `LightningBolt`, `Sparkles`, `Smiley`, `Flower`, `MusicNote`, `Moon`, `Swirl`, `Crown`.
- Add `embellishment` to `MixtapeVisualProperties` so each mixtape carries font, base jitter key, and doodle identity together.
- Assign an embellishment through the existing `MixtapeViewModel.visualPropertiesFor(...)` random-and-store path, not from Composables.
- Keep embellishments stable across navigation/state refresh and expose the selected mixtape's embellishment through `currentMixtapeVisualProperties` for Now Playing.
- Render a reusable Canvas `HandDrawnEmbellishment` immediately left of the mixtape name in both `CassetteSpineRow` and `CassetteTape`.
- Preserve weighted title width so long cassette names still ellipsize instead of being crowded by the doodle.

## Added failing specs

- `MixtapeEmbellishmentContractTest`
  - Requires the 10-entry enum catalog.
  - Requires `MixtapeVisualProperties` to expose an embellishment value.
  - Verifies generated mixtape groups receive candidate embellishments and keep them across navigation.
  - Verifies the Now Playing state exposes the selected spine's same embellishment.
- `HandDrawnEmbellishmentUiContractTest`
  - Requires a reusable Canvas doodle composable with branches for all 10 embellishments.
  - Requires the spine row to draw the persisted doodle before the title.
  - Requires Now Playing to pass the current visual-properties embellishment into `CassetteTape`, and `CassetteTape` to draw it before the cassette name.

## Commands

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest --tests 'com.example.androidmixtape.viewmodel.MixtapeEmbellishmentContractTest' --tests 'com.example.androidmixtape.ui.HandDrawnEmbellishmentUiContractTest'
```

Expected before implementation: unit tests fail because `MixtapeEmbellishment`, the persisted `embellishment` visual property, and `HandDrawnEmbellishment` UI call sites do not exist yet.

After implementation, run the broader checks from the planning doc:

```sh
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```
