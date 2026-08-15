# Card 2373 — Preserve playback and mixtape font when deleting a device track

## Bug statement

When `Delete from device` succeeds, the currently playing track stops and playback restarts from the beginning of the mixtape. The open mixtape's handwriting/font also changes. Deleting a non-current track should not interrupt the current song or change the visible mixtape identity.

## Current repo facts

- `MixtapeViewModel.deleteTrackFromDevice(...)` and `confirmTrackDeletedFromDevice()` both call `removeDeletedTrackFromAppState(...)` after confirmed deletion.
- The first implementation pass added `MixtapeController.replaceQueuePreservingCurrentTrack(...)` and uses it from delete cleanup, but review found a production gap: `PlayerEngine.replacePlaylistPreservingPlayback(...)` is still a default no-op. The UI/controller state can look fixed while real playback engines keep stale media queues.
- Production engine files to update explicitly: `app/src/modern/java/com/example/androidmixtape/playback/ExoPlayerEngine.kt`, `app/src/modern/java/com/example/androidmixtape/playback/MediaControllerPlayerEngine.kt`, and `app/src/legacy/java/com/example/androidmixtape/playback/PlatformMediaPlayerEngine.kt`.
- `removeDeletedTrackFromAppState(...)` filters the deleted track and rebuilds assigned groups. Any replacement path must update both the controller `PlayerUiState` and the underlying engine queue so next/previous/autoplay cannot later play the deleted item or an index-shifted wrong item.
- The current mixtape visual key is built by `MixTapeGroup.stableMixtapeKey()` from `startIndex` plus all track IDs. Deleting a track changes that key, so `visualPropertiesFor(newKey)` creates or loads a different `MixtapeVisualProperties`, including a potentially different `handwritingFont`.
- A previous delete-device card exists at `docs/CARD_2337_DELETE_FROM_DEVICE_BUG_PLAN.md`; do not regress its scoped-storage delete flow or lint compatibility expectations.

## Recommended implementation plan

1. Add failing tests first.
   - In `MixtapeViewModelTest`, cover: start playback on track B, seek to a non-zero position, delete a different track from the open queue, and assert the current track ID, `isPlaying`, and position are preserved while the deleted track disappears from library/groups/queue.
   - Add the same scenario for the pending Android user-action path: call `deleteTrackFromDevice(...)`, then `confirmTrackDeletedFromDevice()`, and assert continuity only after confirmation.
   - Add a visual regression assertion: capture `currentMixtapeVisualProperties.handwritingFont` (ideally the whole visual-properties object) before deletion and assert it is unchanged after a non-current deletion from the open mixtape.
   - Keep existing behavior for deleting the currently playing track explicit: it cannot keep playing a file that was deleted; choose a valid nearest remaining track or stop with a valid empty/queue state.
2. Fix playback preservation all the way through the production `PlayerEngine` implementations, not just controller/view-model state.
   - Keep or refine `MixtapeController.replaceQueuePreservingCurrentTrack(...)`: if the current track still exists in `newTracks`, update `PlayerUiState.tracks`, remap `currentIndex` to that same track ID/URI, preserve `isPlaying`, `positionMs`, and `durationMs`, and avoid `select(0)`/`playIndex(0)`.
   - Make `PlayerEngine.replacePlaylistPreservingPlayback(tracks, currentIndex)` a real contract, not a silent default no-op. Prefer removing the default body so every engine must implement it, or add contract tests that fail if an engine relies on the default.
   - `ExoPlayerEngine`: replace the Media3 playlist with `setMediaItems(mediaItems, currentIndex, currentPosition)` (or equivalent remove/move APIs) and call `prepare()` only as needed, preserving `player.currentPosition`, `playWhenReady`, and the remapped current media item. Ensure media item IDs identify tracks (`track:${id}` or URI fallback) so remapping is deterministic.
   - `MediaControllerPlayerEngine`: perform the same queue replacement through the connected `MediaController`, preserving `currentPosition` and `playWhenReady`. If the controller is not yet connected, queue a replacement action that does not later run an old `loadPlaylist` with stale tracks ahead of the preserving action.
   - `PlatformMediaPlayerEngine`: update its backing `tracks` list and `currentIndex` to the remapped index without releasing/recreating the active `MediaPlayer` when the current track survived. Keep the current player instance and position; only release/reload if the current media item was deleted or no valid current item remains.
   - If the current track was deleted, handle intentionally: remove it from the queue and select the nearest valid replacement only if product wants that, otherwise stop with a valid state and message.
3. Use the new preservation path from deletion cleanup.
   - In `removeDeletedTrackFromAppState(...)`, capture the pre-delete current track ID, index, playing flag, position, and visual properties before filtering/rebuilding.
   - After calculating the continuation `openGroup`, call the preservation method when the previous current track remains in the new queue. Do not call `controller.load(queueTracks)` unconditionally.
4. Preserve mixtape visual identity across membership shrink.
   - When the selected/open tape survives deletion with only membership changed, migrate/copy the previous `currentMixtapeVisualProperties` to the new stable key before `toUiState(...)` resolves group visuals, or introduce a stable tape identity that is not invalidated by deleting one track.
   - Preserve at least `handwritingFont`, `baseJitterKey`, `embellishment`, `symbolColor`, and `tapeSkin`; this avoids fixing font while accidentally changing other cassette visuals.
5. Watch secondary rebuild paths.
   - `applyExclusionSettingsAndRefresh(...)` has similar `controller.load(...)` and stable-key churn. This card is about delete, but if helper methods are reusable, route exclusion rebuilds through the safer path without broad behavior changes.

## Additional tests required after review failure

- Add/extend `MixtapeControllerTest` with a fake engine that records `replacePlaylistPreservingPlayback(...)` calls and asserts the fake queue is actually replaced, the deleted track is gone, and the remapped index points at the same current track.
- Add source/contract tests in `PlaybackContinuityContractTest` (or dedicated engine contract tests if practical under JVM unit tests) proving all three production engines override/implement `replacePlaylistPreservingPlayback`: `ExoPlayerEngine`, `MediaControllerPlayerEngine`, and `PlatformMediaPlayerEngine`.
- Contract assertions should look for the real playlist replacement APIs/behaviors, not just method names: Media3/MediaController should preserve current position when calling `setMediaItems`/playlist update; legacy should assign the new `tracks` list and remapped `currentIndex` without unconditional `player?.release()` in the preserve path.
- Add a regression assertion that after deleting a non-current queued item, a subsequent `next()`/automatic completion cannot target the deleted item or the old stale index.

## Success criteria

- Deleting a non-current track from the device removes it from `tracks`, all `mixTapeGroups`, the open `queueTracks`, and every production engine playlist/backing queue without restarting playback.
- The same current track remains selected and playing after delete confirmation; its position is preserved as closely as the engine API allows and must not reset to `0` just because the queue was rebuilt.
- The open mixtape's handwriting font and full visual identity remain unchanged after the delete.
- Deleting the actual currently playing track has a deliberate, tested behavior with a valid queue/current-index state.
- Existing delete-device behavior from card 2337 still works: direct success, Android user-action confirmation, cancel/failure paths, and legacy/modern lint compatibility.

## Suggested verification

```sh
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew lintModernDebug lintLegacyDebug
```

Manual smoke on Android 11+ if available:

1. Start playback on track 2 and let it play for a few seconds.
2. Long-press/delete a different queued track and confirm Android's delete prompt.
3. Verify track 2 continues without audible restart, the progress does not jump to the start, the deleted song disappears, and the cassette/mixtape font does not change.
