# Card 2236 — Cassette reel graphics test plan

## TDD contract added

Added `app/src/test/java/com/example/androidmixtape/ui/CassetteReelGraphicsContractTest.kt` to lock the intended modern Compose cassette reel behavior before implementation.

The contract verifies:

1. `CassetteTape` keeps the existing slow 16s linear animation, left/right opposite reel rotations, and `Cassette reels spinning` / `Cassette reels stopped` semantics.
2. `drawReel` keeps using `rotate(degrees = rotation, pivot = center)` for reel artwork.
3. Spokes become chunky black/near-black molded spokes:
   - materially thicker than the current `5f` line width, accepting direct or named `8f`–`10f` stroke widths;
   - no longer leave the old `Color(0xFF2B2522)` + `strokeWidth = 5f` as the only/main spoke treatment;
   - include layered line/shadow/highlight/emboss source intent.
4. Center spindle becomes a retro beige/tan/cream hub:
   - named as spindle/hub for maintainability;
   - drawn above the spokes;
   - includes an asymmetric slot/notch/groove/dot in the rotation block so it visibly rotates with the spokes.

## Expected initial failure

Current source is expected to fail the new contract because `drawReel` still draws six thin brown `5f` spokes and a plain symmetric light center circle before the rotating spoke layer.

## Verification commands

Run the focused failing contract first:

```sh
cd /home/puzzleduck/x/android-mixtape
./gradlew testModernDebugUnitTest --tests 'com.example.androidmixtape.ui.CassetteReelGraphicsContractTest'
```

After implementation, run broader regression checks:

```sh
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
```

If a modern Android device/emulator is available, perform visual smoke on Now Playing while audio is playing and save evidence under `docs/evidence/card2236-*`.
