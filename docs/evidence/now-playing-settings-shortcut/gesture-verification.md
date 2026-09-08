# Gesture verification, card 4834

The Kunlun API 24 emulator passed `NowPlayingSettingsGestureTest` on 2026-09-09 local time. The test uses injected Compose touch events for short taps, cancellation and long presses in both body modes. It checks that long press returns to the same mode without an accidental toggle, exercises system Back and the Settings header Back, and invokes the accessibility long-click semantics action in both modes. Four Settings openings are expected and observed.

The first run failed because the test searched for text `Back` instead of the header's accessible description `Back to Back`. The corrected run passed. That redundant accessible label should be cleaned up before Review.

`testModernDebugUnitTest` passed with 272 tests and no failures or errors. The first combined build exceeded the command timeout after assembling both APKs. The subsequent unit/lint invocation completed the unit task but failed lint with seven errors in Android Auto diagnostics and the media library service, outside this card's changed code. See `full-unit-lint.log`. No lint baseline or suppression was added.

The APK was deployed through `kunlun-sync.sh` before connected testing. Connected testing removed its installed app afterward, so the APK was installed again through the script. The three `resumed-*.png` screenshots show the gear, Settings opened by a long press, and system Back returning to the track list. Playback advanced during this manual round trip; these images are navigation evidence, not an assertion that the same track remained active.

Still needed before Review: theme/orientation matrix, nested edited-settings runtime evidence, fresh-store persistence and stronger engine-command assertions, isolation and commit of the preceding worker's production changes, and resolution or explicit baseline comparison of unrelated lint errors. The existing `.work/card-4834/baseline` and `owned.patch` remain available for isolating those changes.

The run stopped its Kunlun emulator, SSH ADB tunnel and idle build daemons.

## Source isolation follow-up

The next run isolated the shortcut production changes from the existing settings redesign and other unrelated edits using a three-way merge against `.work/card-4834/baseline`. The scoped commit includes callback wiring, retained body mode, gear drawing, combined gestures and origin-aware navigation, plus the previously uncommitted plan, unit tests and final screenshot set. The unrelated working-tree files were not overwritten. PNG entries were verified with `git lfs ls-files`.

No source or test bytes in the working tree changed during this isolation, so the recorded 272-test run and Kunlun deployment remain the working-tree evidence. The isolated committed tree has not been built separately and still contains the older Settings layout. The generic Back label in the uncommitted settings redesign is not included in the isolated commit. Theme/orientation coverage, nested edited-settings runtime evidence, stronger persistence and engine assertions, and the redundant accessible Back label remain open. Do not move to Review on the strength of this source-isolation step alone.

## Engine command regression follow-up

The playing and paused navigation tests now record every playlist load, track selection, seek and release call. Comparing only the previous fake's last index and seek value could miss a repeated command with the same argument. The command history stays unchanged through nested Settings, visual edits and Back. Existing play, pause and queue-replacement counters also remain unchanged.

Both focused tests passed in a real `testModernDebugUnitTest --tests '*NowPlayingSettingsNavigationTest'` run, with zero failures or errors. The worker environment lacked Java and SDK configuration. The successful invocation supplied `JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS` and `ANDROID_HOME=$HOME/Android/Sdk`. Raw output is in `.work/card-4834/engine-command-tests.log`. Idle Gradle and Kotlin daemons were stopped afterward.

This pass changed tests only. It did not build or deploy a new APK or capture new screenshots. Remaining acceptance work is the theme/orientation matrix, nested edited-settings runtime evidence, fresh-store persistence, accessible Back label cleanup, isolated-tree verification and the unrelated lint baseline comparison.

## Isolated committed-tree verification

Verified detached commit `feaa244` without the unrelated working-tree edits. `testModernDebugUnitTest` passed all 257 tests with zero failures, errors or skips. The working tree's previously recorded 272 tests include unrelated additions. Per-suite counts are in `isolated-tests.txt`; the Gradle output is in `isolated-checks.log`. The first invocation timed out after 200 seconds during compilation. A second invocation completed the tests and lint in 6 minutes 9 seconds.

Lint still fails with seven errors and 31 warnings. `isolated-lint.txt` records the full report. The error IDs and affected statements match the existing working-tree report: one WrongConstant and three UnsafeOptInUsageError findings in MixtapeMediaLibraryService, plus three UnsafeOptInUsageError findings in AndroidAutoDiagnostics. The service line numbers differ by 38 because of unrelated working-tree edits. Both committed files are byte-identical to pre-shortcut commit `eaf0f94`, as verified with git diff. This is a source/report comparison, not a fresh lint execution at the older commit. No shortcut lint error was reported.

No production or test source changed in this verification pass. No APK deployment or screenshots were repeated. Removed the temporary worktree and stopped idle build daemons. Remaining before Review: theme/orientation matrix, nested edited-settings runtime evidence, fresh-store persistence and accessible Back label cleanup. The isolated committed tree now has compilation and unit evidence, but its UI has not been independently deployed.

## Real preference-store reload coverage

Added `NowPlayingSettingsPersistenceTest`. Kunlun API 24 passed its single connected test. It changes handwriting messiness and deck theme through the production SharedPreferences stores, drains pending writes, and verifies complete settings equality through newly constructed stores. Unique test-only preference names keep the installed app's settings untouched and are deleted afterward. This tests fresh store objects in the same process, not process-death recovery or UI editing.

Built the working-tree modern app and instrumentation APK, deployed with `kunlun-sync.sh`, and reinstalled after connected testing removed the app. `persistence-pass-gear.png` shows the redeployed portrait light-theme toggle. The XML and Gradle output are saved alongside this note. No production source changed. The first connected attempt timed out because DDMLib also needed `ANDROID_ADB_SERVER_PORT=15037`; the retry passed in 30 seconds. The launch environment omitted Java and Android SDK paths, so these runs explicitly set JAVA_HOME to the installed Java 21 and ANDROID_HOME to the existing SDK.

Stopped the owned Kunlun emulator, SSH tunnel and idle Gradle/Kotlin daemons. Still required before Review: theme/orientation matrix, nested edited-settings runtime evidence and accessible Back label cleanup. Existing playing/paused unit coverage and the new store test cover separate layers; neither substitutes for the remaining UI round trip.

## Nested visual edit and theme matrix

Deployed the previously built working-tree APK through `./kunlun-sync.sh --all` on Kunlun API 24. APK SHA-256: `46e9f12cdf7de394751a148b32a0122816fc5a9acbae9bf458e8450d79f8b142`. No source or test bytes changed and no build was repeated in this evidence-only pass.

Paused playback, long-pressed the toggle, opened Appearance > Deck theme, and selected Blackout Portable. `nested-blackout-selected.png`, `nested-back-settings.png` and `nested-back-player.png` show the selected nested screen, first system Back to Settings and second Back to the originating track-list view with the new deck applied. The saved production preference XML records `BlackoutPortable`.

Captured both list modes in portrait and landscape for Silverface Hi Fi and Blackout Portable. The `light-*` and `dark-*` PNGs show the gear in the bottom-right of the unchanged toggle, with both diagonal arrows visible. `nested-back-player.png` is the dark portrait track-list case. These are light and dark deck themes, not Android system night mode. `AndroidMixtapeTheme` currently always supplies `lightColorScheme()`.

The two `paused-roundtrip-*.txt` media-session dumps are byte-identical across rotation, toggling, a second nested deck edit from Blackout Portable to Silverface Hi Fi, and Back. They retain owner PID 2811, paused state 2, position 749 ms, active queue item 13, track metadata and queue size 56. This supplies runtime paused-state evidence, not playing-state evidence. Re-entering settings retained the selected theme. A final edit back to Blackout Portable was written to the attached preference XML.

Remaining before Review at that point: clean up the redundant `Back to Back` accessible label in the uncommitted Settings redesign and obtain an edited-settings round trip while playing a sufficiently long track. Existing automated playing-state command-history coverage remains valid. The theme matrix is visual evidence; it does not replace the earlier automated gesture tests. Stopped the owned emulator. No tunnel, build daemon or service was started, and unrelated working-tree edits remain untouched.

## Playing visual edit round trip

Redeployed the unchanged APK through kunlun-sync.sh on Kunlun API 24. Added a ten-minute quiet sine MP3 fixture titled Settings continuity 4834 and used the explicit Reset mixtapes action before starting this check so the new fixture entered a queue. That setup reset is outside the measured visual-settings round trip. Earlier exploratory captures were paused and were replaced, not counted as playing evidence.

The playing-before, playing-theme-selected, playing-back-settings and playing-back-player screenshots show Silverface Hi Fi changing to Blackout Portable, nested Back to Settings and Back to the same tape and track-list mode. The three playing-roundtrip dumps retain PID 3841, queue size 56, active item 32 and Settings continuity 4834. State stays 3 with speed 1.0. Positions advance from 15046 to 21076 to 24079 ms while update timestamps advance from 445054 to 451085 to 454086 ms. This samples uninterrupted progress across the edit, supported by the existing engine-command regression tests; it is not continuous audio instrumentation. Saved production preferences record BlackoutPortable.

No production or test bytes changed and no fresh build was needed. Stopped the owned emulator and removed the temporary local audio fixture. The emulator retains its test audio. Remaining before Review is the redundant Back accessibility label in the unrelated uncommitted Settings redesign. Preserve that redesign when fixing it.

## Handoff audit

The remaining label comes from `SettingsComponents.kt:64`, which builds `Back to $backLabel`, together with `MixtapeApp.kt:607`, which supplies `backLabel = "Back"`. `SettingsComponents.kt` is an untracked part of the existing settings redesign, not part of this card's committed implementation. Do not stage that entire redesign as a shortcut fix.

There is also a connected-test portability gap. `NowPlayingSettingsGestureTest.kt:55` requires the exact description `Back to Back`. The committed Settings screen instead has a text button labeled `Mix Tapes`, wired to the origin-aware callback. The previous isolated-tree unit run does not prove that this connected test passes against the committed UI. Before Review, give the root Settings Back button a stable semantic target in both layouts, remove the redundant accessible wording without importing unrelated changes, and rerun the connected gesture test against the resulting committed UI. Retain the already recorded playback and theme evidence, but do not describe it as an isolated-commit runtime test.

This audit changed documentation only. No fresh build, deployment or runtime verification was performed.


## Back target closure

Committed fb49d9d gives the original Settings layout an enabled, origin-neutral Back button with content description `Back`. The gesture test now targets that description instead of the redesign-only `Back to Back` wording.

A detached fb49d9d build passed all 257 unit tests. Kunlun API 24 passed both connected gesture and fresh-store persistence tests against that build. `committed-back/` contains logs, XML and gear/Settings/header-Back screenshots. The committed Gradle configuration still uses the historical `com.example.androidmixtape` ID. Deployment initially launched the existing org.puzzleduck.mixtape installation; those exploratory screenshots were replaced. The retained scoped screenshots were captured after an explicit historical-package launch through kunlun-sync.sh. Scoped APK SHA-256: 24a252a9440034176b86e3d0daacc0def7cc5e83a0314da71688996be8e1a0df. The initial test-only APK rejection was resolved by repackaging with android.injected.testOnly=false.

Also fixed the redundant label in the unrelated untracked SettingsComponents.kt, without committing that whole redesign. The exact one-line change is preserved in settings-redesign-back-label.patch and is already applied to the working tree. Rebuilt the current modern org.puzzleduck.mixtape APK, deployed through kunlun-sync.sh, and passed the connected gesture test against this layout too. Reinstalled after orchestration, granted emulator audio permission, and captured integration-settings.png and integration-back-player.png. These are current working-tree integration evidence, not isolated-commit screenshots.

Earlier nested visual-edit, playing/paused continuity and theme matrix evidence remains applicable; this pass changed Back wording and its test target only. The documented seven unrelated lint errors remain, with the earlier baseline comparison unchanged. No lint rerun or new continuity measurement is claimed.

All scoped implementation and test changes are committed. The integration-only label patch is committed as a patch so the unrelated redesign stays untracked. Stopped the owned Kunlun emulator, SSH tunnel and idle Gradle/Kotlin daemons, removed the detached worktree, and left the updated modern APK installed. Nothing pushed. Ready for Review.
