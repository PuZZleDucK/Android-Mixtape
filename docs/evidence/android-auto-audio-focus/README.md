# Android Auto audio-focus fix

## Findings and changes

The shared `MixtapeMediaLibraryService` player and standalone `ExoPlayerEngine` did not enable automatic audio-focus handling. Playback could progress without Mixtape owning media focus, consistent with the report that another media app had to be running before the car became audible.

Both players now set media/music audio attributes and enable ExoPlayer's focus handling. Focus loss, ducking and resumption remain ExoPlayer's responsibility; no second AudioManager focus implementation or volume/routing override was added.

Testing on Android Automotive 15 exposed another problem after enabling focus: background controller playback was denied focus because the service was not yet foreground. Media3 1.5.1 only performs early foreground promotion on API 31/32. The shared service now forwards play requests through a small wrapper that asks Media3 to publish its foreground playback notification before ExoPlayer requests focus on API 35+. Pause/release behaviour stays delegated. If foreground startup is refused, the request is logged and playback is not started.

References reviewed:
- https://developer.android.com/media/optimize/audio-focus
- https://raw.githubusercontent.com/androidx/media/1.5.1/libraries/session/src/main/java/androidx/media3/session/MediaSessionService.java

## Verification

- Original APK failed all 3 corrected focus regressions on API 24: `api24-baseline-tests.txt`.
- API 35 initially demonstrated both missing ownership and, after enabling focus alone, a background focus denial. `before-foreground-fix-audio.txt` records the denial with process state 4.
- After both fixes, all 3 focus integration tests passed on a fresh Android Automotive 15 emulator: `automotive-fixed-tests.txt`.
- Current Automotive focus-owner dumps show Mixtape's ExoPlayer AudioFocusManager holding GAIN with USAGE_MEDIA / CONTENT_TYPE_MUSIC, including with the phone activity in the background: `automotive-*.txt`.
- All 24 modern instrumentation tests passed on the Kunlun API 24 emulator: `api24-full-tests.txt`. The focus tests connect to the actual shared service, decode a local generated WAV, verify cold and background starts, and check transient-loss resumption and permanent-loss pause.
- 250 modern and 239 legacy unit tests passed. Legacy lint passed. Modern lint still has the same 7 existing errors and 31 warnings: `final-unit-and-lint.txt`.
- Updated modern APK installed and launched through `kunlun-sync.sh`; `dist/` APK and checksum refreshed.

## Limits and cleanup

These tests verify framework focus ownership, music attributes and sustained playback state. Emulators run with `-no-audio`; they do not prove sound at a physical head unit or Android Auto projection. The user still needs to try the APK in the car with other media sources stopped. No Nokia or other user device was accessed.

The existing Automotive AVD had a differently signed app, so it was not uninstalled or overwritten. A fresh task-only Automotive AVD was used and then deleted. Both task-owned emulator processes and browser sessions were stopped. Test tones were removed. No commits or pushes were made.
