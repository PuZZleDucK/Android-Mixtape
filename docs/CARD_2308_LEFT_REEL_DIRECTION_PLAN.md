# Card 2308 — Left cassette reel anticlockwise direction plan

## Problem

The Now Playing cassette animation currently makes the left reel rotate in the wrong visual direction. In Compose/Android canvas coordinates, a positive `rotate(degrees = ...)` appears clockwise on screen because the Y axis points downward. The user expectation for the left reel is anticlockwise/counterclockwise while playing.

## Current implementation touchpoints

- Main source: `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
- `CassetteTape(...)` computes `reelRotation` from a 16s `rememberInfiniteTransition` when `isPlaying` is true.
- Current reel calls:
  - left: `drawReel(center = Offset(shell.width * 0.31f, shell.height * 0.63f), rotation = reelRotation)`
  - right: `drawReel(center = Offset(shell.width * 0.69f, shell.height * 0.63f), rotation = -reelRotation)`
- Existing contract test to update: `app/src/test/java/com/example/androidmixtape/ui/CassetteReelGraphicsContractTest.kt`, currently asserts the old left-positive/right-negative signs.

## Plan

1. In Testing, update/add a source contract that encodes the intended direction:
   - left reel uses negative animated rotation (`rotation = -reelRotation`) so it appears anticlockwise;
   - right reel remains opposite (`rotation = reelRotation`) unless product explicitly requests both reels anticlockwise;
   - the 16s linear loop and `Cassette reels spinning` / `Cassette reels stopped` semantics stay unchanged.
2. In Running, make the minimal implementation change in `CassetteTape(...)` by swapping the two signed rotation arguments.
3. Prefer readable local values if helpful, for example `val leftReelRotation = -reelRotation` and `val rightReelRotation = reelRotation`, so future tests/docs describe direction rather than relying on inline signs.
4. Do not alter `drawReel(...)` artwork, spoke colors, spindle graphics, layout, playback state, transport controls, library spines, or legacy UI.

## Success criteria

- When playback is active, the left cassette reel visibly spins anticlockwise/counterclockwise.
- The right reel still spins opposite the left reel.
- When stopped/paused, both reels return to the stable non-rotated state as before.
- The existing cassette visual polish from card 2236 remains intact.
- Modern unit/source contract tests pass after updating expectations, and legacy tests/build are not regressed.

## Verification checklist

```sh
cd /home/puzzleduck/x/android-mixtape
./gradlew testModernDebugUnitTest --tests 'com.example.androidmixtape.ui.CassetteReelGraphicsContractTest'
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
```

If a modern device/emulator is available, start playback in Now Playing and capture/observe the left reel moving anticlockwise; save evidence under `docs/evidence/card2308-*`.

## Risks / guardrails

- Do not change animation duration/easing to fake the direction fix.
- Avoid touching symmetric reel artwork; the direction issue is the sign passed into `drawReel`.
- The old contract wording says "left positive"; update that wording so it does not reintroduce this bug.
