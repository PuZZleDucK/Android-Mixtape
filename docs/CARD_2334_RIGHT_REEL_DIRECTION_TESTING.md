# Card 2334 — Right reel direction testing notes

## Expected behavior under test

- In `CassetteTape(...)`, the left reel at `0.31f` must continue to use negative animated rotation (`-reelRotation`) so it appears counterclockwise on Android/Compose's clockwise-positive canvas.
- The right reel at `0.69f` must also use negative animated rotation (`-reelRotation`) so both reels appear counterclockwise.
- The right reel must not keep the old positive `reelRotation` path, which made it spin clockwise/opposite the left reel.
- The existing 16-second `LinearEasing` animation loop and the `Cassette reels spinning` / `Cassette reels stopped` accessibility semantics must remain unchanged.

## TDD contract added

Updated `app/src/test/java/com/example/androidmixtape/ui/CassetteReelGraphicsContractTest.kt`:

- Renamed the first contract test to `cassetteTapeUsesAnticlockwiseRotationForBothReels`.
- Changed the right-reel expectation from positive `reelRotation` to negative `-reelRotation`.
- Added an explicit guard that fails if the right reel continues to use the old positive rotation.

## Current failing evidence before implementation

Command:

```sh
PATH="$HOME/.asdf/shims:$HOME/.asdf/bin:$PATH" \
ANDROID_HOME="$HOME/Android/Sdk" \
./gradlew :app:testModernDebugUnitTest --tests com.example.androidmixtape.ui.CassetteReelGraphicsContractTest.cassetteTapeUsesAnticlockwiseRotationForBothReels
```

Result before code fix:

```text
CassetteReelGraphicsContractTest > cassetteTapeUsesAnticlockwiseRotationForBothReels FAILED
    java.lang.AssertionError at CassetteReelGraphicsContractTest.kt:36

1 test completed, 1 failed
```

This is the expected failing spec for the Running stage. The implementation should make the right reel use negative rotation, then rerun the same command and preferably the full `:app:testModernDebugUnitTest` task.
