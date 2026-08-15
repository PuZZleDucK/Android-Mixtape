# Card 2333 control button layout plan

## Goal

Make the Now Playing Walkman transport controls place Rewind to the left of Stop.

## Current state

- Modern Compose UI renders controls in `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`, inside `WalkmanTransportControls`.
- Current left-to-right order is: Eject, divider, Stop, Play, Pause, Rewind, Fast-forward.
- Existing modern connected test `MixtapeAppTest.nowPlayingTransportControlsPlaceEjectAtFarLeftAndStopBeforePlay` currently asserts Rewind is after Pause, so it must be updated.

## Proposed implementation

1. Add/update a modern Compose UI contract test in `app/src/androidTestModern/java/com/example/androidmixtape/ui/MixtapeAppTest.kt`:
   - Preserve the 320dp Now Playing layout harness.
   - Assert `Eject < divider < Rewind < Stop < Play < Pause < Fast-forward` by comparing content-description bounds.
   - Keep the existing Eject/divider separation assertion.
   - Keep click callback coverage in `nowPlayingRendersCassetteWalkmanTransportAndCompleteJCardTrackList` unchanged.
2. Change only the button order in `WalkmanTransportControls`:
   - Move `WalkmanButton(label = "⏪", description = "Rewind", ...)` from after Pause to immediately after `TransportControlDivider()` and before Stop.
   - Leave callbacks, labels, enabled states, and accessibility descriptions unchanged.
3. Verify with targeted checks before broader build:
   - `./gradlew :app:compileModernDebugAndroidTestKotlin`
   - `./gradlew :app:connectedModernDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.androidmixtape.ui.MixtapeAppTest#nowPlayingTransportControlsPlaceEjectAtFarLeftAndStopBeforePlay` when a device/emulator is available.
   - If connected hardware is unavailable, run the compile target and document the missing-device limitation for Testing.

## Success criteria

- In Now Playing, Rewind is visually and semantically to the left of Stop on compact portrait layouts.
- Eject remains isolated at far left by the transport divider.
- Stop remains before Play, Play before Pause, and Fast-forward after the main playback controls.
- Existing Rewind, Stop, Play/Pause, Fast-forward, and Eject callbacks still fire from their respective buttons.
