# Card 2241 — Landscape two-column mixtape list test plan

## TDD contract added

Added `app/src/test/java/com/example/androidmixtape/ui/MixTapeLandscapeTwoColumnContractTest.kt` to lock the intended modern Compose Mix Tapes layout before implementation.

The contract verifies:

1. `MixTapeLibrary` inspects available constraints, e.g. `BoxWithConstraints`, and makes landscape explicit with width-vs-height logic.
2. Landscape Mix Tapes renders cassette spines in exactly two equal columns inside one case frame, accepting either `LazyVerticalGrid(GridCells.Fixed(2))` or an explicit `Row` with two weighted `LazyColumn`s.
3. `CassetteSpineRow` keeps its compact 64.dp-ish minimum height/touch target and remains spine-shaped instead of using the full `CASSETTE_VISUAL_ASPECT_RATIO` cassette art treatment.

## Expected initial failure

Current source is expected to fail because `MixTapeLibrary` is still a single `LazyColumn` for all orientations and does not branch on `maxWidth > maxHeight` / `isLandscape`.

## Implementation expectations for Running

- Preserve portrait behavior as the current single-column cassette briefcase list.
- Add a landscape branch that renders two cassette-spine columns inside the shared dark/brown/brass briefcase frame.
- Preserve row numbering, tap behavior, semantics, ellipsis, track counts, first-track hints, and the existing 64.dp-ish row height.
- Do not change Now Playing layout or cassette aspect-ratio behavior for this card unless required by compile/test failures.

## Focused red/green command

```sh
cd /home/puzzleduck/x/android-mixtape
./gradlew testModernDebugUnitTest --tests 'com.example.androidmixtape.ui.MixTapeLandscapeTwoColumnContractTest'
```

## Regression checks after implementation

```sh
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
```

If a modern device/emulator is available, visually smoke the Mix Tapes screen in portrait and landscape with at least six mixtapes and confirm landscape shows two cassette-spine columns in one case frame.
