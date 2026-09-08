# Gesture verification, card 4834

The Kunlun API 24 emulator passed `NowPlayingSettingsGestureTest` on 2026-09-09 local time. The test uses injected Compose touch events for short taps, cancellation and long presses in both body modes. It checks that long press returns to the same mode without an accidental toggle, exercises system Back and the Settings header Back, and invokes the accessibility long-click semantics action in both modes. Four Settings openings are expected and observed.

The first run failed because the test searched for text `Back` instead of the header's accessible description `Back to Back`. The corrected run passed. That redundant accessible label should be cleaned up before Review.

`testModernDebugUnitTest` passed with 272 tests and no failures or errors. The first combined build exceeded the command timeout after assembling both APKs. The subsequent unit/lint invocation completed the unit task but failed lint with seven errors in Android Auto diagnostics and the media library service, outside this card's changed code. See `full-unit-lint.log`. No lint baseline or suppression was added.

The APK was deployed through `kunlun-sync.sh` before connected testing. Connected testing removed its installed app afterward, so the APK was installed again through the script. The three `resumed-*.png` screenshots show the gear, Settings opened by a long press, and system Back returning to the track list. Playback advanced during this manual round trip; these images are navigation evidence, not an assertion that the same track remained active.

Still needed before Review: theme/orientation matrix, nested edited-settings runtime evidence, fresh-store persistence and stronger engine-command assertions, isolation and commit of the preceding worker's production changes, and resolution or explicit baseline comparison of unrelated lint errors. The existing `.work/card-4834/baseline` and `owned.patch` remain available for isolating those changes.

The run stopped its Kunlun emulator, SSH ADB tunnel and idle build daemons.
