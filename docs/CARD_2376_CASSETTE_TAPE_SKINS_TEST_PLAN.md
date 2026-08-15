# Card 2376 — Cassette tape skins test plan

## Expected behavior to implement

- Add a stable `MixtapeTapeSkin` enum with first-pass generic skins: `ClassicCreamDots`, `BlackMagentaStripe`, `ChromeGreen`, `CharcoalGold`, and `TranslucentViolet`.
- Persist `MixtapeVisualProperties.tapeSkin` with a `tape_skin` SharedPreferences key.
- Safely migrate old or unknown saved values to `MixtapeTapeSkin.ClassicCreamDots` without discarding otherwise valid visual properties.
- Assign a random skin once when `visualPropertiesFor(stableMixtapeKey)` creates new visual properties, then preserve it through symbol migrations/color changes.
- Thread `state.currentMixtapeVisualProperties.tapeSkin` into both portrait and landscape `CassetteTape` calls.
- Render the modern Now Playing cassette from a skin palette/accent mapping while preserving existing cassette/reel/symbol accessibility semantics.

## Failing TDD specs added

- `app/src/test/java/com/example/androidmixtape/viewmodel/MixtapeTapeSkinContractTest.kt`
  - Verifies the persisted enum/model/store/ViewModel contract.
- `app/src/test/java/com/example/androidmixtape/ui/CassetteTapeSkinUiContractTest.kt`
  - Verifies Now Playing wiring and renderer palette/accent contracts.

## Regression commands

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest --tests '*MixtapeTapeSkinContractTest' --tests '*CassetteTapeSkinUiContractTest'
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```

The first command is expected to fail before implementation and should pass after the Running stage implements the card.
