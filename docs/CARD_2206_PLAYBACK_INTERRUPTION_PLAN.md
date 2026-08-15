# Card 2206 playback interruption plan

Bug: turning the screen off or changing orientation interrupts playback.

## Current observations

- `app/src/modern/.../MainActivity.kt` calls `viewModel.onPermissionResult(...)` on every `onCreate`. When orientation recreates the activity, this can call `refresh()`, reload the MediaStore list, and make `MixtapeController.load(...)` reset the ExoPlayer playlist while audio is playing.
- `app/src/legacy/.../MainActivity.kt` owns `MixtapeController(PlatformMediaPlayerEngine(...))` directly and releases it in `onDestroy`, so a configuration-change destroy/recreate definitely stops legacy playback.
- Both playback engines are activity/view-model scoped and neither declares/uses wake-lock or foreground media playback support, so screen-off playback is fragile. README already notes playback is foreground-only, but this card asks for screen-off continuity.

## Proposed implementation path

1. Add regression tests around orientation-style recreation:
   - Modern/shared ViewModel: once tracks are loaded and playback has started, a repeated granted-permission/startup call must not refresh/reload the playlist or clear `isPlaying`.
   - Legacy: cover whichever retention strategy is chosen with a small unit/contract test where possible.
2. Fix modern recreation reset:
   - Replace the unconditional `viewModel.onPermissionResult(granted)` from `MainActivity.onCreate` with an idempotent startup method such as `ensureAudioPermissionAndLibraryLoaded(granted)`.
   - Only call `refresh()` when permission has just become granted and the library is not already loaded/loading; do not reload while `status == Ready` and playback state exists.
3. Fix legacy orientation interruption:
   - Preferred: move legacy playback/library state into an AndroidX `ViewModel` or other retained owner instead of constructing/releasing it in `Activity` on every rotation.
   - At minimum, do not release the player during configuration changes (`isChangingConfigurations`) and restore/rebind the existing controller/state after recreation. Prefer the ViewModel approach because lifecycle dependencies already exist and support API 19.
4. Improve screen-off continuity for both variants:
   - Add `android.permission.WAKE_LOCK`.
   - Modern: configure ExoPlayer with local wake mode while playback is active.
   - Legacy: call `MediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)` before prepare/start.
   - If wake mode alone is insufficient in manual testing, escalate to a small foreground media playback service/notification (`foregroundServiceType="mediaPlayback"`, target-SDK-compatible permissions) that owns the player instead of the activities.
5. Update README known limitations/manual smoke test to describe expected screen-off/orientation behavior once fixed.

## Success criteria

- Modern and legacy builds keep the selected track playing across portrait/landscape recreation.
- Pressing power to turn the screen off does not stop the currently playing track during a short smoke test (30-60 seconds), and playback status is still coherent after unlocking.
- Refresh remains explicit: normal activity recreation must not rescan/reload and interrupt playback.
- Existing library, mixtape grouping, permission, and launcher tests continue passing.
- Verification commands: `./gradlew testModernDebugUnitTest testLegacyDebugUnitTest assembleModernDebug assembleLegacyDebug`; if a device/emulator is connected, install the relevant variant, seed one audio file, start playback, rotate, screen off, wait, unlock, and confirm playback continues.

## Risks / notes

- A full foreground service with media notification is the robust long-term Android pattern, but it is a larger change than wake-mode plus lifecycle retention. Try the smaller lifecycle/wake-mode fix first, then escalate only if device testing shows screen-off still stops playback.
- Avoid making a specific physical device a hard requirement; use any compatible API 19+ device/emulator for verification, and document limitations if no audio-capable device is available.
