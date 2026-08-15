# Card 2300 — Hand-drawn mixtape embellishments plan

## Goal

Add a small, deterministic collection of hand-drawn doodle embellishments to make each mixtape feel more personal. Each mixtape should receive one of 10 embellishments, assigned randomly once and then persisted with the existing per-mixtape visual properties. The selected embellishment should render to the left of the mixtape name in both:

1. the Mix Tapes cassette-spine list row, and
2. the Now Playing cassette/tape display label.

## Current implementation touchpoints

- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt`
  - Existing persisted per-mixtape visual state: `handwritingFont`, `baseJitterKey`.
  - `SharedPreferencesMixtapeVisualPropertiesStore` currently stores `font` and `base_jitter_key` per stable mixtape key.
- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt`
  - `visualPropertiesFor(stableMixtapeKey)` randomly assigns visual properties once, then stores them.
  - `MixtapeUiState.currentMixtapeVisualProperties` and each `MixTapeGroup.visualProperties` already flow into modern UI.
- `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `CassetteSpineRow(...)` renders the spine title with `JitteredHandwritingText` around lines ~635.
  - `CassetteTape(...)` renders the current tape name with `JitteredHandwritingText` around lines ~915.
  - Helpers already derive per-mixtape jitter keys and use deterministic visual rendering.
- Existing tests likely to extend:
  - `app/src/test/java/com/example/androidmixtape/viewmodel/MixtapeVisualPropertiesContractTest.kt`
  - `app/src/test/java/com/example/androidmixtape/viewmodel/MixtapeHandwritingFontViewModelTest.kt`
  - `app/src/test/java/com/example/androidmixtape/ui/CassetteSpineReadabilityContractTest.kt`
  - `app/src/test/java/com/example/androidmixtape/ui/HandwritingJitterRendererSourceContractTest.kt`
  - `app/src/androidTestModern/java/com/example/androidmixtape/ui/MixtapeAppTest.kt`

## Proposed embellishment set

Use a sealed/enum-like shared model so assignment is testable and persisted by stable enum name. Suggested 10 names:

1. `Star`
2. `Heart`
3. `LightningBolt`
4. `Sparkles`
5. `Smiley`
6. `Flower`
7. `MusicNote`
8. `Moon`
9. `Swirl`
10. `Crown`

Visual style: thin marker/ballpoint strokes, slightly imperfect, single-color dark ink matching the handwriting (`#1F2933` / current text color), with optional tiny accent strokes if already available from the cassette palette. Keep each doodle compact enough for a 20–28.dp square.

## Implementation plan

1. **Extend visual properties**
   - Add `MixtapeEmbellishment` enum in the shared viewmodel package or a small shared model file.
   - Add `val embellishment: MixtapeEmbellishment = MixtapeEmbellishment.Star` to `MixtapeVisualProperties`.
   - Update `SharedPreferencesMixtapeVisualPropertiesStore` to read/write an `embellishment` string key.
   - Preserve backward compatibility: if old prefs have font/jitter but no embellishment, return properties with a safe default or migrate by assigning/storing one the next time properties are requested. Prefer not to make old saved mixtapes disappear because one new field is missing.

2. **Assign once per mixtape**
   - In `MixtapeViewModel.visualPropertiesFor`, assign `embellishment = MixtapeEmbellishment.entries.random(mixtapeRandom)` alongside font and jitter.
   - Because the whole object is stored by stable mixtape key, the random choice becomes persistent and stable across recompositions/app launches.
   - Avoid runtime random calls in Composables.

3. **Render doodles in modern Compose**
   - Add a reusable `@Composable HandDrawnEmbellishment(embellishment, modifier, color)` or `DrawScope` helper in the modern UI layer.
   - Implement with `Canvas` primitives only; no new image assets needed.
   - Draw with `Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)` and simple `Path`/line/circle shapes.
   - Consider a tiny deterministic wobble helper seeded from `mixtapeJitterKey + embellishment.name + surface` if the doodles look too geometric, but keep the first pass simple unless tests can cover it.

4. **Place on spine row**
   - In `CassetteSpineRow`, add the doodle immediately left of the title text within the label/title row.
   - Keep layout width stable: a fixed `Modifier.size(22.dp)` plus title `JitteredHandwritingText(Modifier.weight(1f) or fillMaxWidth in a weighted row)`.
   - Preserve `Cassette spine <name>` content description and click target.
   - Add semantics such as `contentDescription = "Mixtape embellishment ${embellishment.displayName}"` only if it does not create noisy duplicate test nodes; visual-only can be acceptable if the mixtape title remains accessible.

5. **Place on tape display**
   - In `CassetteTape`, render the same embellishment to the left of the tape name label.
   - Thread `state.currentMixtapeVisualProperties.embellishment` into `CassetteTape(...)` where `currentMixtapeHandwritingFont`/`currentMixtapeJitterKey` are already passed.
   - Keep the tape-name text semantics discoverable by existing `onNodeWithText(...)` tests.

6. **Tests and contracts**
   - Add unit tests confirming 10 enum values and stable persistence through `MixtapeVisualPropertiesStore`.
   - Add/rework ViewModel tests: the same stable mixtape key gets the same saved embellishment; different new mixtapes can receive embellishments through the RNG-controlled path.
   - Add source contract tests requiring spine and tape display call the embellishment composable near the mixtape-name `JitteredHandwritingText` call sites.
   - Update modern connected Compose tests if layout bounds/content descriptions change.
   - Build both modern and legacy variants; the shared model change must not break the legacy API 19 build.

## Success criteria

- There are exactly 10 supported hand-drawn embellishments with clear names.
- Every mixtape has one persisted embellishment selected through the same per-mixtape visual-properties flow as handwritten font/jitter.
- The embellishment appears directly to the left of the mixtape name on cassette spines and on the Now Playing tape display.
- Embellishments are visually hand-drawn, compact, readable on small screens, and do not crowd or truncate titles.
- Existing cassette title text semantics, tap targets, jitter stability, and playback/settings flows are not regressed.
- Modern and legacy unit/build checks pass; connected/visual evidence confirms the doodles render in both required places.

## Verification checklist

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```

If a compatible emulator/device is available:

```sh
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew connectedModernDebugAndroidTest
```

Manual visual smoke:

1. Install `modernDebug` on a device/emulator with enough audio files for multiple mixtapes.
2. Launch, grant permission, and view the Mix Tapes list.
3. Confirm each spine row shows a doodle to the left of the title and titles remain readable in portrait and landscape.
4. Open at least two mixtapes and confirm the Now Playing tape label shows the same doodle that was shown on that mixtape's spine.
5. Restart the app and confirm embellishments do not reshuffle.

## Search/reference terms

- `hand drawn cassette doodles star heart lightning flower`
- `mixtape cover doodles marker pen stars hearts`
- `journal doodle icons simple line art`
- `hand drawn music note doodle cassette label`
- `retro mixtape handwritten sticker doodles`

## Risks / guardrails

- Do not generate a new embellishment during recomposition or each app launch; it must persist per mixtape.
- Do not make old saved `MixtapeVisualProperties` unreadable just because the new preference key is absent.
- Keep the doodle Canvas small and cheap; avoid adding heavy bitmap/vector assets unless a later design pass requires them.
- Avoid placing the doodle inside the jittered-text Canvas; separate composables keep text semantics and layout tests simpler.
- Keep the title lane wide enough after adding the icon, especially in landscape two-column spine layout.
