# Card 2416 — Mixtape symbol/spine consistency plan

## Goal

When the user changes the current mixtape symbol or marker color from the Now Playing cassette, the matching Mix Tapes library spine must show the same persisted symbol and color for that mixtape. The Now Playing cassette label and the cassette spine should always represent the same `MixtapeVisualProperties` identity.

## Current diagnosis

Relevant code paths:

- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt`
  - `MixtapeVisualProperties` already carries `embellishment` and `symbolColor`.
  - `SharedPreferencesMixtapeVisualPropertiesStore` already persists both values per stable mixtape key.
- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt`
  - `cycleCurrentMixtapeSymbolColor()` and `selectCurrentMixtapeEmbellishment(...)` update/save `currentMixtapeVisualProperties` and call `refreshCurrentUiState()`.
  - `buildAssignedMixTapeGroups()` rebuilds each spine row group from `visualPropertiesFor(stableKey)`, so it should be the source of truth for the library spine.
- `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `CassetteTape(...)` receives `state.currentMixtapeVisualProperties.embellishment` and `symbolColor`, then renders the Now Playing symbol with the selected color.
  - `CassetteSpineRow(...)` renders `group.visualProperties.embellishment`, but the color is currently hard-coded as `Color(0xFF233C6E)` instead of `group.visualProperties.symbolColor.toComposeColor()`.

Likely bug: symbol choice persistence is mostly wired, but the spine color cannot reflect user changes because the spine ignores `group.visualProperties.symbolColor`. Add tests to make both the color and selected symbol refresh contract explicit.

## Implementation plan

1. Add/extend a source-level UI contract test in `app/src/test/java/com/example/androidmixtape/ui/MixtapeSymbolInteractionUiContractTest.kt`.
   - Extract `CassetteSpineRow`.
   - Assert it renders `HandDrawnEmbellishment` from `group.visualProperties.embellishment`.
   - Assert the spine symbol color comes from `group.visualProperties.symbolColor` (or equivalent persisted ink value) and `toComposeColor()`, not a hard-coded navy literal.
2. Add a ViewModel regression assertion in `MixtapeSymbolInteractionViewModelContractTest`.
   - Select a mixtape.
   - Change color while playing and verify the selected entry inside `uiState.value.mixTapeGroups` has the same `visualProperties.symbolColor` as `currentMixtapeVisualProperties`.
   - Select a different enabled symbol and verify the selected group in `mixTapeGroups` also reflects the same `embellishment`.
   - Keep existing playback-preservation checks intact.
3. Update `CassetteSpineRow(...)` in `MixtapeApp.kt`.
   - Change the spine `HandDrawnEmbellishment` color argument from hard-coded `Color(0xFF233C6E)` to `group.visualProperties.symbolColor.toComposeColor()`.
   - Keep the current `Modifier.size(44.dp)`, row click behavior, text layout, and semantics unchanged.
4. If the ViewModel regression fails, adjust only the refresh path so `mixTapeGroups` is rebuilt from the saved properties after symbol/color changes.
   - Prefer the existing `refreshCurrentUiState()`/`buildAssignedMixTapeGroups()` source-of-truth flow.
   - Avoid duplicated transient UI-only state.

## Success criteria

- Changing the Now Playing symbol color updates the matching Mix Tapes spine symbol color after returning to or viewing the library list.
- Choosing a different Now Playing symbol updates the matching Mix Tapes spine symbol for the same stable mixtape key.
- The Now Playing cassette remains unchanged except for the intended symbol/color update.
- Playback state, current track, queue order, and current index are not reset by symbol/color edits.
- Disabled symbols still cannot be selected through the long-press selector.
- The 44.dp spine symbol and 96.dp Now Playing symbol sizes remain unchanged.

## Verification

Run targeted tests first:

```sh
./gradlew testModernDebugUnitTest --tests '*MixtapeSymbolInteraction*'
./gradlew testModernDebugUnitTest --tests '*HandDrawnEmbellishmentUiContractTest*'
```

Then run the broader unit suite if time permits:

```sh
./gradlew testModernDebugUnitTest
```

Manual smoke check on a compatible device/emulator:

1. Launch the modern app with a small audio library.
2. Open a mixtape and start playback.
3. Tap the large Now Playing symbol; confirm the color changes.
4. Long-press the large symbol and choose a different symbol.
5. Return to Mix Tapes; the corresponding cassette spine shows the same symbol and color.
6. Reopen the mixtape; Now Playing still matches the spine.

## Risks / notes

- Do not change `MixtapeEmbellishment` enum names/order or persisted preference keys.
- Do not make spine symbols interactive for this card; the interaction remains on the Now Playing symbol.
- Keep `MixtapeSymbolColor.toComposeColor()` in the modern UI layer so shared viewmodel code does not gain a Compose dependency.
- If connected-device install is blocked by emulator/device storage, document that as environment limitation rather than product failure.
