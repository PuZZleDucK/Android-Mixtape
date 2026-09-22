# Android 16 migration plan, card 4900

## Scope and ownership

Canonical source is `/home/puzzleduck/x/android-mixtape`. Keep `org.puzzleduck.mixtape`, min API 24, modern flavor, Compose cassette UI, scanning, saved customization, playback and car support. The android-apps clone is inventory only. Card 4899 is Done and its successful assembleModernDebug baseline remains valid. Do not rebuild baseline recovery.

Planning started at local HEAD `fea8466`. There are extensive unrelated tracked and untracked changes, including retired legacy sources, membership, audio meters, car playback and publishing. The board snapshot showed no other active Mixtape worker; the only other active card was 4502 in neon-vice. No active Gradle process was present before the test run. Recheck both before implementation. The planning commit owns only this directory and ModernizationScanLifecycleTest.kt. No application file was changed, staged, reset or stashed. No push.

Preserve the existing HEAD and all dirty content. Before editing shared files, save a patch and checksums under ignored `.work/card-4900/`. Compare the initial content with each final hunk. Stage only owned hunks or use an isolated index; never commit the whole working tree. Do not commit existing untracked membership/UI files merely because this build needs them. Explain this dirty-tree dependency in the final receipt. A replayable migration patch against the preserved starting tree is useful evidence.

Planning intentionally does not build/deploy an updated app: this stage requires failing tests before implementation. AGENTS.md's updated-app deployment requirement applies when the implementation changes. No new APK, emulator result or runtime success is claimed here.

## Source findings

- app/build.gradle.kts currently uses compile/target 35, Java source/target 17 and Kotlin JVM 17. Java 21 in AGENTS.md/STATUS.md is the Gradle runtime, not app bytecode. Keep those installed paths and bytecode 17. No JDK replacement is needed.
- Root AGP is 8.7.3, wrapper 8.10.2, Kotlin/Compose plugin 2.0.21. Baseline libraries are Compose BOM 2024.12.01, Activity 1.9.3, Lifecycle 2.8.7, coroutines 1.9.0, Media3 1.5.1, Car App 1.8.0-beta01 and media compat 1.6.0.
- MainActivity checks media permission only in onCreate and request-result. Returning from Settings without recreation can leave stale permission state. UI uses collectAsState rather than collectAsStateWithLifecycle. The ViewModel stores an Activity-bound deletion callback until ViewModel clearance, which is later than Activity destruction during rotation.
- MixtapeViewModel.refresh launches independent scans without a generation check or permission guard. Revoking permission does not invalidate a pending scan. runCatching also catches CancellationException. Older scans can overwrite newer results. These are demonstrated failures, not speculative cleanup.
- MediaStoreAudioRepository already queries on Dispatchers.IO and closes its cursor. READ_MEDIA_AUDIO and READ_EXTERNAL_STORAGE capped at API 32 are present. Scoped deletion already uses system confirmation on API 30+ and RecoverableSecurityException on 29. Do not replace it with broad storage access. Confirm API 29 retry semantics separately.
- Launcher and both car/media services already have explicit exports. The media service declares mediaPlayback and its foreground permissions. Media3 owns the background playback session. Keep background playback when the Activity stops; lifecycle-aware UI collection must not release the service player.
- MixtapeApp already uses Scaffold padding and Compose BackHandler. Verify actual insets and settings/subscreen navigation rather than adding duplicate system-bar padding. Review the retained delete callback and pending confirmation across recreation.
- CarAppService currently allows all hosts. Review debug versus release host validation against supported host signatures, without breaking emulator support. Inspect controller authorization, foreground launch/focus, notification session activity and PendingIntent flags in merged manifests/dependencies; do not invent direct PendingIntent bugs where Media3 owns them.
- kunlun-sync.sh supports KUNLUN_AVD and KUNLUN_EMULATOR_SERIAL, but its launch command hardcodes port 5554. An alternate serial cannot safely use --launch-emulator until this is fixed or the AVD is separately launched under the emulator skill. Always deploy the APK through this script. Do not manually copy source to Kunlun.

## Research and version decision

Official references read on 2026-09-22:

1. https://developer.android.com/about/versions/16/behavior-changes-16
   API 36 disables the edge-to-edge opt-out on Android 16. Back uses supported dispatch APIs rather than onBackPressed/KEYCODE_BACK. At sw600dp or greater the platform ignores orientation/aspect/resizability restrictions. Test short and wide windows and recreation; do not add opt-outs as a migration shortcut. The font change concerns TextView and does not justify replacing this app's custom handwriting renderer.
2. https://developer.android.com/build/releases/agp-8-13-0-release-notes
   AGP 8.13 supports through API 36.1 and needs Gradle 8.13, JDK 17 minimum. Select the already portfolio-tested AGP 8.13.2/Gradle 8.13 pair, compile/target 36, retaining JDK runtime 21 and Java/Kotlin output 17. No AGP 9 conversion is needed.

Implementation references:
- https://developer.android.com/develop/ui/compose/state#other-supported-types-of-state
- https://developer.android.com/training/permissions/requesting
- https://developer.android.com/media/media3/session/background-playback
- https://developer.android.com/media/optimize/audio-focus
- https://developer.android.com/training/data-storage/shared/media
- https://developer.android.com/training/cars/apps/library

Retain working libraries unless API 36 lint, back/insets, metadata or a documented compatibility fix requires an update. Upgrade aligned families together and record resolved versions. Evaluate a stable Car App replacement for beta01 only if it retains media-template API 8 support. Do not perform an unrelated Kotlin/Compose rewrite. Preserve all artwork and handwritten settings previews.

## Executed TDD evidence

New test file:
`app/src/test/java/com/example/androidmixtape/viewmodel/ModernizationScanLifecycleTest.kt`

Real command, with STATUS.md JAVA_HOME and ANDROID_HOME:

```sh
./gradlew --no-daemon --max-workers=2 testModernDebugUnitTest \
  --tests '*ModernizationScanLifecycleTest' --console=plain
```

Compilation succeeded. Three tests ran, all failed assertions, zero test errors:

| Case | Expected | Actual |
| --- | --- | --- |
| Revoke permission before scan completes | PermissionRequired with no tracks | Ready |
| Complete new refresh before old scan | Track ID 2 | Track ID 1 |
| Refresh after permission denied | Zero provider queries | One provider query |

Log: planning-red-tests.txt. These tests exercise the real ViewModel with a deferred fake repository and FakePlayerEngine. They do not prescribe a class name or check text patterns. Implement cancellation plus generation/permission guards; cancellation alone is insufficient for providers that return after cancellation. Ensure CancellationException is not reported as a user scan error.

## Implementation sequence

1. Refresh card 4900 and card 4899, STATUS.md, ownership and shared device usage. Preserve dirty-tree snapshots. Upgrade only SDK/toolchain first. Run current full unit suite and lint to separate inherited failures from migration failures; keep the report rather than suppressing it.
2. Track permission explicitly in ViewModel. Gate refresh; cancel/invalidate old work on refresh/revocation/clear. Only current authorized work may publish tracks, load the controller or assign names. Clear inaccessible queues on revocation without erasing durable tape membership or customization. Add cancellation and grant-again tests. Keep scans off main thread.
3. Recheck permission on resume with idempotent transitions; do not rescan/reset playback on every rotation. Use lifecycle-aware Compose collection. Detach Activity callbacks when the Activity is destroyed without clearing a newer Activity's binding. Add Activity recreation, Settings-return, and pending-delete cancellation/result tests before each fix.
4. Verify supported back/insets behavior on API 36 and API 24. Keep service-owned playback continuous during navigation, rotation and backgrounding. Address focus/noisy handling and controller failures where observed. Audit car host/controller validation and browser behavior with denied permission and no library.
5. Run all existing unit tests and lint, then real assembleModernDebug and assembleModernDebugAndroidTest. Fix new failures, not assertions that preserve valid product behavior. Compare inherited failing tests with initial source state if necessary.
6. Deploy through kunlun-sync.sh to compatible Kunlun emulators; never local Artigas, Nokia or Nothing. Use only synthetic music. Run the acceptance matrix below and capture tested-APK SHA256 plus visible feature evidence. Use an owned API 36 AVD and API 24 compatibility target when available. Car changes require the API 35 Automotive host; explicitly distinguish this from projected Android Auto. No phone-projected test is authorized.
7. Add BUILDING.md, which is currently absent. Update user-facing README only for actual modernized behavior and platform limits. Remove target-28 baseline wording only if found; canonical Mixtape is target 35, not target 28. Keep agent commands in AGENTS.md/BUILDING.md, not README. Update portfolio STATUS.md with versions, results, APK path and limitations. Commit owned hunks locally in a separate migration commit.

## Required regression and acceptance matrix

- Permission APIs 24/32 use READ_EXTERNAL_STORAGE; 33/36 use READ_MEDIA_AUDIO. Fresh denial, repeated denial, Settings grant, Settings revoke and grant-again give an honest empty/permission state. No camera/microphone/location or broad storage grant. Audio permission on an owned synthetic emulator is sufficient; never grant sensitive permissions on a user device.
- Scan results: empty database, malformed/zero-duration rows, SecurityException, cancelled scan, rapid refresh, old completion after revocation, newest completion winning, and non-cooperative old completion. Name generation must not publish after revocation. Keep existing mapper/exclusion/membership tests.
- Playback: seeded music appears in tapes; play, pause, seek, previous/next, eject and cues work. Position advances and service/controller state confirms actual playback. Rotation and Home/return do not restart or duplicate playback. Test competing media focus and noisy pause using emulator/synthetic inputs. Paused/stopped session must release resources appropriately.
- Lifecycle: Settings return updates permission without an Activity recreation. Ten stop/start cycles do not multiply listeners/controllers. Rotate while delete confirmation is pending; cancel is nondestructive. Confirm only synthetic test media deletion. Background process death restores durable names/settings/membership and does not claim nonexistent playback. Task removal/force-stop follow Android limits.
- UI: cassette deck, tape list and relevant settings at portrait/landscape, gesture/three-button navigation, cutout, dark theme, 200% font, short window and sw600dp. No bar-obscured transport or double padding. Back closes nested settings before leaving. Photograph screenshots of actual content, not just launch.
- Car: existing media-tree tests remain green. Browse synthetic tapes and operate playback through media session on Automotive if car code changes; check empty/denied library handling. Preserve metadata and queue continuity across phone/car controllers. Record projection-host absence without requiring a specific phone.
- Build: real modern debug APK, correct original application ID/min 24/target 36 and bytecode 17; lint has zero unsuppressed migration errors, with inherited warnings individually recorded. All existing and new focused tests pass. Store APK and screenshots through Git LFS, including checksum receipts, not placeholder files. Disposable builds/downloads belong in ignored .work/.

Build sequentially with --no-daemon --max-workers=2. Keep the full task commands and final tested source/APK hashes. Review prior successful evidence only when its covered bytes are unchanged. Shut down owned emulator process trees, temporary AVDs and browser sessions; preserve shared emulators/services. After checking no active build remains, stop any owned idle Gradle/Kotlin daemons. Planning started no emulator and its browser session was closed; the single-use Gradle daemon exited.
