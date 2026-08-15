# Card 2496 — Tracklist double-click jump with tape cue

## Requirement

Double-clicking a track row in the currently playing Now Playing track list should:

1. Stop the currently playing track immediately.
2. Play a 5 second tape transport cue:
   - fast-forward cue when the target row is after the current row.
   - rewind cue when the target row is before the current row.
   - no direction cue, or a very short stop/start path, when target equals current row.
3. Start playback of the double-clicked track from the beginning after the cue completes.

## Current repo facts

- Modern Now Playing renders both portrait and landscape track lists through `CassetteCoverTrackList(...)` in `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`.
- `CassetteCoverTrackList` already uses `Modifier.combinedClickable(...)` for each row, but `onClick = {}` and only `onLongClick` is wired for the actions menu.
- `NowPlaying(...)` currently passes delete/remove/info callbacks to the track list, but no row play/jump callback.
- Playback state lives in shared `MixtapeController` (`app/src/main/java/com/example/androidmixtape/playback/MixtapeController.kt`). Existing `select(index)` immediately calls `player.playIndex(index)` and starts the selected track.
- `stop()` already pauses and seeks the current track to zero.
- Modern playback uses `ExoPlayerEngine`; legacy/API 19 uses `PlatformMediaPlayerEngine`. Any cue implementation must not introduce Media3 dependencies into the legacy/shared source set.

## Recommended implementation plan

1. Add regression coverage first.
   - Add a focused controller/ViewModel test for a jump from current index 1 to 4: verify pause/seek-to-zero happens before any target `playIndex(4)`, the transition direction is fast-forward, and target playback starts only after the 5,000 ms cue completes.
   - Add the mirror test for target index before current index using rewind direction.
   - Add a same-index test: restart the current track without a rewind/fast-forward direction cue, or explicitly document any chosen same-row behavior.
   - Add a UI source/contract test ensuring `CassetteCoverTrackList` accepts an `onTrackDoubleClick`/`onTrackJumpRequested` callback and uses `combinedClickable(onDoubleClick = ...)`, while preserving `onLongClick` for the existing actions menu.
2. Thread the UI callback.
   - Extend `NowPlaying(...)` and both portrait/landscape `CassetteCoverTrackList(...)` calls with a track/index double-click callback.
   - In the row loop, set `combinedClickable(onClick = {}, onDoubleClick = { onTrackDoubleClick(index, track) }, onLongClick = { expandedTrackId = track.id })`.
   - Keep normal single taps inert so the existing long-press action menu remains discoverable and no accidental playback jump occurs.
3. Add an async transition path in ViewModel/controller.
   - Prefer a single public method such as `MixtapeViewModel.jumpToTrackWithCue(index: Int)` or `MixtapeController.jumpToTrackWithCue(index, cuePlayer, scope)` so all callers share ordering and cancellation behavior.
   - Cancel any in-flight jump job when a new double-click arrives.
   - Snapshot `currentIndex` before stopping to choose direction.
   - Call `controller.stop()` (or equivalent pause + seek zero) immediately, update UI message to something like `Fast-forwarding to <title>…` / `Rewinding to <title>…`, play/wait the 5,000 ms cue, then call `controller.select(targetIndex)` if the queue and target track are still valid.
   - Revalidate by track identity/id after the delay so deletes/eject/mixtape changes during the cue do not start the wrong row.
4. Implement the cue without breaking variants.
   - Define a small shared abstraction, e.g. `TransportCuePlayer` with `suspend fun play(direction: TransportCueDirection, durationMs: Long)` and `fun cancel()`/`release()` if needed.
   - Modern implementation can use a small bundled raw resource, a lightweight generated AudioTrack/tone/scrub loop, or a dedicated ExoPlayer instance. Do not interrupt the main music player after it has been stopped.
   - Legacy implementation must be API-19-safe: avoid Media3; use `MediaPlayer` with a raw resource or generated `AudioTrack`/`ToneGenerator`.
   - Add raw resources only if licensing is clear. A generated cue is safer than importing external sound assets.
5. Lifecycle and interruption handling.
   - Stop/cancel cue playback on eject, ViewModel `onCleared`, controller release, and when the app leaves Now Playing if that should abort the transition.
   - If the target is no longer in `queueTracks` after the cue, do not play anything; leave playback stopped and show a clear message.
   - Keep transport buttons usable: pressing Stop/Eject during the cue should cancel the jump.
6. Verification.
   - Run focused tests plus related playback/ViewModel/UI source contracts.
   - Run `testModernDebugUnitTest` and `testLegacyDebugUnitTest` after implementation.
   - Per project policy, build/install an x86/x86_64-compatible modern APK to Kunlun `emulator-5554`, open Now Playing with a multi-track mixtape, start a track, double-click a later row and then an earlier row, and capture screenshots/log notes showing the old track stopped, the appropriate 5s cue occurred, and the target row becomes current/red after the cue.

## Success criteria

- Double-clicking a later visible track stops current playback, plays a 5 second fast-forward cue, then starts the clicked track from 0:00.
- Double-clicking an earlier visible track stops current playback, plays a 5 second rewind cue, then starts the clicked track from 0:00.
- Same-row double-click behavior is deterministic and tested.
- Normal single-click remains inert; long-press action menu still works for delete/remove/info.
- Repeated double-clicks, Stop, and Eject cancel any pending cue safely and do not start stale tracks.
- No Media3/ExoPlayer dependency leaks into shared or legacy API-19 code.
- Modern and legacy unit gates pass, and emulator evidence is captured after implementation.

## Useful implementation search terms

- `combinedClickable onDoubleClick Compose`
- `Android AudioTrack generated rewind fast forward sound effect Kotlin`
- `MediaPlayer raw resource API 19 short sound effect`
- `kotlinx.coroutines TestScope advanceTimeBy ViewModel test`
- `ExoPlayer separate sound effect player short raw resource`
