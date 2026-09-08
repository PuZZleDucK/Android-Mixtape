# Card 4834: Now Playing settings shortcut

## Source findings

Inspected the supplied Screenshot_20260908-205302.png. It shows the two diagonal arrows beside the spine, with space at the button's bottom-right for a small gear. Keep both arrows recognizable. No generated bitmap asset is needed.

Current modern MixtapeApp.kt:
- DeckViewToggle uses a 48.dp Canvas with 7.dp padding and clickable. The footer reserves 64.dp in its width calculations. Preserve these existing layout dimensions rather than resizing the footer based on its comments.
- NowPlayingModeToggleAndSpine is shared by portrait and landscape. Thread onShowSettings through NowPlaying and that footer to DeckViewToggle.
- NowPlaying owns bodyMode using rememberSaveable. Leaving its conditional composition for Settings can discard that state. Hoist the mode above the screen switch or use a SaveableStateHolder. Preserve the originating mode as well as the current tape.
- Root Settings BackHandler calls onBackToMixTapes. Nested screens call onShowSettings. Toolbar and system Back need the same origin-aware exit.
- MainActivity binds onShowSettings to viewModel.showSettings and onBackToMixTapes to backToMixTapes. Do not repurpose the latter globally because Eject/library navigation still needs its current behavior.

Current shared MixtapeViewModel.kt:
- showSettings only changes the screen through controller.toUiState. It records no return destination.
- showMixTapes/backToMixTapes always select the library.
- updateDeckTheme and saveThemeSettings persist settings and refreshCurrentUiState, without rebuilding playback. Keep this path. Audit other ordinary visual setters, including handwriting, for the same property.

Existing coverage to retain: NowPlayingMixTapeToggleContractTest, MixTapeQueueViewModelTest, PlaybackContinuityContractTest, modern MixtapeAppTest, SettingsUiTest and NowPlayingLayoutTest. Several are already modified by other work. Do not overwrite them.

## Implementation plan

1. Give the entire 48.dp toggle one combinedClickable with the original onClick and a separate onLongClick opening Settings. Set onLongClickLabel to Open Settings and role to Button. Keep the existing state-dependent short-click description. Use Compose gesture arbitration rather than a timer alongside clickable, which risks firing both callbacks on release.
2. Overlay a decorative gear, initially about 12 to 14.dp, bottom-right inside the existing button bounds. Use theme palette contrast and a small backing if necessary. It must not intercept input or add a second accessibility focus stop. Keep the arrows visible. Use a vector or Canvas rather than an image download.
3. Track a Settings return destination in the viewmodel. Capture it only on entry from outside the Settings subtree. Returning from nested Settings must not replace NowPlaying with Settings as the origin. Add a dedicated exitSettings callback for toolbar/system Back. Library-origin Settings must still return to the library. Clear or replace stale origins on a later independent entry.
4. Retain NowPlaying's bodyMode while its screen is absent. Do not restore an old playback snapshot on Back. Playback can advance while Settings is open. Navigation and visual setters must never call prepare, setQueue, seek, stop or play merely to reconstruct the screen.
5. Add behavioral tests below, then make the planning source checks pass. Names in those checks describe the proposed callback plumbing, not an excuse to substitute string presence for behavior.

## Regression cases and acceptance

- Both body modes: short touch toggles exactly once and never opens Settings. Long touch followed by release opens Settings exactly once and never changes mode. Return preserves the original mode. Also exercise the labeled semantic long-click action.
- Test touch cancellation or drag out of bounds without an accidental short tap. Use Compose performTouchInput longClick and click, not only semantic performClick.
- Direct Settings Back and toolbar Back return to NowPlaying. Nested Deck theme -> Back -> Settings -> Back -> NowPlaying. Repeat for a second nested screen. Library -> Settings -> Back remains Library. Reopen after these flows to catch stale return origins.
- With a fake engine, capture active tape identity, queue IDs, track index, nonzero position and playing state. Navigate, edit a deck theme and handwriting setting, then return. Queue/tape/index stay identical. Paused position stays identical; playing position may advance normally but must not reset. Assert no additional queue preparation, stop, seek or transport commands. Cover playing and paused separately.
- Assert store writes and read the changed values through a fresh store/viewmodel. Persistence testing must not imply that playback must survive a process restart, which is outside this card.
- Existing explicit reset/destructive actions keep their intended behavior. Do not apply continuity assertions to intentional reset.
- Kunlun UI matrix: portrait and landscape, both body modes, light and dark deck themes. Gear fits the original button, has visible contrast, arrows remain legible, and the full control remains the target. Rotate while in Settings and return to check retained origin and mode.
- Capture updated gear screenshots and a NowPlaying -> nested Settings with edit -> Settings -> NowPlaying sequence. Include runtime/test evidence of continuity because screenshots alone cannot prove it.

## Red evidence and handoff

Added NowPlayingSettingsShortcutContractTest without modifying production code. Real execution on 2026-09-08:

```sh
JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS" \
ANDROID_HOME="$HOME/Android/Sdk" \
./gradlew testModernDebugUnitTest --tests '*NowPlayingSettingsShortcutContractTest' --console=plain
```

All three tests compiled and failed on assertions in 73 seconds:
- toggleUsesOneCombinedGestureTargetWithLabeledLongPress: missing combinedClickable.
- nowPlayingThreadsSettingsActionToSharedPortraitLandscapeFooter: missing Settings callback.
- settingsBackMustNotBeHardwiredToLibrary: root Back is hardwired to MixTapes.

Raw log: .work/card-4834/red-tests.log. JUnit report: app/build/test-results/testModernDebugUnitTest/TEST-com.example.androidmixtape.ui.NowPlayingSettingsShortcutContractTest.xml. These are source guardrails, not executed gesture/navigation regressions. Add those behavioral tests during implementation before changing the relevant behavior.

The first command could not find Java in the worker environment; the documented project JDK path resolved it. Compilation reported a Kotlin daemon startup retry but reached all three intended failing assertions.

Planning stops before production changes. Build the updated emulator-compatible modern APK, run focused and full unit checks plus lint, deploy using kunlun-sync.sh, and run connected gesture/navigation coverage on Kunlun before Review. No updated UI or deployment is claimed at this stage. Preserve unrelated changes. Commit only card-owned changes when implementation is complete; do not push. Store durable screenshot evidence through Git LFS and disposable captures under .work/card-4834. Stop idle card-owned build daemons after verification.
