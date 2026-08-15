# Card 2234 — Now Playing eject button plan

## Request

Add an eject button to the modern now-playing transport controls. Pressing eject must stop the currently playing track and return the user to the cassette list page. The now-playing tape page should no longer show the navigation bar or the `Now Playing` title; navigation back is via eject.

## Existing touch points

- UI: `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `NowPlaying(...)` currently renders `NowPlayingHeader(...)`, `Text("Now Playing")`, `CassetteTape`, `WalkmanTransportControls`, and `CassetteCoverTrackList` in both portrait and landscape branches.
  - `NowPlayingHeader` is the current nav bar (`Mix Tapes`, `Settings`, queued count).
  - `WalkmanTransportControls(...)` currently exposes Rewind, Play, Pause, Fast-forward, and Stop.
- View model: `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt`
  - `stop()` calls `controller.stop()` and keeps the current screen.
  - `backToMixTapes()` changes the screen to `MixtapeScreen.MixTapes` without stopping playback.
- Activity wiring: `app/src/modern/java/com/example/androidmixtape/MainActivity.kt`
  - Passes `viewModel::stop` as `onStop` and `viewModel::backToMixTapes` as `onBackToMixTapes`.
- Existing tests to preserve/extend:
  - `app/src/androidTestModern/java/com/example/androidmixtape/ui/MixtapeAppTest.kt`
  - `app/src/test/java/com/example/androidmixtape/viewmodel/MixtapeViewModelTest.kt`
  - UI source contract tests under `app/src/test/java/com/example/androidmixtape/ui/`.

## Implementation plan

1. Add an explicit `eject()` action to `MixtapeViewModel`:
   - Call `controller.stop()` first so the current track is paused/reset.
   - Then set state from `controller.toUiState(status = Ready, screen = MixtapeScreen.MixTapes, message = "${mix tape count} mix tapes ready")`.
   - Keep `backToMixTapes()` for settings/back flows unless the implementation later decides to route it through `eject()` only from now-playing.

2. Thread a new UI callback:
   - Add `onEject: () -> Unit = {}` to `MixtapeApp`.
   - Pass `onEject` into `NowPlaying(...)`.
   - Pass it into `WalkmanTransportControls(...)`.
   - In modern `MainActivity`, wire `onEject = viewModel::eject`.

3. Add the transport button:
   - In `WalkmanTransportControls`, add a `WalkmanButton(label = "⏏", description = "Eject", enabled = hasTrack, onClick = onEject)`.
   - Keep the existing Stop button intact unless product direction changes; stop remains a local transport action, eject is the navigation action.
   - If six buttons feel tight, keep the current 34dp width but verify on the small 320dp Compose test container; no layout overflow should occur.

4. Remove now-playing nav/title chrome:
   - Delete the `NowPlayingHeader(...)` call from both portrait and landscape branches of `NowPlaying`.
   - Delete `Text("Now Playing", ...)` from both branches.
   - Remove `onBackToMixTapes`/`onShowSettings` parameters from `NowPlaying` if no longer used there, but keep app-level callbacks for `SettingsScreen` and `MixTapeLibrary`.
   - Delete `NowPlayingHeader(...)` composable if it becomes unused.

## Test plan

- ViewModel unit test:
  - Given a loaded library and selected mixtape, call `eject()`.
  - Assert `screen == MixtapeScreen.MixTapes`, `isPlaying == false`, and `positionMs == 0L`.
- Modern Compose test:
  - In `nowPlayingRendersCassetteWalkmanTransportAndCompleteJCardTrackList`, add an eject counter and pass `onEject`.
  - Assert node with content description `Eject` is displayed and click increments the counter.
  - Assert the old `Mix Tapes`/`Settings` nav controls and `Now Playing` title are not displayed on the now-playing screen.
- Optional source contract test:
  - Add a small `NowPlayingEjectContractTest` that checks the NowPlaying source contains `description = "Eject"`/`label = "⏏"` and does not contain `NowPlayingHeader(` or `Text("Now Playing"` in the NowPlaying body.
- Regression command:
  - `./gradlew testModernDebugUnitTest`
  - If an Android device/emulator is available, also run `./gradlew connectedModernDebugAndroidTest`.

## Success criteria

- Now-playing screen has cassette art, track list, and Walkman transport controls only; no top nav bar and no `Now Playing` title.
- The transport controls include an accessible `Eject` button.
- Pressing eject stops playback and returns to the cassette list (`MixtapeScreen.MixTapes`).
- Existing Stop behavior remains available and still does not navigate.
- Modern unit/UI tests cover the new behavior.

## Risks / notes

- Existing worktree has unrelated in-progress changes; implementation should avoid overwriting them and should inspect `git diff` before editing shared files.
- `controller.stop()` is a pause-and-seek-to-zero operation and leaves the selected track in state. That should satisfy “track should stop” while avoiding destructive queue unloads; if product wants a visually unloaded cassette, add that as a separate follow-up.
- The UI currently has separate `onBackToMixTapes` and `onStop` callbacks; introducing `onEject` is clearer than overloading either existing action.
