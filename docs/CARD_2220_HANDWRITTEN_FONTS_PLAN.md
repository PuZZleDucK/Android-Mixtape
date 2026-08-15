# Card 2220 — handwritten cassette fonts plan

## References

- Font source directory: `/home/puzzleduck/agent-projects/fonts/fonts`
  - `kalam.ttf`
  - `patrick-hand.ttf`
  - `caveat.ttf`
  - `nanum-pen-script.ttf`
  - `indie-flower.ttf`
  - `gloria-hallelujah.ttf`
  - `architects-daughter.ttf`
  - `shadows-into-light.ttf`
- Comparison image: `/home/puzzleduck/agent-projects/fonts/samples/handwritten-fonts-cassette-demo.png` (1500 x 2056 PNG)

## Current implementation notes

- Modern Compose UI is in `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`.
- Mixtape grouping/state is in `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt`.
- Existing cassette handwriting locations currently use `FontFamily.Cursive`:
  - `CassetteSpineRow` for spine name and first-track hint.
  - `CassetteTape` for the tape name and selected-track title.
  - `CassetteCoverTrackList` for the track-list header and rows.
- `MixtapeViewModel` already accepts injectable `mixtapeRandom: Random`, so deterministic unit tests can cover font assignment.

## Implementation plan

1. Copy all eight TTFs into Android font resources, preferably `app/src/modern/res/font/` because only the modern Compose flavor uses the cassette UI.
   - Rename hyphenated files to Android resource-safe names:
     - `kalam.ttf`
     - `patrick_hand.ttf`
     - `caveat.ttf`
     - `nanum_pen_script.ttf`
     - `indie_flower.ttf`
     - `gloria_hallelujah.ttf`
     - `architects_daughter.ttf`
     - `shadows_into_light.ttf`
2. Add a shared model enum in `MixtapeViewModel.kt`, e.g. `MixtapeHandwritingFont`, with one entry per candidate font.
3. Add `val handwritingFont: MixtapeHandwritingFont` to `MixTapeGroup` with a safe default to avoid breaking existing pure grouping tests.
4. Keep `buildMixTapeGroups(...)` pure and deterministic for existing tests; assign random fonts in `MixtapeViewModel` when groups become UI mixtapes.
   - Use a stable assignment map keyed by group identity, such as the ordered track IDs plus `startIndex` and group name.
   - Use `mixtapeRandom` for `MixtapeHandwritingFont.entries.random(mixtapeRandom)`.
   - Do not roll a new font during recomposition or repeated calls to `showMixTapes`, `showSettings`, `backToMixTapes`, or `selectMixTapeGroup`.
   - Clear/rebuild assignments when mixtapes are intentionally recreated by `resetAllMixTapes()`; setting changes may reuse keys for still-identical groups and assign new fonts for new groups.
5. Expose the selected font to Now Playing.
   - Add `currentMixtapeHandwritingFont` or `selectedMixTapeGroup` to `MixtapeUiState`.
   - In `selectMixTapeGroup(index)`, set the selected/current font from the clicked `MixTapeGroup`.
6. Add a modern UI font mapper, e.g. `HandwrittenCassetteFonts.kt`, mapping `MixtapeHandwritingFont` to Compose `FontFamily(Font(R.font...))`.
   - Add an `effectiveCassetteWeight(preferred: FontWeight)` helper that forces at least `FontWeight.Bold` for `IndieFlower` and `ShadowsIntoLight`.
7. Replace `FontFamily.Cursive` in cassette handwriting locations with the selected per-mixtape font family:
   - `CassetteSpineRow`: use `group.handwritingFont` for spine name and first-track hint.
   - `CassetteTape`: accept a `handwritingFont` parameter and use it for the mixtape name and selected-track title.
   - `CassetteCoverTrackList`: accept a `handwritingFont` parameter and use it for the track-list header, rows, and empty-state note.
8. Keep all non-handwriting UI text on normal Material fonts unless it is intentionally cassette writing.

## Tests / verification

- Add or update unit tests in `app/src/test/java/com/example/androidmixtape/viewmodel/`:
  - All generated `MixTapeGroup`s in `MixtapeUiState` receive a candidate `MixtapeHandwritingFont`.
  - Font choices remain stable across repeated state refreshes/navigation for the same created mixtapes.
  - `selectMixTapeGroup(index)` exposes the selected group font in Now Playing state.
  - With injected `Random(0)`, behavior is deterministic.
- Update the existing `HandwrittenFontReadabilityContractTest` so it no longer expects `FontFamily.Cursive`; it should verify the cassette handwriting paths use the new mapped font family and explicit/effective weights.
- Add a resource contract test or simple source assertion that all eight expected resource names exist under `app/src/modern/res/font/`.
- Run:
  - `./gradlew testModernDebugUnitTest`
  - If a device/emulator is available, install/launch the modern debug APK and visually confirm several mixtapes show different handwritten fonts and that one selected mixtape keeps the same font in spine, tape name, and track list.

## Risks / cautions

- Android resource filenames cannot contain hyphens, so rename on copy.
- Avoid random font selection inside Composables; that would reroll on recomposition and violate stability.
- Avoid random font selection inside pure `buildMixTapeGroups(...)`; existing tests rely on deterministic grouping.
- The app currently has in-memory mixtape state rather than a durable database, so “persist” should be interpreted as stable for the created mixtape lifecycle unless a broader durable-mixtape persistence feature is added.
