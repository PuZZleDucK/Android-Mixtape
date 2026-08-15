# Card 2348 — Mixtape image settings TDD plan

## Added/updated failing checks

- `MixTapeGroupingTest`
  - Default `buildMixTapeGroups(...)` now expects 24-track mixtapes.
  - `MixtapeSettings()` now expects `ArtistGrouping.ArtistTripletsAcrossTapes`.
- `MixTapeQueueViewModelTest`
  - Default UI state now expects 24 songs per mixtape, triplets-across-tapes grouping, and corresponding group sizes.
- `MixtapeSymbolSettingsContractTest`
  - Requires shared `MixtapeSymbolSettings`, `MixtapeSymbolSettingsStore`, in-memory store, and SharedPreferences store.
  - Requires ViewModel state/navigation/toggle APIs for symbol settings.
  - Guards against random selection from the full `MixtapeEmbellishment.entries`; assignment must use the enabled set and migrate disabled persisted assignments.
- `MixtapeSymbolSettingsScreenContractTest`
  - Requires Settings -> Mixtape symbol settings navigation.
  - Requires dedicated modern Compose page listing all `MixtapeEmbellishment.entries` with `HandDrawnEmbellishment` previews and checkbox/switch controls.
  - Requires UI and MainActivity wiring for persistent symbol settings and ViewModel callbacks.

## Running implementation checklist

1. Add global symbol settings model/store and persistence.
2. Add ViewModel screen/state/callbacks and enforce at least one enabled symbol.
3. Use enabled symbols when assigning visuals; migrate disabled persisted visuals while preserving font/jitter key.
4. Add modern settings page and MainActivity wiring.
5. Change defaults to 24 songs and `ArtistTripletsAcrossTapes`.
6. Run:

```sh
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest --tests '*MixtapeSymbolSettings*' --tests '*MixTapeGroupingTest*' --tests '*MixTapeQueueViewModelTest*'
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
```
