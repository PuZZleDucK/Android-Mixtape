# Card 2334 — Right cassette reel counterclockwise direction plan

## Goal

Fix the Now Playing cassette animation so both reels visibly spin counterclockwise while playback is active. The current left reel is already counterclockwise; the right reel is clockwise and should change to match the left.

## Current findings

- Main implementation: `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
- `CassetteTape(...)` computes `reelRotation` from a 16s infinite linear animation.
- Android/Compose canvas positive `rotate(degrees = ...)` appears clockwise on screen because the Y axis points downward.
- Current calls near `CassetteTape`:
  - left: `drawReel(... 0.31f ..., rotation = -reelRotation)` — visually counterclockwise.
  - right: `drawReel(... 0.69f ..., rotation = reelRotation)` — visually clockwise; this is the bug.
- Existing source contract test `app/src/test/java/com/example/androidmixtape/ui/CassetteReelGraphicsContractTest.kt` still encodes the previous expectation that reels rotate in opposite directions, so it must be updated with the implementation.
- Older planning doc `docs/CARD_2308_LEFT_REEL_DIRECTION_PLAN.md` intentionally preserved opposite directions; this new card supersedes that expectation for the right reel.

## Implementation plan for Running

1. In `CassetteTape(...)`, introduce or update readable local values:
   - `val leftReelRotation = -reelRotation`
   - `val rightReelRotation = -reelRotation`
2. Pass those values to `drawReel`:
   - left reel at `shell.width * 0.31f` uses `leftReelRotation`.
   - right reel at `shell.width * 0.69f` uses `rightReelRotation`.
3. Do not change `drawReel(...)` artwork, spindle/spoke drawing, animation duration, easing, cassette layout, transport controls, playback behavior, legacy UI, or accessibility semantics.
4. Update `CassetteReelGraphicsContractTest` so it asserts both reels use the negative animated rotation / counterclockwise direction. Rename the test from the old “opposite right reel” wording if needed.
5. If any previous comments/docs mention right reel should remain opposite, treat them as historical context only; the card requirement is now both reels counterclockwise.

## Success criteria

- While playing, both left and right cassette reels visibly rotate counterclockwise.
- While stopped/paused, both reels remain stable in the non-rotated state as before.
- The 16-second linear loop remains unchanged.
- Accessibility semantics remain `Cassette reels spinning` and `Cassette reels stopped`.
- No unrelated UI changes to cassette art, labels, symbols, track list, library spines, transport buttons, or legacy UI.
- JVM/source contract tests pass after updating the direction expectation.

## Suggested verification

- Run the relevant source/unit contract test, e.g. `./gradlew testModernDebugUnitTest` or the narrow Gradle test task available for `CassetteReelGraphicsContractTest`.
- If an emulator/device is available, launch the modern app, start playback, and visually confirm both asymmetric spindle details move counterclockwise.
- Optional evidence path if visual verification is captured: `docs/evidence/card2334-right-reel-counterclockwise-*`.

## Risks

- Changing only the implementation without updating the old contract test will leave a false regression failure because the test currently expects opposite directions.
- A perfectly symmetric reel can make direction hard to see; rely on the existing asymmetric spindle detail/groove for visual confirmation.
- Avoid flipping the sign inside `drawReel(...)`; that would invert every caller and make future direction intent harder to reason about.
