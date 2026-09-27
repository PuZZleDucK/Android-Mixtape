# Native UI refinement - Nothing-phone verification

Date: 2026-09-27 (Australia/Melbourne)

## Requested changes

- Idle startup is a full-page tape library, with no deck or section header. Only active playback automatically reopens the player.
- Idle returns from Home/launcher also show the library, including Android's task-to-front path which does not deliver `onNewIntent`. Pausing or stopping while already inside the player does not hide it. Rotation does not count as a new entry. Explicit notification navigation retains its non-transport route.
- Track rows: 36 dp minimum instead of 48 dp, with 3 dp rather than 10 dp vertical padding. Existing lettering size is retained; long titles use one-line ellipsis, with full titles in accessibility semantics and Track info.
- Flat spines: a shared 6.5:1 aspect instead of 8.75:1, including shelf centering calculations.
- Portrait player-to-spine gap: 6 dp instead of 18 dp.
- Solid-filled eject, rewind, stop, play/pause and fast-forward symbols. The demo's opposing-arrow/gear artwork is retained.
- Transport order: Eject, Rewind, **Stop, Play/Pause**, Fast Forward, List/Settings.
- The seven compact Android deck faces now use 560 x 422, rather than 560 x 400. Their inner face panel encloses the buttons with a visible bottom margin. Other chassis proportions are unchanged; each chassis keeps its aspect through rotation.
- Larger mechanical and digital counter digits with slim borders. Shared geometry keeps their windows, labels, cassette wells and keys separate.

Source remains canonical on Artigas. Native code was not copied to Kunlun. Existing dirty worktree changes were not reset, no dependencies were added and nothing was pushed/published. A private source snapshot was made before this refinement.

## Build

Final verification: `verification-2.log`.

```
./gradlew --no-daemon --max-workers=2 assembleModernDebug testModernDebugUnitTest lintModernDebug --console=plain
```

- BUILD SUCCESSFUL in 7m 6s.
- **337 unit tests; 0 failures, 0 errors, 0 skipped**, independently counted from JUnit XML.
- Lint: 0 errors/fatal findings; 44 warnings and 2 hints remain.
- New checks cover idle/active launcher routing without transport commands, cold/warm session restoration, rotation-safe task returns, filled transport symbols/order, compact spacing, all counter bounds and taller panel/spine geometry.
- The first pass exposed an obsolete source contract requiring the old exact intent-handler call. That contract now verifies both notification handling and the new rotation-safe launcher argument. Device QA also caught the no-new-intent task-return path; the final lifecycle handling was updated and re-tested.
- Final source hashes: `verified-source-sha256.txt`.
- No Gradle/Kotlin build process or idle daemon remained after the final build.

No emulator or phone instrumentation suite was run for this follow-up. Physical verification used the specifically requested Nothing phone; Android Auto was not redesigned or newly device-tested.

## Artifact and duplicate removal

- Modern package retained: `org.puzzleduck.mixtape`.
- Obsolete development package removed: `com.example.androidmixtape` (target SDK 35; last updated 2026-09-08). `adb uninstall` returned Success. The final installed-package inventory contains only `org.puzzleduck.mixtape`.
- Updated modern app installed in place; its newly reviewed tapes/settings were not cleared. Audio files were not deleted or modified.
- Canonical dev artifact: `dist/android-mixtape-native-demo-debug.apk` and its `.sha256` file.
- Kunlun review APK: `/home/puzzleduck/x/remote-mixtape-testing/android-mixtape-native-refinement.apk`.
- SHA256: `f17b226d1535b8946c0c95384cc4c7537f299a27aae874e3fb3ebaccd0582f6b`.
- Installed `base.apk` SHA256 independently matched that artifact.

## Physical-device QA

All ADB calls pinned Nothing A001T / Galaxian, serial `00252359V002047`, API 36, 1080 x 2392 at density 420.

Verified on the installed final APK:

- Cold idle launch, warm paused Home/launcher return and new-Activity paused-session entry all show only the full-page tape library.
- Warm return and freshly created Activity during playback show the playing tape, moving hubs and real meters; the counter advances and session playback remains active.
- Pause and Stop keep the player on screen. Eject returns to the full-page library. The Stop action was invoked at its new position and playback became paused at position 0.
- Both orientations and light/dark appearance, with no counter clipping or buttons protruding from the compact face.
- Safety Yellow, Blackout Portable (digital counter) and Graphite Tall (mechanical counter) visually inspected. All eleven deck bounds are covered by geometry tests; not all combinations were manually inspected.
- List toggle centers the taller current spine. Selecting a tape opens its track list.
- Compact track long-press still exposes Track info, Remove from mixtape and Delete from device. Menu dismissed without a destructive action.
- No AndroidRuntime error for the final running process.

UI hierarchy measurements are in `layout-checks.txt`: Safety Yellow aspect 1.32637 portrait / 1.32641 landscape (pixel rounding); portrait player/spine gap 6.10 dp; spine ratio 6.5128:1; track rows 36.19 dp; current shelf spine centered within 1 pixel. Stop bounds are wholly left of Play/Pause.

## Handoff

The app is foregrounded on the full-page tape library, not playing. MediaSession reports PAUSED, position 0. The user's selected Safety Yellow deck was restored. Night mode yes, user rotation 0 and automatic rotation on were restored and verified.

Volume-control caveat: the initial media-volume read was 12/16; by the first attempted test adjustment the speaker was already at 16/16. Shell set/adjust commands and a volume-down key did not change the reported speaker level. The device was not reconfigured to override this behavior. Final media volume still reports **16/16**; playback is stopped. No claim is made that the intended low-volume test setting or volume restoration took effect.

Only `final-*` captures are the final APK's evidence. Earlier locally retained captures were preliminary checks and are not included here. The final numbered captures document idle/active entry paths, compact portrait/landscape layouts, mechanical/digital counters and the final library handoff.
