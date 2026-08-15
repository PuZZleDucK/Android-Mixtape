# Card 2375: Android Auto support plan

## Request

Make Android Mixtape support Android Auto so the app can be discovered by a car head unit, expose a safe browse tree, and play local audio through car media controls.

## Current code touchpoints

- Modern playback is local to `app/src/modern/java/com/example/androidmixtape/playback/ExoPlayerEngine.kt` and uses Media3 ExoPlayer only while the Activity is alive.
- Shared playback abstraction lives in `app/src/main/java/com/example/androidmixtape/playback/PlayerEngine.kt` and `MixtapeController.kt`.
- Modern `MainActivity.kt` currently constructs `MixtapeController(ExoPlayerEngine(applicationContext))` directly.
- Local audio loading is already centralized through `MediaStoreAudioRepository` / `AudioRepository` in `app/src/main/java/com/example/androidmixtape/data/AudioRepository.kt`.
- Manifest currently only declares `MainActivity`; there is no media browser/library service, media session, foreground media service, or Android Auto metadata.
- Legacy flavor deliberately avoids Media3 runtime dependencies for API 19 compatibility. Android Auto support should be scoped to the `modern` flavor unless a separate legacy-safe browser service is explicitly requested later.

## Official references checked

- Android for Cars media overview: `https://developer.android.com/training/cars/media`
  - Android Auto/AAOS discover media apps through `MediaBrowserService` or `MediaLibraryService` plus a `MediaSession`.
  - Media items must be marked browsable and/or playable.
- Add Android Auto support: `https://developer.android.com/training/cars/media/auto`
  - Add app metadata `com.google.android.gms.car.application` pointing at `@xml/automotive_app_desc`.
  - `automotive_app_desc.xml` needs `<automotiveApp><uses name="media"/></automotiveApp>`.
- Media3 background/session docs:
  - `https://developer.android.com/media/media3/session/background-playback`
  - `https://developer.android.com/media/media3/session/serve-content`

Useful search terms for Running if docs drift: `Media3 MediaLibraryService Android Auto`, `androidx.media3 session automotive media browser service`, `automotive_app_desc uses media`, `Desktop Head Unit Android Auto media app testing`.

## Proposed behavior

1. The modern app declares itself as an Android Auto media app.
2. Android Auto can discover a modern-flavor `MediaLibraryService`/media browser service.
3. The service exposes a simple distraction-safe browse tree:
   - Root
   - `All tracks` playable children from `MediaStoreAudioRepository`
   - Optional `Mix tapes` browsable children using the app’s existing grouping rules if it can be done cleanly without pulling Compose/UI code into the service
4. Car controls support play, pause, previous, next, seek, queue navigation, and track metadata.
5. If storage/audio permission has not been granted on the phone, the service returns an empty/error root with a clear message rather than crashing.
6. Phone UI and car session should not create competing unsynchronized players. Prefer making modern playback service-backed and have the Activity talk to the same Media3 session/controller, rather than keeping a separate Activity-only ExoPlayer.
7. Legacy build stays API-19-safe and continues compiling/passing without Media3 session dependencies in shared or legacy runtime configurations.

## Suggested implementation steps

1. Add modern-only Media3 session dependencies in `app/build.gradle.kts`:
   - `modernImplementation("androidx.media3:media3-session:1.5.1")`
   - Keep Media3 dependencies out of shared `implementation` and legacy configurations.
2. Add modern Android Auto metadata/resources:
   - `app/src/modern/res/xml/automotive_app_desc.xml`
   - Modern manifest overlay with application metadata:
     - `android:name="com.google.android.gms.car.application"`
     - `android:resource="@xml/automotive_app_desc"`
3. Add required service permissions in manifest scope used by modern builds:
   - `android.permission.FOREGROUND_SERVICE`
   - `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` for modern target SDK behavior
   - Service attribute `android:foregroundServiceType="mediaPlayback"`
4. Create a modern service, for example `app/src/modern/java/com/example/androidmixtape/playback/MixtapeMediaLibraryService.kt`:
   - Extend `MediaLibraryService`.
   - Own a single `ExoPlayer` and `MediaLibrarySession`.
   - Implement session callback methods for root, children, item lookup, search, and search results.
   - Convert `Track` to `MediaItem` with stable media IDs, URI, title, artist/album if available, duration, and playable metadata.
   - Load `MediaStoreAudioRepository(applicationContext)` on a background coroutine/dispatcher.
5. Declare the service in a modern manifest overlay:
   - `android:exported="true"`
   - Intent actions for Media3 library/session and Android Auto compatibility, including platform media browser discovery.
   - Keep the service modern-source-set only.
6. Reconcile modern phone playback with the service:
   - Preferred: add a `PlayerEngine` implementation backed by a Media3 `MediaController` connected to `MixtapeMediaLibraryService`, and wire modern `MainActivity` to it when the session is ready.
   - Acceptable first pass if time-boxed: Android Auto service has its own player, but document the limitation and avoid simultaneous local/Auto playback where possible. This should be treated as less complete.
7. Add a small content-tree builder abstraction if needed so tests can verify browse nodes without spinning up Android services.
8. Update `README.md` known limitations/build notes after implementation: Android Auto support is modern flavor only; phone permission must be granted before car browse can see local audio.

## Tests to add/update

- Source/contract tests:
  - Modern build script has `media3-session` in `modernImplementation` only.
  - Legacy/shared build configurations do not pull in Media3 session/runtime dependencies.
  - Modern resources include `res/xml/automotive_app_desc.xml` with `<uses name="media"/>`.
  - Manifest/service source declares an exported MediaLibraryService/media browser entry and foreground media playback type.
- Unit tests for browse/content mapping where practical:
  - Root contains `All tracks`.
  - Track `content://` URIs become playable `MediaItem`s with stable IDs.
  - Empty/no-permission library returns safe empty/error content, not an exception.
  - Search matches title/artist/display name case-insensitively.
- Regression tests:
  - Existing `MixtapeControllerTest`, `PlaybackContinuityContractTest`, and API-19 compatibility tests still pass.
  - Modern and legacy unit tasks still compile/pass.

Suggested commands:

```sh
cd /home/puzzleduck/x/android-mixtape
./gradlew testModernDebugUnitTest --tests '*AndroidAuto*' --tests '*PlaybackContinuity*' --tests '*Api19VariantContractTest'
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
```

If a compatible Android Auto test environment is available, run a manual smoke using Android Auto Desktop Head Unit or a car/head-unit emulator:

1. Install modern debug APK on a compatible Android phone/emulator.
2. Grant audio/media permission in the phone app.
3. Seed at least two audio files.
4. Launch Android Auto/DHU and confirm Mixtape appears as a media app.
5. Browse `All tracks`, start playback, then verify play/pause/next/previous/seek update metadata and audio.
6. Disconnect/reconnect and verify the session remains discoverable without crashing.

## Risks and notes

- Android Auto will not render the existing Compose cassette UI; it only consumes the media browse/session model. Keep car UI simple and distraction-safe.
- Service-side MediaStore scans need permission handling; Android Auto cannot be the first place the user grants phone storage/media permission.
- Running two independent ExoPlayers would make phone UI and car state diverge. Prefer a service-backed modern playback architecture.
- Target SDK 35 foreground service requirements can fail at runtime if `FOREGROUND_SERVICE_MEDIA_PLAYBACK` or service type metadata is missing.
- Do not put Media3 session dependencies into shared `implementation`, or the legacy API 19 build may regress.

## Success criteria

- Modern APK declares Android Auto media capability and includes the automotive app descriptor.
- Android Auto can discover Mixtape through a media library/browser service.
- Car browse shows local tracks in a safe hierarchy after phone audio permission is granted.
- Playback from the car supports core controls and metadata through one coherent media session.
- Legacy APK remains API-19-safe and unaffected.
- The suggested modern/legacy unit and assemble commands pass, or any environment-only limitation is documented with concrete output.
