# Card 2376 — Cassette tape skins plan

## Goal

Give each generated mixtape a persisted cassette tape skin for the Now Playing page, similar to how each tape already receives a stable handwriting font, jitter key, and symbol. The visual direction should be inspired by the linked cassette reference sheet, but avoid copying brand logos or trademarked text.

Reference: https://crosseyedpianist.com/wp-content/uploads/2018/01/mixtape-cassette-mixtape.jpg

## Current touchpoints

- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt`
  - `MixtapeVisualProperties` is the persisted per-mixtape visual identity model.
  - It currently stores `handwritingFont`, `baseJitterKey`, `embellishment`, and `symbolColor`.
  - `SharedPreferencesMixtapeVisualPropertiesStore` is the right migration point for old installs.
- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt`
  - `visualPropertiesFor(stableMixtapeKey)` assigns new visual properties once per stable mixtape identity and saves them.
  - `buildAssignedMixTapeGroups()` carries visual properties to library rows and Now Playing.
  - `selectMixTapeGroup()` copies the selected group properties into `currentMixtapeVisualProperties`.
- `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `NowPlaying(...)` passes current visual identity into `CassetteTape(...)`.
  - `CassetteTape(...)` currently hard-codes one shell/label palette in the Canvas.
  - Reel animation, title text, current-track text, and symbol interactions should keep working.
- Existing source/contract tests around visual identity, reel graphics, current-track color, and symbol interactions are useful regression guides.

## Proposed UX contract

1. Every generated mixtape receives exactly one stable `MixtapeTapeSkin` when first assigned visual properties.
2. The selected tape skin is persisted per stable mixtape key and survives recomposition, navigation away/back, app restart, and playback changes.
3. The Now Playing cassette uses that skin to change shell, label, accent stripe, window/well, screw, and optional decorative band colors.
4. The tape title, current track, selected symbol, and reel animation remain readable on every skin.
5. Skins are generic and inspired-by, not replicas. Do not draw `Sony`, `Scotch`, `Philips`, `Agfa`, or other brand marks from the reference image.
6. The legacy build should still compile; the legacy UI does not need a custom cassette-skin renderer.
7. After this implementation is completed, create a separate Todo card for adding the remaining samples from the reference sheet as additional generic skins.

## Initial skin set

Implement the framework plus a representative first pass of five generic skins. These cover the major visual variety in the reference while leaving clear follow-up scope:

1. `ClassicCreamDots`
   - Off-white shell, cream label, black lower well, orange/yellow/red dot accents.
   - Inspired by the top-left warm cream cassette.
2. `BlackMagentaStripe`
   - Black shell, white label, smoky center window, magenta/purple horizontal accent stripe.
   - Inspired by the top-middle black cassette.
3. `ChromeGreen`
   - White upper label, black lower shell, green accent strip and small green arrow/blocks.
   - Inspired by the top-right chrome/green cassette.
4. `CharcoalGold`
   - Charcoal shell, pale label, gold/ochre title band, darker lower panel.
   - Inspired by the second-row left dark/gold cassette.
5. `TranslucentViolet`
   - Pale label with blue/violet translucent shell hints and darker corner detail.
   - Inspired by the bottom-left translucent violet cassette.

If implementation time is tight, prioritize the model/store framework plus the first three skins, but keep the enum and renderer easy to extend.

## Implementation plan

1. Extend the shared visual model.
   - Add a stable enum in `MixtapeVisualProperties.kt`, for example `MixtapeTapeSkin`.
   - Add `tapeSkin: MixtapeTapeSkin = MixtapeTapeSkin.ClassicCreamDots` to `MixtapeVisualProperties`.
   - Keep enum entry names stable because SharedPreferences will store them.
2. Update persistence.
   - In `SharedPreferencesMixtapeVisualPropertiesStore.propertiesFor(...)`, read `tape_skin` by enum name.
   - Default missing/unknown `tape_skin` to `ClassicCreamDots` so old installs migrate safely.
   - In `saveProperties(...)`, write `properties.tapeSkin.name`.
   - In-memory store needs no special change beyond the data class field.
3. Assign skins per tape.
   - In `MixtapeViewModel.visualPropertiesFor(stableMixtapeKey)`, include a randomized `tapeSkin = MixtapeTapeSkin.entries.random(mixtapeRandom)` for new tapes.
   - Keep existing enabled-symbol migration logic intact; do not let a symbol migration reset the chosen skin.
   - Verify reset/rebuild flows only assign new skins when the stable mixtape key changes or no saved properties exist.
4. Thread the field into Now Playing.
   - Import `MixtapeTapeSkin` in modern UI.
   - Add `tapeSkin` to `CassetteTape(...)` parameters.
   - Pass `state.currentMixtapeVisualProperties.tapeSkin` from both portrait and landscape `NowPlaying(...)` branches.
   - Keep existing callbacks for symbol tap/long-press unchanged.
5. Replace hard-coded cassette colors with a skin palette.
   - Create a private UI-only palette data holder, e.g. `CassetteTapeSkinPalette`, mapping each `MixtapeTapeSkin` to Compose `Color` values.
   - Use palette values for shell, label, inner label, reel well, lower slot, screw color, tape path, and text colors where needed.
   - Add optional per-skin accent drawing helpers for stripes/dots/bands, but avoid overcomplicating the Canvas.
   - Preserve `CASSETTE_VISUAL_ASPECT_RATIO`, reel positions, and current label layout.
6. Accessibility/readability polish.
   - Keep `Cassette tape`, `Cassette reels spinning/stopped`, and `Current mixtape symbol` semantics.
   - If a dark skin needs lighter title/current-track ink, use palette title/current-track colors rather than global constants.
   - Ensure the selected symbol color still appears intentional against every label background.
7. Follow-up Todo creation.
   - Once the feature is implemented and accepted, create a new Todo card titled something like `[android-mixtape] Add remaining cassette tape skins from reference sheet`.
   - Include the reference URL, note that current card implemented the skin framework plus five representative skins, and list the remaining reference positions to adapt generically.

## Test plan

Add tests before or alongside implementation:

1. Model/persistence contracts
   - `MixtapeTapeSkin` enum exists with at least the initial first-pass skin entries.
   - `MixtapeVisualProperties` exposes `tapeSkin` with a default value.
   - `SharedPreferencesMixtapeVisualPropertiesStore` saves/loads `tapeSkin` using a stable `tape_skin` string key.
   - Missing/unknown saved skin names migrate to the default rather than failing to load otherwise valid visual properties.
2. ViewModel behavior
   - Generated groups receive non-null tape skins in their `visualProperties`.
   - Selecting a mixtape exposes that same skin as `currentMixtapeVisualProperties.tapeSkin` on Now Playing.
   - Navigating away/back keeps the same skin for the same stable key.
   - Symbol changes/color cycling do not overwrite the tape skin.
3. Modern UI source/contract tests
   - `CassetteTape(...)` accepts/uses `tapeSkin` rather than rendering only one hard-coded palette.
   - Both portrait and landscape `NowPlaying(...)` calls pass the current visual properties skin.
   - The existing cassette title, current-track text, reel semantics, and symbol semantics remain present.
4. Regression commands

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```

If a compatible emulator/device is available, also run the modern connected smoke test or manually verify with seeded audio.

## Manual acceptance checklist

1. Launch the modern app with enough audio to create multiple mixtapes.
2. Open several tapes from the library.
3. Confirm the Now Playing cassette skin varies between at least some tapes.
4. Navigate away/back to each tape; the same tape keeps the same skin.
5. Restart the app; the same tape still keeps its skin.
6. Confirm title/current-track text is readable on every implemented skin.
7. Confirm reels still animate while playing and symbol tap/long-press behavior still works.
8. Reset/rebuild mixtapes; newly generated stable tape identities may receive new skins.
9. Confirm modern and legacy unit tests/builds pass.
10. Create the follow-up Todo card for remaining reference-sheet skins after feature completion.

## Creative directions / prompt ideas

- “Draw a generic 1980s compact cassette UI with a cream shell, colorful label dots, black lower well, and handwritten mixtape title. Do not include brand logos.”
- “Create a dark cassette skin with a white label and a neon magenta stripe, preserving high contrast for handwritten blue marker text.”
- “Design a generic chrome-green compact cassette label with simple geometric accents and no trademarked markings.”
- “Use off-white plastic, aged paper labels, translucent tinted shells, screw heads, and small alignment ticks to evoke real cassette variety.”
- Future skin names can come from material/color language rather than brands: `LowNoiseGreen`, `FerroRed`, `BlueLibrary`, `IvoryNavy`, `ClearSmoke`, `LimeBlock`, etc.

## Search terms / references

- `Jetpack Compose Canvas themed cassette palette`
- `Kotlin enum SharedPreferences migration default`
- `Jetpack Compose DrawScope decorative stripes dots rounded rect`
- `cassette tape label color palettes 1980s generic`
- `compact cassette UI skeuomorphic design no trademarks`

## Risks and guardrails

- Trademark risk: use the reference for layout/color inspiration only; do not recreate brand names, logos, or product markings.
- Contrast risk: every palette needs explicit text colors that keep title and current track readable.
- Persistence risk: do not make missing `tape_skin` invalidate older saved visual properties.
- Randomness risk: keep assignment in the persisted visual-properties path, not transient Compose `remember` state.
- Scope risk: do not add a new skin settings screen unless explicitly requested; this card is about automatic assignment for the playing page.
- Regression risk: avoid moving reel geometry or transport layout while skinning the shell.
