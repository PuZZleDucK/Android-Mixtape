# Card 2308 — left reel direction TDD test plan

## Expected behavior

- In Now Playing playback, the left cassette reel uses negative animated rotation (`-reelRotation`) so it appears anticlockwise/counterclockwise on Android/Compose's clockwise-positive canvas.
- The right cassette reel uses positive animated rotation (`reelRotation`) so it remains opposite the left reel.
- The 16-second `LinearEasing` animation loop remains unchanged.
- Existing reel accessibility semantics remain `Cassette reels spinning` while playing and `Cassette reels stopped` while paused/stopped.
- Reel artwork/spindle/spoke polish from card 2236 is not changed by this bug fix.

## Spec updated

- `CassetteReelGraphicsContractTest.cassetteTapeUsesAnticlockwiseLeftReelAndOppositeRightReel`
  - Replaces the old left-positive/right-negative source contract.
  - Accepts either direct signed draw calls or clear `leftReelRotation` / `rightReelRotation` local values.
  - Requires the left reel at `shell.width * 0.31f` to be negative and the right reel at `shell.width * 0.69f` to be positive.

## Verification performed

```sh
export PATH="$HOME/.asdf/shims:$PATH"
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew testModernDebugUnitTest --tests 'com.example.androidmixtape.ui.CassetteReelGraphicsContractTest'
```

Result: fails as intended before implementation. The failure is at the new left-reel anticlockwise assertion because current source still passes `rotation = reelRotation` to the left reel and `rotation = -reelRotation` to the right reel.

## Running implementation guidance

- Make the minimal sign swap in `CassetteTape`: left `rotation = -reelRotation`; right `rotation = reelRotation`.
- Do not change animation duration/easing, semantics, reel layout, or reel artwork.
- After implementation, run the focused contract test, then the broader modern/legacy unit tests listed in the planning document.
