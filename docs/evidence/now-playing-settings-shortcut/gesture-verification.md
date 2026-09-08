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
