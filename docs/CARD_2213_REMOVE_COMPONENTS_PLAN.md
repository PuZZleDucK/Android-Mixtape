# Card 2213 plan: remove library view and extra titles

## Request

Remove the library view page. Remove the two titles above the navbar.

## Current source references

- Modern Compose UI: `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `Scaffold(topBar = { TopAppBar(title = { Text("Mixtape") }) })` renders the top app title.
  - The top-level `Text(text = state.message, ...)` renders the second status/title line above each screen's navigation row.
  - `TrackLibrary(...)` and `MixtapeScreen.Library` are the modern library page path.
  - `MixTapeLibrary`, `SettingsScreen`, and `NowPlaying` include `Library` buttons that navigate back to the library page.
- Modern view model: `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt`
  - `MixtapeScreen.Library`, `backToLibrary()`, and `selectTrack()` are the library-screen state/actions.
  - `refresh()` already opens `MixtapeScreen.MixTapes`, so the app can remain mixtape-first.
- Activity wiring is in modern `MainActivity.kt`; remove or retarget `onBackToLibrary`/library callbacks there after changing the composable API.
- Legacy plain-Views UI: `app/src/legacy/java/com/example/androidmixtape/MainActivity.kt` is library-list based. Treat legacy separately: do not risk API-19 playback unless product scope explicitly requires parity; at minimum ensure the legacy build still compiles.

## Implementation plan

1. Modern UI header cleanup
   - Remove the `TopAppBar` import/usage and the top-level `state.message` banner from `MixtapeApp` so no title/status text appears above the screen nav row.
   - Keep error/empty/permission messaging inside the relevant state-specific content so users still understand blocked states.

2. Remove the modern library page as a reachable screen
   - Delete `MixtapeScreen.Library` usage from `MixtapeApp` and remove the `TrackLibrary` composable if it becomes unused.
   - Remove `Library` buttons from `MixTapeLibrary`, `SettingsScreen`, and `NowPlaying`, or retarget any necessary back action to `Mix Tapes`.
   - Keep `tracks` and `libraryTracks` internally so mixtape grouping and playback still work.
   - Prefer leaving `selectTrack()` only if tests or non-UI code still need it; otherwise remove it and update tests.

3. ViewModel contract updates
   - Remove `MixtapeScreen.Library` enum value and `backToLibrary()` if no remaining caller uses them.
   - Ensure permission grant/refresh still lands on `MixtapeScreen.MixTapes`.
   - Ensure now-playing and settings navigation returns to `MixTapes` only.

4. Tests/contracts
   - Update or replace tests that assert the library route exists, especially `MixTapeQueueViewModelTest.selectingTrackFromMainLibraryStillUsesFullLibraryQueue`.
   - Add a contract test that `MixtapeScreen` has no `Library` entry or that source no longer contains `TrackLibrary(` / visible `Text("Library")` nav buttons.
   - Add/adjust UI source contract to assert there is no `TopAppBar(title = { Text("Mixtape") })` and no top-level `Text(text = state.message...)` banner.

5. Verification
   - Run `./gradlew test` from `/home/puzzleduck/x/android-mixtape`.
   - If time permits, run `./gradlew assembleModernDebug assembleLegacyDebug` to catch variant compile regressions.
   - Optional visual smoke on an available emulator/device: fresh launch after permission should show the mixtape list first with no extra title/status text above the nav controls; settings and now-playing should have no Library navigation.

## Success criteria

- The modern app has no reachable library page and no visible `Library` navigation button.
- The app still loads local tracks, builds mixtape groups, opens mixtapes, plays tracks, and supports settings/reset/grouping controls.
- The two header/title lines above the main navigation area are gone.
- Existing relevant unit/contract tests pass after updates, and both modern/legacy variants still compile or any intentional legacy limitation is documented.

## Risks / watch-outs

- Removing `state.message` globally can hide permission, empty-library, or error guidance; preserve those messages inside the affected content states.
- Tests may encode the previous library behavior; update them to the new product decision rather than deleting coverage blindly.
- Legacy UI is a separate API-19 implementation and may not have the same mixtape screens; avoid a large legacy redesign unless the running stage confirms it is required by this card.
