# Card 2337 — Delete from device bug plan

## Review rework addendum — 2026-07-03

The first implementation was functionally close but failed Review because Android lint rejects the scoped-storage recovery path in shared source. Current failure target is `app/src/main/java/com/example/androidmixtape/data/AudioRepository.kt` lines 102-103: `catch (error: RecoverableSecurityException)` and `error.userAction.actionIntent.intentSender` reference API 29 APIs from code compiled for the legacy `minSdk 19` variant without an API guard.

Actionable rework plan:

1. Replace the direct `catch (error: RecoverableSecurityException)` clause with a `catch (error: SecurityException)` path that only inspects/casts to `RecoverableSecurityException` after `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q`.
2. Move API-29-only recovery extraction into a small helper annotated or guarded for API 29, for example `@RequiresApi(Build.VERSION_CODES.Q) private fun recoverableDeleteResult(error: RecoverableSecurityException): DeleteTrackResult`.
3. Keep the API 30+ `MediaStore.createDeleteRequest(...)` branch behind the existing `Build.VERSION.SDK_INT >= Build.VERSION_CODES.R` guard; do not paper over this with broad `@SuppressLint("NewApi")` unless the guarded code still produces a demonstrably false-positive lint warning.
4. Add or update Testing coverage so this cannot regress: the existing behavior/source tests proved the feature but missed the lint compatibility issue. A useful source contract is that `AudioRepository.kt` must not contain a direct `catch (...RecoverableSecurityException...)` in minSdk-shared code and must keep the recovery extraction behind `Build.VERSION_CODES.Q`/`@RequiresApi`.
5. Running must rerun both unit and lint gates before Review: `./gradlew testModernDebugUnitTest testLegacyDebugUnitTest lintModernDebug lintLegacyDebug`. If APK/dist artifacts change during verification, commit or intentionally revert them before moving forward.

Rework success criteria:

- `lintModernDebug` and `lintLegacyDebug` pass with no `NewApi` errors for `RecoverableSecurityException`, `userAction`, or `actionIntent`.
- The delete flow still returns `RequiresUserAction(IntentSender)` for API 30+ delete requests and API 29 recoverable security exceptions.
- Direct pre-scoped-storage delete success/failure behavior remains unchanged for legacy devices.
- Existing ViewModel pending-delete finalization still removes confirmed deleted tracks from `tracks`, `mixTapeGroups`, and `queueTracks`, and cancel/failure paths still leave state unchanged.

## Bug statement

`Delete from device` currently does not remove the audio file from device storage and does not remove the track from the visible track list.

## Current repo facts

- Modern long-press menu is implemented in `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt` inside `CassetteCoverTrackList(...)`.
- The menu opens a confirmation dialog and calls `viewModel.deleteTrackFromDevice(track.id)` from `app/src/modern/java/com/example/androidmixtape/MainActivity.kt`.
- Shared delete state lives in `MixtapeViewModel.deleteTrackFromDevice(...)` and the new pending-delete finalization methods in `app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt`.
- `DeleteTrackResult` now includes `RequiresUserAction(IntentSender)`, and modern `MainActivity` registers `ActivityResultContracts.StartIntentSenderForResult()` to call `confirmTrackDeletedFromDevice()` on `RESULT_OK` or cancel otherwise.
- `MediaStoreAudioRepository.deleteTrack(...)` now uses `MediaStore.createDeleteRequest(...)` for API 30+ and tries to convert API 29 `RecoverableSecurityException.userAction` into the same user-action result.
- Review found the remaining blocker: the API 29 recovery is written as a direct `catch (error: RecoverableSecurityException)` in shared minSdk 19 source, causing Android lint `NewApi` failures even though unit tests pass.
- Existing tests include behavior/source coverage for the feature, but they did not catch the lint compatibility issue. The next Testing pass should add a contract or make lint failure part of the recorded red evidence before Running fixes it.

## Recommended fix direction

Implement a real platform-mediated delete flow and refresh in-memory state only after confirmed deletion.

1. Expand the delete result contract.
   - Add a result such as `DeleteTrackResult.RequiresUserAction`/`NeedsConfirmation` that carries enough information for modern `MainActivity` to launch an `IntentSender`.
   - Keep `Failure(message)` for unsupported/invalid cases and `Success` for direct deletes.
2. Update `MediaStoreAudioRepository.deleteTrack(...)`.
   - For API 30+, use `MediaStore.createDeleteRequest(resolver, listOf(Uri.parse(track.uri))).intentSender` instead of relying only on direct `resolver.delete(...)`.
   - For API 29, catch `RecoverableSecurityException` and expose `error.userAction.actionIntent.intentSender`.
   - For older APIs, keep direct `ContentResolver.delete(...)` and report failure if zero rows are deleted.
   - Avoid treating a permission/recovery requirement as a hard failure when the Activity can resolve it.
3. Add Activity/result handling in the modern app.
   - Register an `ActivityResultContracts.StartIntentSenderForResult()` launcher.
   - When delete needs user action, save the pending track id/URI in the ViewModel or Activity, launch the intent sender, and on `RESULT_OK` finalize by refreshing/deleting the track from app state.
   - On cancel/failure, leave the track list unchanged and show a clear message.
4. Add a ViewModel finalization path.
   - Add a method such as `confirmTrackDeletedFromDevice(trackId)` or `refreshAfterDeviceDelete(trackId)` that reloads MediaStore and removes/rebuilds local tape state only when the track is gone.
   - Ensure the currently open tape and controller queue are rebuilt so the deleted track disappears from `state.tracks`, `state.mixTapeGroups`, and `state.queueTracks`.
   - If the deleted track was playing, stop or select a valid replacement track; do not leave playback pointing at a missing URI.
5. Keep confirmation UX clear.
   - Existing Compose confirmation is useful, but Android's scoped-storage prompt may appear after it; message copy should make that double confirmation understandable.

## Testing plan

- Unit tests in `MixtapeViewModelTest` or a new `TrackDeleteViewModelTest`:
  - direct delete success removes the track from `tracks`, all mixtape groups, and the current queue.
  - direct delete failure leaves all state unchanged and reports `Could not delete ...`.
  - pending/user-action delete leaves state unchanged until a successful finalization callback.
  - deleting the currently playing track resets/stabilizes controller state without an invalid current index.
- Repository/source contract tests:
  - `DeleteTrackResult` exposes a user-action/pending result.
  - `MediaStoreAudioRepository` uses `MediaStore.createDeleteRequest` for modern scoped-storage deletes and catches `RecoverableSecurityException` for API 29.
  - Modern `MainActivity` registers `StartIntentSenderForResult` and routes `RESULT_OK` back to the ViewModel.
- Manual/device verification:
  - Install the modern debug APK on an Android 11+ emulator/device with at least two audio files.
  - Long-press a queued track, choose `Delete from device`, accept app confirmation, accept Android system delete prompt.
  - Confirm the file disappears from the system media provider/file listing and the app's visible mixtape track list without needing an app restart.
  - Repeat cancel path and ensure the file/track remain visible.

Suggested commands after implementation:

```sh
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testLegacyDebugUnitTest
```

## Acceptance criteria

- `Delete from device` removes the selected audio item from Android device storage/media provider after required Android confirmation.
- The deleted track immediately disappears from app-level `tracks`, mixtape groups, and the now-playing track list.
- Failure/cancel/permission-denied paths leave state unchanged and show a clear failure/cancel message.
- Deleting the currently playing track does not leave playback or UI state pointing at a missing item.
- Existing long-press actions (`Remove from mixtape`, `Track info`) and playback/navigation behavior remain intact.
- Modern and legacy unit test tasks still pass, or any legacy limitation is explicitly documented.

## Risks and notes for Running

- Device-file deletion cannot be fully proven by local JVM tests; a real emulator/device smoke test is needed for Android scoped-storage behavior.
- `MediaStore.createDeleteRequest` availability is API-gated; keep old direct delete behavior for pre-29/pre-30 variants.
- The app currently supports a legacy flavor; if the legacy Activity does not expose the long-press delete UI, keep shared repository changes compile-safe for that variant.
- Search terms if extra platform confirmation is needed: `MediaStore.createDeleteRequest`, `RecoverableSecurityException`, `ActivityResultContracts.StartIntentSenderForResult`, `ContentResolver delete scoped storage audio Android 11`.
