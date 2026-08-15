# Card 2432 — Now Playing mixtape-list toggle plan

## Request

While a mixtape is playing, the user should be able to switch the now-playing body between:

1. the current cassette cover/track list, and
2. the full mixtape list.

When the full mixtape list is opened from Now Playing, it should initially position the current mixtape in the center of the visible list/grid. If the user browses that list and then presses Eject to return to the Mix Tapes screen, the Mix Tapes screen must preserve the browsed scroll position so the user can tap the mixtape they found without scrolling back again.

## Current implementation notes

- Main UI target: `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `MixtapeApp` already owns stable `mixTapeListState = rememberLazyListState()` and `mixTapeGridState = rememberLazyGridState()`.
  - `MixTapeLibrary(...)` already accepts those hoisted states and uses them for portrait `LazyColumn` and landscape `LazyVerticalGrid`.
  - `NowPlaying(...)` currently always shows cassette art, Walkman controls, and `CassetteCoverTrackList`.
  - `onMixTapeGroupClick` is already threaded at the app level and can be reused when a mixtape is selected from the Now Playing mixtape list.
- View model target: `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt`
  - `currentMixtapeStableKey` is tracked internally and exposed on `MixtapeUiState`, but `MixTapeGroup.stableMixtapeKey()` is private to the ViewModel, so the UI should not recompute stable keys.
  - Prefer exposing `currentMixtapeIndex: Int = -1` on `MixtapeUiState` from `toUiState()` for centering/highlighting.
- Existing source contract coverage:
  - `MixTapeScrollStateContractTest` already protects the hoisted list/grid states and their use by `MixTapeLibrary`.
  - `NowPlayingEjectContractTest` protects the eject callback and removed Now Playing nav chrome.
  - `MixTapeLandscapeTwoColumnContractTest` protects the landscape two-column mixtape list.

## Implementation plan

1. Add current-mixtape index to state:
   - Add `val currentMixtapeIndex: Int = -1` to `MixtapeUiState`.
   - In `MixtapeController.toUiState(...)`, compute `selectedGroupIndex` with `assignedMixTapeGroups.indexOfFirst { it.stableMixtapeKey() == selectedStableKey }`.
   - Continue using `selectedGroup` for `currentMixtapeName`/visual properties, but also populate `currentMixtapeIndex = selectedGroupIndex`.
   - Add or update ViewModel unit coverage that selecting mixtape index N results in `currentMixtapeIndex == N`.

2. Split the reusable mixtape list body from the library header:
   - Keep `MixTapeLibrary(...)` as the public screen-level composable with the Settings/header row.
   - Extract the cassette briefcase list/grid to a private reusable composable such as `MixTapeBriefcaseList(...)` with parameters:
     - `groups`
     - `mixTapeListState`
     - `mixTapeGridState`
     - `onMixTapeGroupClick`
     - optional `currentMixtapeIndex: Int = -1`
     - optional `modifier`
   - Have `MixTapeLibrary(...)` call `MixTapeBriefcaseList(...)` below the header.
   - Now Playing can call the same briefcase list without the Settings row, guaranteeing shared scroll state and consistent portrait/landscape behavior.

3. Add a Now Playing view toggle:
   - Introduce a small local mode enum/private state, e.g. `NowPlayingBodyMode.Tracks` vs `NowPlayingBodyMode.MixTapes`.
   - Use `rememberSaveable { mutableStateOf(NowPlayingBodyMode.Tracks) }` in `MixtapeApp` or inside `NowPlaying` so rotation/configuration changes do not drop the selected mode unnecessarily.
   - Render two accessible controls near the transport controls: `Tracks` / `Mix tapes`, with content descriptions like `Show current track list` and `Show mix tape list`.
   - Default mode should be `Tracks` when entering Now Playing.
   - The toggle should not stop playback and should not navigate away from `MixtapeScreen.NowPlaying`.

4. Render mixtape list inside Now Playing:
   - When mode is `Tracks`, keep the existing `CassetteCoverTrackList(...)` layout in both portrait and landscape.
   - When mode is `MixTapes`, replace that pane with `MixTapeBriefcaseList(...)` using the same `mixTapeListState`/`mixTapeGridState` owned by `MixtapeApp`.
   - Reuse `onMixTapeGroupClick(index)` so selecting a mixtape from this list starts that mixtape with existing ViewModel behavior.
   - Add a visual/semantic highlight for `currentMixtapeIndex` in `CassetteSpineRow`, for example a warmer border/background and semantics text/content description containing `Current mixtape`. Keep non-current rows unchanged.

5. Center the current mixtape on first Now Playing mixtape-list exposure:
   - When the user switches from `Tracks` to `Mix tapes`, run a one-shot `LaunchedEffect` keyed by the current selected mixtape identity/index.
   - If `state.currentMixtapeIndex >= 0`, scroll the shared state so the selected mixtape is near the center:
     - portrait list: `mixTapeListState.animateScrollToItem((currentIndex - visibleListPadding).coerceAtLeast(0))`
     - landscape grid: scroll to the row containing the current index, e.g. `(currentIndex / 2 - visibleRowPadding).coerceAtLeast(0)` if using `LazyVerticalGrid(GridCells.Fixed(2))`.
   - Guard with a remembered key such as `lastCenteredMixtapeIndex`/`lastCenteredMixtapeStableKey` so recomposition does not keep yanking the list back after the user scrolls.
   - Reset that guard only when a different mixtape is selected or the user re-enters Now Playing for another tape.

6. Preserve scroll position through eject:
   - Do not create fresh lazy states inside Now Playing.
   - Keep using the same app-owned `mixTapeListState` and `mixTapeGridState` for both the Now Playing mixtape-list pane and the Mix Tapes screen.
   - `viewModel.eject()` can remain a screen change to `MixtapeScreen.MixTapes`; because the same states are hoisted above the screen switch, the user’s last browsed position should remain intact.

## Test plan for the Testing stage

Add failing tests before implementation where practical:

- ViewModel unit test:
  - Build enough tracks for multiple mixtapes.
  - Select a non-zero mixtape index.
  - Assert `uiState.currentMixtapeIndex` matches the selected index and remains valid after `eject()`.
- Source contract test for toggle wiring:
  - `NowPlaying` or `MixtapeApp` defines a track-list vs mixtape-list mode.
  - UI includes labels/content descriptions for `Tracks` and `Mix tapes`.
  - `NowPlaying` conditionally renders both `CassetteCoverTrackList(` and `MixTapeBriefcaseList(`.
- Source contract test for scroll sharing/centering:
  - Now Playing receives or can access the app-owned `mixTapeListState` and `mixTapeGridState`.
  - The same state names are passed to both `MixTapeLibrary` and the Now Playing mixtape-list body.
  - Source contains `animateScrollToItem` or `scrollToItem` driven by `currentMixtapeIndex`.
- Optional modern Compose UI test:
  - Render Now Playing with several mixtape groups.
  - Assert the `Mix tapes` toggle appears.
  - Click it and assert cassette spine rows appear in the Now Playing screen.
  - If feasible, assert the current mixtape row has `Current mixtape` semantics.

## Verification commands after implementation

```sh
cd /home/puzzleduck/x/android-mixtape
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
```

If a compatible Android device/emulator is available:

```sh
./gradlew connectedModernDebugAndroidTest
```

Manual smoke path:

1. Seed enough audio to create at least 8 mixtapes.
2. Open a middle mixtape and start playback.
3. Tap the Now Playing `Mix tapes` toggle.
4. Confirm the current mixtape is visible near the center and marked as current.
5. Scroll to another mixtape but do not tap it.
6. Press Eject.
7. Confirm the Mix Tapes screen opens at the same scrolled location.
8. Tap the found mixtape and confirm it starts/opens normally.

## Success criteria

- Now Playing has an accessible toggle between current track list and full mixtape list.
- Playback continues while switching between the two panes.
- The initial Now Playing mixtape-list view centers or near-centers the current mixtape.
- The current mixtape is visibly and semantically identifiable in the list.
- If the user scrolls the Now Playing mixtape list and then ejects, the Mix Tapes screen preserves that exact browsed scroll area.
- Selecting a mixtape from the Now Playing mixtape list opens/plays that mixtape using existing selection behavior.
- Portrait and landscape list/grid behavior both work and preserve the existing two-column landscape design.
- Existing eject, transport controls, track long-press actions, and cassette visuals continue to pass tests.

## Risks / guardrails

- Do not recenter on every recomposition; it would fight user scrolling.
- Do not duplicate independent list/grid state in Now Playing; that would break eject preservation.
- Do not expose or recompute the private stable-key algorithm in UI code; expose a simple current index from the ViewModel.
- Keep this in modern Compose UI/ViewModel state unless tests reveal a shared playback change is needed.
- Watch existing uncommitted work in this repository; inspect `git status`/`git diff` before implementation and avoid overwriting unrelated changes.
