# Android Mixtape TDD Test Plan

Card: Kanban #2111 — `[android mixtape] new project`

Planning rework: see `docs/PLANNING_REWORK.md` for the API 24 compatibility and sideloadable APK plan.

The initial project exists, but Review sent it back because the available test device is API 24 while the app was planned as minSdk 26. The next Running stage should lower the compatibility floor to API 24, add/update tests first where practical, and produce a sideloadable APK artifact.

## Rework pass results

The prior red checks have been resolved for Review:

- `app/build.gradle.kts` now uses `defaultConfig.minSdk = 24`.
- `README.md` and `docs/RUNNING_VERIFICATION.md` describe Android 7+/API 24 support and sideload usage.
- `app/src/test/java/com/example/androidmixtape/permissions/AudioPermissionPolicyTest.kt` explicitly covers SDK 24, 26, 32, and 33+.
- `dist/android-mixtape-v0.1.0-debug.apk` and `dist/android-mixtape-v0.1.0-debug.apk.sha256` are generated from `assembleDebug`.
- `connectedDebugAndroidTest` and manual playback smoke ran on the attached F3115 API 24 device; see `docs/RUNNING_VERIFICATION.md` for commands and evidence.

## Command-line quality gates

Expected from a clean checkout after project generation:

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
```

On the attached F3115 API 24 device, after lowering `minSdk` to 24:

```sh
./gradlew connectedDebugAndroidTest
```

The build must also leave a sideloadable debug-signed APK under `dist/` with a SHA-256 checksum.

## Unit tests to add first

### Permission policy

Create a small permission helper so it can be tested without Android UI:

- SDK 33+ requests `android.permission.READ_MEDIA_AUDIO`.
- SDK 24–32 requests `android.permission.READ_EXTERNAL_STORAGE` if the chosen target still supports it.
- Permission denied produces a visible `PermissionRequired` / denied state instead of trying to scan MediaStore.
- Permission granted triggers a library scan.

### Audio repository / MediaStore mapping

Use a fake `ContentResolver` or repository abstraction for JVM tests; reserve direct MediaStore cursor checks for instrumentation if needed.

- Maps audio rows to a `Track` model with stable `content://media/external/audio/media/{id}` URI.
- Uses non-empty fallbacks when title or artist columns are null/blank.
- Ignores rows with missing IDs or non-positive duration if the app chooses to filter unusable files.
- Returns an empty list without crashing when the cursor is empty or null.
- Sorts results deterministically, preferably by title then ID or by MediaStore display name.

### Player/controller behavior

Use a fake player interface around Media3/ExoPlayer for local tests:

- Loading a non-empty track list initializes the playlist and selects index 0 without crashing.
- Selecting a track seeks to that index and starts playback.
- Play/pause toggles update UI state.
- Next/previous stay within bounds and expose disabled controls at the ends, or wrap only if intentionally specified.
- Seek clamps to `0..durationMs`.
- Releasing the controller releases the player exactly once.

### ViewModel/state behavior

- Initial state is loading or permission-needed, not a blank screen.
- Granted permission + empty repository emits an explicit empty-library message.
- Repository failure emits a recoverable error state and keeps a refresh action visible.
- Track selection updates current track metadata and progress state.
- State survives Compose recomposition; rotation should not duplicate playlist items or leak players.

## Compose UI tests

Add tests with fake state/controller objects:

- Permission prompt explains why audio access is needed and exposes a request button.
- Empty library state tells the user to add audio files and exposes refresh/rescan.
- Track list renders title, artist/path fallback, and duration for each fake track.
- Tapping a track calls the select/play action with the correct index.
- Mini-player shows current track title, elapsed/duration, play/pause, previous, next, and seek slider.
- Disabled/error states are accessible and do not hide the main recovery action.

## Manual/emulator smoke test

Use the attached F3115 API 24 device for the blocking Review gap, plus any newer emulator/device if available:

1. Build the sideloadable debug APK and install it.
2. Seed one or more audio files, for example:
   ```sh
   adb push sample-audio.mp3 /sdcard/Music/mixtape-smoke.mp3
   ```
3. Launch app and grant the requested audio permission.
4. Verify the seeded file appears in the list with recognizable metadata/fallbacks.
5. Tap the track and verify audio playback starts.
6. Exercise pause/resume, seek, next/previous, and refresh.
7. Revoke permission from Android settings and verify the app returns to the permission/denied state cleanly.
8. Remove/rename audio files and verify the empty-library state.

## Acceptance criteria for Running

- Android project exists under `/home/puzzleduck/x/android-mixtape` with README build/run/sideload instructions.
- `minSdk` is 24 so the available F3115 API 24 device can validate the app.
- A sideloadable APK and checksum exist under `/home/puzzleduck/x/android-mixtape/dist/`.
- The command-line gates above pass or documented blockers are left in the card.
- Connected tests run on the API 24 device, or any failure is a real app/test failure rather than minSdk incompatibility.
- At least the JVM unit tests for permission policy and player/controller behavior are present.
- Manual smoke steps are executed and documented with the device/emulator API level used.

## Card #2200 — Settings page and reset-all-mixtapes TDD checks

Add these failing checks before implementation:

- `MixTapeQueueViewModelTest.resetAllMixTapesReshufflesOnlyTheMixtapeOrderAndPreservesLibraryTracks`: inject a deterministic `Random`, reset all mixtapes, preserve the scanned library order and track set without duplicates/loss, keep groups capped at 12 tracks, and confirm reset produces a different randomized mixtape order.
- `MixTapeQueueViewModelTest.settingsResetStopsPlaybackClearsStaleQueueAndReturnsToMixTapes`: expose a Settings screen, reset from Settings, stop playback, clear any old selected queue/current track, return to Mix Tapes, and leave a reset confirmation message.
- `MixTapeQueueViewModelTest.selectingSecondMixTapeQueuesOnlyItsRandomizedGroup`: selecting a mixtape queues the randomized group shown to the user, not the original MediaStore slice.
- `MixtapeAppTest.libraryScreenOffersSettingsEntryPoint`, `mixTapesScreenOffersSettingsEntryPoint`, `nowPlayingScreenOffersSettingsEntryPoint`, and `settingsScreenShowsResetSectionAndCallsResetAllMixtapes`: Compose UI exposes Settings from Library/Mix Tapes/Now Playing and a Reset section with a visible `Reset all mixtapes` action.

Running should implement the minimum production API to satisfy those tests, then verify:

```sh
./gradlew testModernDebugUnitTest
./gradlew testLegacyDebugUnitTest
./gradlew assembleModernDebug
./gradlew assembleLegacyDebug
```

## Card #2211 — Cassette visual aspect ratio TDD checks

Red test added first: `CassetteVisualAspectRatioContractTest`.

Expected Running behavior:

- Add one shared cassette visual aspect-ratio contract in `MixtapeApp.kt`, e.g. `CASSETTE_VISUAL_ASPECT_RATIO`.
- Apply that ratio through `Modifier.aspectRatio(...)` to both drawn cassette surfaces:
  - `CassetteCaseCard` on the Mix Tapes screen.
  - `CassetteTape` on the Now Playing screen.
- Remove the old full-width + fixed-height cassette drawing pattern that flattens or stretches art on different widths.
- Preserve the existing Mix Tapes readability contract: three preview lines and reduced handwritten text sizes.
- Verify visually after implementation in phone portrait, phone landscape, and a wider/tablet-width configuration.

Focused red/green command:

```sh
./gradlew :app:testModernDebugUnitTest --tests com.example.androidmixtape.ui.CassetteVisualAspectRatioContractTest
```
