# Card 2379 — Mixtape symbol interaction plan

## Goal

On the modern Now Playing cassette, make the current mixtape symbol interactive:

- Tap/click the symbol while playback is active to change the symbol color.
- Long-press the symbol to open a selector for choosing a different mixtape symbol.

Keep the behavior tied to the current mixtape identity so returning to a tape shows its chosen symbol and color.

## Current touchpoints

- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt`
  - `MixtapeVisualProperties` currently persists `handwritingFont`, `baseJitterKey`, and `embellishment` per stable mixtape key.
  - SharedPreferences persistence is already centralized in `SharedPreferencesMixtapeVisualPropertiesStore`.
- `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt`
  - `currentMixtapeStableKey` and `currentMixtapeVisualProperties` already track the open/playing tape.
  - `visualPropertiesFor(stableMixtapeKey)` already migrates disabled symbols to an enabled replacement.
  - `mixtapeSymbolSettings.enabledEmbellishments` should constrain the long-press selector choices.
- `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `HandDrawnEmbellishment(...)` is the reusable Canvas renderer.
  - `NowPlaying(...)` passes `state.currentMixtapeVisualProperties.embellishment` to `CassetteTape(...)`.
  - `CassetteTape(...)` renders the large Now Playing symbol directly before the tape name.
  - Existing `MixtapeSymbolSettingsScreen` is a global enable/disable screen, not a per-tape selector.

## Proposed UX contract

1. The Now Playing symbol has a clear semantics label such as `Current mixtape symbol`.
2. A normal tap only changes color when `state.isPlaying == true` and a current mixtape is selected.
   - If the tape is paused/stopped, the tap should be a no-op to match the card wording.
   - Do not toggle playback, seek, or change tracks from this tap.
3. Each successful tap cycles through a small, readable cassette-label color palette, for example:
   - navy `#263864` (current default)
   - red `#D32F2F`
   - green `#2E7D32`
   - purple `#6A3D9A`
   - amber/brown `#8A5A14`
4. The selected color is saved per mixtape in `MixtapeVisualProperties` so the spine/Now Playing identity remains stable. If implementation scope needs to be narrower, at minimum persist the Now Playing color per stable mixtape key; do not use transient `remember` state only.
5. A long press opens a lightweight per-tape selector dialog/dropdown showing only `state.enabledMixtapeEmbellishments`.
6. Choosing an item updates and persists `currentMixtapeVisualProperties.embellishment`, refreshes the current UI, and dismisses the selector.
7. If global settings later disable the selected symbol, the existing migration path should still replace it with an enabled symbol and preserve the chosen color.

## Implementation plan

1. Extend the visual model.
   - Add a stable enum/value model such as `MixtapeSymbolColor` or `MixtapeSymbolInk` with display name and hex/Compose color mapping in modern UI.
   - Add `symbolColor` to `MixtapeVisualProperties` with the current navy as the default.
   - Update `SharedPreferencesMixtapeVisualPropertiesStore` to read/write `symbolColor`, defaulting to navy when absent for old installs.
   - Keep enum names stable because SharedPreferences will store them.
2. Add ViewModel actions.
   - `cycleCurrentMixtapeSymbolColor()` should no-op unless the controller/ui is playing and `currentMixtapeStableKey` is non-null.
   - `selectCurrentMixtapeEmbellishment(embellishment: MixtapeEmbellishment)` should no-op if the embellishment is not enabled, otherwise copy/save current properties for the current stable key.
   - Both actions should call `visualPropertiesStore.saveProperties(stableKey, updated)` and refresh current UI state.
3. Wire callbacks from Activity to Compose.
   - Add `onCycleCurrentMixtapeSymbolColor` and `onCurrentMixtapeEmbellishmentSelected` to `MixtapeApp(...)`, `NowPlaying(...)`, and `CassetteTape(...)`.
   - In `modern/MainActivity.kt`, pass `viewModel::cycleCurrentMixtapeSymbolColor` and `viewModel::selectCurrentMixtapeEmbellishment`.
4. Make only the Now Playing symbol interactive.
   - Wrap the `HandDrawnEmbellishment` call in `CassetteTape(...)` with a `Box` using `combinedClickable(onClick = ..., onLongClick = ...)`.
   - Keep the existing `96.dp` visual size and add padding/minimum touch target if needed without shrinking the symbol.
   - Use the persisted color instead of the hard-coded `Color(0xFF263864)` at this call site.
   - Leave cassette spine rows non-interactive except for their existing row click behavior.
5. Implement the selector UI.
   - Use a small `AlertDialog` or `DropdownMenu` owned by `CassetteTape(...)`/`NowPlaying(...)` state.
   - Title: `Choose mixtape symbol`.
   - Render one row per enabled `MixtapeEmbellishment`, ideally with a small `HandDrawnEmbellishment` preview plus humanized enum label.
   - Mark the current symbol and keep at least one option available by falling back to all entries if enabled set is unexpectedly empty.
6. Keep legacy behavior unchanged.
   - The legacy build does not render this Compose cassette UI, so avoid adding legacy-specific work unless shared model compilation requires it.

## Test plan

Add tests before or alongside implementation:

1. Unit/model tests
   - `MixtapeVisualProperties` carries a default symbol color.
   - SharedPreferences-backed store defaults old records to the default color and saves/loads non-default colors.
2. ViewModel tests
   - When a tape is selected and playing, `cycleCurrentMixtapeSymbolColor()` changes `currentMixtapeVisualProperties.symbolColor` and persists it.
   - When paused/stopped, `cycleCurrentMixtapeSymbolColor()` does not change the color.
   - `selectCurrentMixtapeEmbellishment()` changes the current tape symbol only when the requested symbol is enabled.
   - Changing symbol/color does not reset `currentTrack`, `currentIndex`, queue, or playback state.
3. Modern UI contract/instrumented tests
   - Now Playing exposes `Current mixtape symbol` semantics.
   - Tapping the symbol invokes the color-cycle callback only in playing state.
   - Long-pressing the symbol opens `Choose mixtape symbol` and selecting an option invokes the symbol-selection callback.
   - The symbol remains before the cassette name and keeps the existing 96.dp size.
4. Regression commands

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```

If a compatible emulator/device is available, also run `connectedModernDebugAndroidTest` or manually verify with seeded audio.

## Manual acceptance checklist

1. Open the modern app with at least one audio file.
2. Select a mixtape so Now Playing appears.
3. Start playback.
4. Tap the large symbol on the cassette label; its color changes while the same track keeps playing.
5. Pause/stop playback and tap the symbol; color does not change.
6. Long-press the symbol; a selector opens.
7. Pick a different symbol; the cassette label updates and remains on the same tape/track.
8. Navigate away and back to the same mixtape; chosen symbol and color are retained.
9. Global symbol settings still enforce enabled symbols and do not allow selecting disabled symbols.

## Creative directions / prompt ideas

- Keep the color palette looking like hand-drawn marker ink on a cassette label, not bright Material primary colors.
- Selector copy: `Choose mixtape symbol`, `Marker ink`, `Tap while playing to change ink color`.
- Future polish prompt: “Make the symbol briefly pulse or wiggle when its color changes, while preserving cassette label readability.”

## Search terms / references for implementation

- `Jetpack Compose combinedClickable Canvas longClick`
- `Compose semantics contentDescription Canvas clickable`
- `Compose DropdownMenu long press selector`
- `Compose AlertDialog selectable list enum`
- `Android SharedPreferences enum migration default value`

## Risks and guardrails

- `Canvas` alone has no text semantics; add explicit semantics to the clickable wrapper for testability/accessibility.
- A 96.dp clickable symbol is large; ensure taps do not steal interactions from transport controls or cassette title text.
- Avoid transient-only UI state for color/symbol changes; this feature should survive recomposition and navigation.
- Do not let global settings leave the selector empty; use the normalized enabled set.
- Keep hard-coded color mapping out of shared JVM model code if it would force Compose UI dependencies into main/viewmodel tests.
- Do not change the track-list long-press actions or the global Mixtape symbol settings screen behavior.
