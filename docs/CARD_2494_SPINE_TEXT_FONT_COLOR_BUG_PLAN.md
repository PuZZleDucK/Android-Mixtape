# Card 2494 — Spine text alignment and font-color list refresh plan

## Bug summary

Two related visual regressions need fixing in the modern Compose UI:

1. In landscape Now Playing, the cassette-spine text shown in the left pane is visibly misaligned on the spine.
2. When the user changes the current tape/name font color, list presentations do not update to that color. This includes the main Mix Tapes library list and the Now Playing mix-tape list/spine preview.

## Likely source areas

- `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `CassetteSpineRow(...)`: shared cassette spine row used by the Mix Tapes list, the Now Playing current-spine preview, and the Now Playing mix-tape list. It currently hard-codes title ink as `Color(0xFF233C6E)` instead of using `group.visualProperties.nameColor.toComposeColor()`.
  - `MixTapeBriefcaseList(...)`: landscape/two-column and Now Playing list route into `CassetteSpineRow`.
  - `NowPlayingModeToggleAndSpine(...)`: left-pane landscape current spine preview route into `CassetteSpineRow`.
  - `CassetteTape(...)`: current cassette already uses `nameColor` for the tape name/current track; use this as the expected behavior reference.
- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt`
  - `MixtapeVisualProperties.nameColor` is already persisted and defaults from `symbolColor` for older saves.
- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt`
  - `cycleCurrentMixtapeNameColor()` updates persisted `nameColor` and calls `refreshCurrentUiState()`; verify the selected `MixTapeGroup.visualProperties` refreshes after this.
- Tests to extend:
  - `app/src/androidTestModern/java/com/example/androidmixtape/ui/MixtapeAppTest.kt`
  - `app/src/test/java/com/example/androidmixtape/ui/MixtapeSymbolInteractionUiContractTest.kt` or a new UI contract test for name-color propagation.
  - Existing view-model tests around symbol/name visual properties if refresh state needs coverage.

## Implementation plan

1. Reproduce/inspect the landscape misalignment path.
   - Launch Now Playing in landscape with enough tracks to show the left pane.
   - Toggle to the mode where the current cassette spine is shown next to the square mode button.
   - Compare the spine row there against the main Mix Tapes list row.

2. Make spine title ink data-driven.
   - In `CassetteSpineRow`, compute `val nameInk = group.visualProperties.nameColor.toComposeColor()` near the existing palette values.
   - Pass `nameInk` to the spine-title `JitteredHandwritingText` instead of hard-coded navy.
   - Keep the embellishment color on `group.visualProperties.symbolColor.toComposeColor()`; symbol and name colors are intentionally separate.
   - This should automatically cover both list views because both route through `CassetteSpineRow`.

3. Verify ViewModel refresh semantics.
   - After `cycleCurrentMixtapeNameColor()`, confirm `uiState.currentMixtapeVisualProperties.nameColor` and the matching selected `uiState.mixTapeGroups[...] .visualProperties.nameColor` both change.
   - If the selected group does not update, adjust the refresh/group rebuilding path so refreshed visual properties are copied into the list groups without resetting playback.

4. Fix alignment in `CassetteSpineRow` without breaking touch targets.
   - Keep the 64.dp minimum accessible row height.
   - Avoid the current content floating independently of the painted label lane in short/wide landscape contexts. Prefer aligning the row overlay to the drawn label band by reducing top/bottom padding and using `Box(contentAlignment = Alignment.Center)` or a fixed `heightIn(min = 64.dp)` content row centered vertically.
   - Consider lowering the spine title size in the spine row from 44.sp to a named row-title size around 32–36.sp if landscape left-pane screenshots show glyph clipping or baseline drift.
   - Preserve the existing `CassetteSpineTouchTargetRatioException` / physical spine-ratio comments.

5. Add regression coverage.
   - Compose UI test/contract: a MixTape group with `visualProperties.nameColor = MixtapeSymbolColor.Red` renders its spine title with red/name ink, not hard-coded navy.
   - ViewModel test: cycling/selecting name color updates the current visual properties and the corresponding list group visual properties.
   - UI/contract test: `CassetteSpineRow` title color must reference `group.visualProperties.nameColor.toComposeColor()`.
   - If feasible, add a landscape-sized Compose test asserting the spine-title semantics/bounds remain inside the row and visible in the Now Playing left preview.

## Success criteria

- Landscape Now Playing left-side cassette spine title is visually centered/aligned within the painted spine label and is not clipped.
- Changing the current tape/name font color updates:
  - the current cassette tape title,
  - the current spine preview in Now Playing,
  - the Now Playing mix-tape list,
  - the main Mix Tapes list after navigating back.
- Symbol color behavior remains unchanged and separate from name/font color.
- Existing cassette-spine semantics and tap behavior still work.
- Unit/contract/Compose tests cover the color propagation and at least one alignment guard.
- Build succeeds; per project instructions, deploy to the Kunlun Android emulator and capture screenshot evidence before review.

## Manual verification checklist

1. Build an emulator-compatible modern debug APK.
2. Install/launch on Kunlun emulator (`emulator-5554` if available).
3. Seed or use enough audio to create multiple mixtapes.
4. Start playback, tap the current tape name to cycle the tape/name font color, then confirm the list/spine titles change to the same color.
5. Rotate to landscape and confirm the left-side spine title alignment is correct.
6. Capture screenshots for the updated cassette, main Mix Tapes list, and landscape Now Playing left pane.
