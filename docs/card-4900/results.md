# Android 16 migration results

Canonical checkout `/home/puzzleduck/x/android-mixtape`, card 4900. Recovery
baseline and planning commit `00202f5` remain intact. No push or phone use.

## Changes

- Compile/target 36, Gradle 8.13 and AGP 8.13.2. Java/Kotlin bytecode stays 17;
  installed Java 21 runs Gradle. Application ID, minimum API 24, version 0.1.0
  and the existing modern Compose product are retained. Library versions are
  listed in BUILDING.md; none needed upgrading for these checks.
- Scans now have cancellation, generation ownership and an explicit permission
  gate. A late non-cooperative provider cannot replace a newer scan or revive a
  revoked library. Cancellation is not rendered as an error. SecurityException
  returns to permission-required state. Revocation clears accessible tracks and
  the actual player queue, while preserving stored customization/membership.
- MainActivity rechecks permission on resume, collects state with lifecycle
  ownership and releases only its own deletion callback on destruction. Repeated
  resumes do not reset playback or rescan an already loaded library.
- The shared Media3 player handles becoming-noisy events. Seven lint errors were
  fixed with the supported SessionError constant and explicit, narrow Media3
  unstable-API opt-ins, not lint suppression or a baseline.
- kunlun-sync.sh now uses the requested emulator port and supports an explicit
  physical display ID for Automotive screenshots. All APK deployment and final
  screenshot capture went through this script.

## Executed checks

| Check | Result and evidence |
| --- | --- |
| Real Gradle build | Modern debug and instrumentation APKs built, final-build-tests.txt and device-test-build.txt |
| Full JVM suite | 295 tests, zero failures/errors, unit-summary.txt |
| Permission/scan regressions | Five tests, including non-cooperative stale completion, revocation, denied refresh, repeated resume/grant-again and provider SecurityException |
| Android lint | Zero errors, 42 warnings, two hints, final-lint.txt; no new suppression |
| API 36 full device suite | 50 discovered, 48 passed and two existing assumption skips, zero failures, final-device-suite.txt |
| API 24 focused device suite | 11 passed, focused-api24.txt |
| API 35 Automotive media focus | Three passed, focused-auto35.txt |
| APK identity and bytecode | apk-metadata.txt, bytecode.txt and apk-sha256.txt |

The device suites exercise actual service/controller playback, pause/seek/stop,
track changes, rotation and recreation, eject/reselection, background controller
start, temporary/permanent focus loss, callback rebinding, ten stop/resume cycles,
settings navigation, names, exclusions, layouts, artwork and PCM meters. The two
pre-existing assumption skips are a debug tap mode and opt-in visual font audit;
they are not disabled migration failures. All five new JVM regressions and the
new Activity lifecycle test execute.

The first full device run exposed fixture/test assumptions rather than SDK
compilation errors. Its transport test required seeded MediaStore music. The PCM
meter test fed only one buffer although the meter intentionally expires after
500 ms. Layout expected a 64 dp control despite the existing 48 dp design. Settings
used a stale Back label and clicked below the keyboard. The overhang oracle did
not generate negative bearing on this emulator and mishandled fully clipped ink.
Repairs feed continuous synthetic PCM, require the existing 48 dp target with
one dp rounding tolerance, scroll/dismiss the keyboard, use the actual Back
label and independently check explicit negative-bearing raster pixels including
empty clips. The complete final run passes; no failing case was removed.

## Emulator evidence

Only owned Kunlun AVDs were used, sequentially on emulator-5580. Synthetic music
was a generated 90-second sine tone, not the user's library.

- playing-api36.png and playing-api24.png show the real cassette deck and meters.
  playing-session.txt and background-rotation.txt record PLAYING and advancing
  positions after rotation and Home/return.
- revoked-api36.png shows the permission state after a real OS revocation.
  grant-resume.txt records permission grant while backgrounded and return in the
  same process. The new device test also verifies the same Activity rechecks its
  permission and retains the same ViewModel through recreation.
- library-api36.png, landscape-api36.png, small-dark-large-font.png and
  settings-large-font.png cover actual library, landscape, a 600 by 400 dp
  window with 200% font and system night mode. The custom deck retains its chosen
  colors; settings use their own night palette.
- process-death.txt records an actual app-UID kill, PID 5012 to 5075.
  restored-library.png shows the persisted tape name after restart. The initial
  am kill attempt did not kill the bound service and was not counted as evidence.
- car-host.png is a limitation capture, not successful car browse evidence. The
  stock Automotive classic browser connects and the app returns a playable root
  item after seeding the current Automotive user 10. The host still reports
  "Media isn't available for this list". car-session-log.txt records the returned
  item. The existing flat media-tree/templated car design is unchanged; three real
  Automotive shared-service focus tests pass. This is not phone-projected Auto
  verification and no Nokia or Nothing device was inspected.

## Limits and audit

The manifest already has explicit component exports, API-specific read-audio
permissions and the mediaPlayback foreground-service declaration. Media3 owns
session notifications and their intents. There is no new broad storage access.
The existing car host validator accepts all hosts; this debug-only deliverable
is not a release security certification. A signed public release should restrict
host validation separately from this SDK/lifecycle migration.

Physical predictive-back gestures, actual headset removal, pending scoped-delete
confirmation across rotation and API 29's confirmation retry were not device-tested.
Existing deletion/cancellation unit contracts pass. A shell attempt to inject the
protected becoming-noisy broadcast was denied by Android; it is not counted as a
runtime pass. ExoPlayer's noisy handling is configured, and focus-loss behavior
was tested on all three platform versions. Projected/template Auto remains
unverified because no phone work is authorized. The measured classic Automotive
browse limitation is retained above, not described as a successful feature test.

Long handwritten labels deliberately retain their existing clipped overflow
behavior. Below Android's normal 320 dp application width, the library heading
can wrap heavily. The 600 by 400 dp large-font capture remains usable. No custom
UI redesign or blanket accessibility claim is made.

## Ownership and reproducibility

There was no other active Mixtape worker or Gradle process when implementation
started. Only card 4502 in another project was active. All pre-existing tracked
and untracked changes were preserved. Shared files were snapshotted before edits.
Only migration hunks were staged against HEAD; no reset, stash or whole-tree
commit was used. The APK intentionally includes the pre-existing canonical work,
so this commit alone is not a clean-tree reproduction of the tested build.

owned-dirty-tree.patch records all deltas to previously dirty files. Three
pre-existing untracked tests remain untracked, with only their owned repairs
stored in untracked-test-adjustments.patch. The tracked overhang test and new
regressions are committed normally. tested-source-sha256.txt records the complete
source/build-file bytes used for verification. Recheck these before reusing logs.
No dependency folder was edited and no baseline APK was replaced.

Both final APKs and all newly retained PNGs use Git LFS. cleanup.txt confirms
removal of all three owned AVDs and the remote QA directory. No owned emulator,
Gradle/Kotlin daemon, tunnel or browser remains. Small ignored ownership snapshots
remain under .work/card-4900; disposable generated audio was removed.
