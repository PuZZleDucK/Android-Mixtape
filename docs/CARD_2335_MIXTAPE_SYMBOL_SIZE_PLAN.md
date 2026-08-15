# Card 2335 — Mixtape symbol size plan

## Goal

Make the persisted hand-drawn mixtape symbol more prominent in both places where it appears:

- Now Playing cassette label: symbol on the cassette should be **4× bigger** than today.
- Mix tape library spine row: symbol on the cassette spine should be **2× bigger** than today.

## Current implementation touchpoints

- `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `HandDrawnEmbellishment(...)` is the reusable Canvas symbol renderer.
  - `CassetteSpineRow(...)` renders the library spine symbol from `group.visualProperties.embellishment` with `Modifier.size(22.dp)` immediately before `surface=spine-title`.
  - `CassetteTape(...)` renders the Now Playing cassette symbol from `embellishment` with `Modifier.size(24.dp)` immediately before `surface=cassette-name`.
  - `NowPlaying(...)` already passes `state.currentMixtapeVisualProperties.embellishment` into `CassetteTape(...)`; no ViewModel/model changes should be needed.
- Relevant existing tests:
  - `app/src/test/java/com/example/androidmixtape/ui/HandDrawnEmbellishmentUiContractTest.kt`
  - `app/src/test/java/com/example/androidmixtape/ui/HandwritingJitterRendererSourceContractTest.kt`
  - `app/src/test/java/com/example/androidmixtape/ui/CassetteSpineReadabilityContractTest.kt`

## Implementation plan

1. Add or extend a source-level UI contract test that extracts `CassetteSpineRow` and `CassetteTape` from `MixtapeApp.kt` and asserts the requested symbol sizes are encoded explicitly.
   - Spine row expected size: `44.dp` (current `22.dp × 2`).
   - Now Playing cassette expected size: `96.dp` (current `24.dp × 4`).
   - The assertions should target the `HandDrawnEmbellishment(` calls so unrelated icons/spinners are not matched.
2. Update `CassetteSpineRow(...)` so the `HandDrawnEmbellishment` modifier changes from `Modifier.size(22.dp)` to `Modifier.size(44.dp)`.
3. Update `CassetteTape(...)` so the `HandDrawnEmbellishment` modifier changes from `Modifier.size(24.dp)` to `Modifier.size(96.dp)`.
4. Preserve placement and data flow:
   - The spine symbol must remain directly before the mixtape name.
   - The Now Playing symbol must remain directly before the cassette name.
   - Both should continue using the persisted/current `MixtapeEmbellishment` value; do not introduce new randomization or settings changes.
5. Inspect layout after the size bump. If the 96.dp Now Playing symbol makes the title too cramped, adjust only surrounding spacing/alignment/min-width constraints while keeping the requested 96.dp symbol size. Prefer preserving one-line ellipsis for the name over shrinking the symbol.

## Success criteria

- Library cassette spines render the persisted mixtape symbol at 44.dp, twice the current 22.dp size.
- Now Playing cassette label renders the persisted mixtape symbol at 96.dp, four times the current 24.dp size.
- The symbol still appears immediately to the left of the mixtape name in both places.
- Existing title, spinner, handwriting jitter, and cassette spine readability behavior are not regressed.
- Modern and legacy unit tests pass.

## Suggested verification

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```

If a compatible device/emulator with audio files is available, manually verify:

1. Open the Mix Tapes list and confirm each spine symbol is visibly larger but row labels still ellipsize cleanly.
2. Open a mix tape / Now Playing and confirm the cassette-side symbol is much larger, still hand-drawn, and still attached to the same current mixtape identity.

## Risks / guardrails

- Do not change `HandDrawnEmbellishment` drawing internals unless the larger canvases reveal clipping; size changes should be at call sites.
- Do not resize `TitleGenerationSpinner`, settings-page previews, or other unrelated controls.
- Do not reduce row touch targets or remove single-line ellipsis to make space for the larger spine symbol.
- Keep changes in the modern Compose UI source; the legacy build should remain unaffected except for shared tests continuing to pass.
