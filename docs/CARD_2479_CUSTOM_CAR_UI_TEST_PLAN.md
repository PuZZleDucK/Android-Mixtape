# Card 2479 Testing Plan: Custom Mixtape car UI

## Stage goal

Convert the Planning document into executable TDD gates before implementation. These tests are expected to fail on the current card-2375 media-browser-only code and should guide the Running implementation.

## Automated contract gates added

### `AndroidAutoSupportContractTest`

- Modern flavor must add Android for Cars App Library as `modernImplementation("androidx.car.app:app...")` while keeping Cars/Media3 dependencies out of shared and legacy variants.
- `automotive_app_desc.xml` must contain both `<uses name="media"/>` and `<uses name="template"/>`.
- Modern manifest must request `androidx.car.app.MEDIA_TEMPLATES`, declare `androidx.car.app.minCarApiLevel` with value `8+`, and expose `MixtapeCarAppService` with action `androidx.car.app.CarAppService` plus category `androidx.car.app.category.MEDIA`.
- `MixtapeMediaLibraryService` must remain only as playback/media-browser compatibility plumbing, not as the primary `root -> All tracks` user-visible car experience.
- Modern source must include the custom flow: `MixtapeCarAppService` -> `MixtapeCarSession` -> `MixtapeListScreen` -> `MixtapeSideScreen` -> `MediaPlaybackTemplate`/`MediaPlaybackManager`.
- README/TEST_PLAN/tests must no longer describe the old stock `All tracks` Android Auto browse root as success.

### `AndroidCustomCarUiFlowContractTest` (modern JVM source set)

- `MixtapeCarSession.onCreateScreen` launches directly to `MixtapeListScreen`.
- Mixtape list rows navigate to `MixtapeSideScreen` for the selected mixtape.
- Side screen renders selected side/track rows and starts playback through the shared `MixtapeMediaLibraryService` session, preserving selected track index.
- Playback screen uses `MediaPlaybackTemplate` and registers the shared session token with `MediaPlaybackManager`.
- Car catalog exposes `CarMixtape`/`CarMixtapeCatalog`, reuses `buildMixTapeGroups`, emits stable `mixtape:*` and `track:*` IDs, and handles permission-denied MediaStore access safely.

## Expected current failure evidence

Run:

```sh
./gradlew testModernDebugUnitTest --tests '*AndroidAuto*' --tests '*AndroidCustomCarUi*'
```

Expected until Running implements the card:

- failure for missing `androidx.car.app:app` dependency;
- failure for missing `<uses name="template"/>` and `MEDIA_TEMPLATES`/`minCarApiLevel`/`CarAppService` manifest declarations;
- failure because old `ALL_TRACKS`/`All tracks` media-browser tree and README language still exist;
- failure for missing `MixtapeCarAppService`, `MixtapeCarSession`, `MixtapeListScreen`, `MixtapeSideScreen`, `MediaPlaybackTemplate`, `MediaPlaybackManager`, and car catalog model.

## Rework gate from Review: phone/car playback coherence

Review found that the custom car UI and shared-session plumbing are present, but coherence is not proven: car-side playback can replace the Media3 queue while the phone `MediaControllerPlayerEngine` only reports `currentMediaItemIndex`, causing `MixtapeController` to map that index into stale phone-side `state.tracks`. Testing must add gates that fail for that exact bug before returning to Running.

Executable Testing rework gates added:

- `MixtapeControllerTest.externalPlaybackSnapshotReplacesStalePhoneQueueAndPlayState` is the direct regression fixture: phone has mixtape A loaded, the car starts mixtape B at index 1, and the controller must replace the queue/current track/play state/position/duration together.
- `MixtapeViewModelTest.carInitiatedPlaybackUpdatesPhoneNowPlayingQueueAndCurrentTrack` proves the phone UI state follows the car-selected side instead of keeping the stale phone mixtape.
- `PlaybackContinuityContractTest.playerEngineExposesExternalPlaybackSnapshotForCarInitiatedQueueChanges`, `mediaControllerPlayerEngineObservesExternalTimelineMediaItemAndPlayingEvents`, and `mixtapeControllerHandlesExternalPlaybackSnapshotByReplacingQueue` are source contracts for the required PlayerEngine snapshot API, Media3 listener coverage, and controller queue replacement behavior.

Add or update tests so they require:

- `PlayerEngine` exposes an external playback-state/queue callback richer than the current index-only listener, or an equivalent contract. It must carry queue/media IDs or `Track` values, current index/media ID, play-pause state, position, and duration when available.
- `MediaControllerPlayerEngine` listens for externally initiated Media3 timeline/playlist changes, media-item transitions, playback-state changes, and `isPlaying` changes from the shared `MixtapeMediaLibraryService` session.
- `MixtapeController` replaces its phone-visible queue when an external queue snapshot arrives, rather than reusing stale `state.tracks`.
- `MixtapeViewModel` refreshes `queueTracks`, `currentTrack`, `currentIndex`, now-playing state where appropriate, and play-pause state after a car-initiated playback update.
- Regression fixture: phone starts on mixtape A, car starts mixtape B at index 1, then the phone-visible state must show mixtape B's index-1 track and `isPlaying=true`; it must not show mixtape A's index-1 track.

Suggested focused test files/names:

- Add to `MixtapeControllerTest`: `externalPlaybackSnapshotReplacesStalePhoneQueueAndPlayState`.
- Add a source/contract test near `AndroidCustomCarUiFlowContractTest`: assert `MediaControllerPlayerEngine` observes `onEvents`/timeline/`isPlaying` and does not rely solely on `onMediaItemTransition(currentMediaItemIndex)`.
- Add to `MixtapeViewModelTest` or a new playback continuity test: `carInitiatedPlaybackUpdatesPhoneNowPlayingQueueAndCurrentTrack` using a fake `PlayerEngine` that emits the external snapshot.

## Running verification expectations

After implementation, run at minimum:

```sh
./gradlew testModernDebugUnitTest --tests '*AndroidAuto*' --tests '*AndroidCustomCarUi*' --tests '*PlaybackContinuity*' --tests '*MixtapeController*' --tests '*MixTapeGrouping*'
./gradlew testLegacyDebugUnitTest --tests '*Api19VariantContractTest*'
./gradlew testModernDebugUnitTest
```

Then build and deploy the modern APK to the Kunlun emulator per project policy. Manual DHU/AAOS verification should confirm:

1. Mixtape appears as a templated/custom media app, not just stock media browse.
2. First car view is the mixtape list.
3. Selecting a mixtape opens the side-of-tape track list.
4. Starting playback from the side view updates shared playback controls.
5. Phone UI remains coherent with car-initiated playback: after car starts/changes a side, the phone shows the same current track, the car-selected side/queue, and the same play-pause state.

If no DHU/AAOS host with media-template support is available, document exact emulator/device image and missing host capability rather than treating that as a product failure.
