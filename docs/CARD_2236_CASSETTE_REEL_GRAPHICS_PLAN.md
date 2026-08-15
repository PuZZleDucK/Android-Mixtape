# Card 2236 — Cassette reel spokes and retro spindle graphics plan

## Goal

Polish the modern Now Playing cassette visualization so each reel has visibly thicker black spokes with a subtle embossed/plastic depth effect, and the center hub reads as a warm retro beige spindle that participates in the same reel rotation animation.

This is a modern Compose/Canvas visual pass. The legacy API 19 Views UI does not need visual parity unless a later card requests it.

## Current implementation touchpoints

- Main file: `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
- `CassetteTape(...)` computes `reelRotation` from `rememberInfiniteTransition` when `isPlaying` is true and calls:
  - `drawReel(center = Offset(shell.width * 0.31f, shell.height * 0.63f), rotation = reelRotation)`
  - `drawReel(center = Offset(shell.width * 0.69f, shell.height * 0.63f), rotation = -reelRotation)`
- `drawReel(...)` currently draws symmetric circles and six thin dark-brown spokes with `strokeWidth = 5f`.
- Existing accessibility semantics already expose `Cassette reels spinning` / `Cassette reels stopped`; keep those unchanged.

## Visual direction

- Spokes:
  - Make the six spokes clearly black/near-black, e.g. `Color(0xFF11100F)` or `Color.Black` softened only enough to fit the warm shell palette.
  - Increase spoke stroke width materially, likely from `5f` to about `8f–10f` relative to the current reel scale.
  - Prefer rounded line caps for chunky molded-plastic spokes if practical (`StrokeCap.Round`).
  - Add subtle emboss by layering a low-contrast highlight/shadow: for example draw a slightly larger dark shadow first, then the black spoke, plus a tiny warm highlight offset along one side. Keep it subtle so the spokes do not look blurry.
- Center spindle:
  - Replace the current plain light center circle with a retro beige spindle/hub palette: aged cream, tan rim, small darker center or slot.
  - Add an asymmetric detail inside the hub, such as a small notch/slot/dot or short radial groove, so the center visibly rotates rather than looking static because it is a perfect circle.
  - Draw that asymmetric hub detail inside the same `rotate(degrees = rotation, pivot = center)` block as the spokes, or otherwise rotate it by the same signed `rotation` value.
  - Keep the hub readable against the black spokes; the beige spindle should sit above the spokes, not be hidden under them.
- Motion:
  - Preserve opposite reel directions (`rotation` and `-rotation`) and the existing slow 16s linear loop.
  - When stopped, both reels should return to the non-rotated visual state without jitter or stateful offsets.

## Suggested implementation steps

1. In `drawReel(...)`, rename/readability-factor the radii if helpful: `outerRadius`, `innerWindowRadius`, `spindleRadius`, `spokeEndRadius`.
2. Keep the existing outer/tape rings or only tune their colors lightly; avoid changing cassette shell layout for this card.
3. Move the spoke drawing into a small local loop that can draw layered lines:
   - first shadow/emboss line, slightly thicker and offset by about `1f`, muted dark brown/black with alpha if available;
   - then the main black spoke at `8f–10f`;
   - optionally a very small tan highlight line offset by `-0.75f` with low contrast.
4. Add `StrokeCap.Round` import/use if the chosen line style uses rounded caps.
5. Draw the beige spindle after the spoke layer so it remains visible:
   - outer beige/tan disk/rim;
   - inner cream disk;
   - rotating slot/dot/groove inside the hub.
6. Put the asymmetric spindle detail inside the same `rotate(...)` block as the spokes. If the hub disks are symmetric they can be outside, but the slot/dot must rotate with the spokes.
7. Add or extend a JVM source contract test in `app/src/test/java/com/example/androidmixtape/ui/` to guard the intent without brittle pixel matching:
   - `drawReel` still accepts/uses `rotation` and wraps a `rotate(degrees = rotation, pivot = center)` block;
   - the spoke stroke width is materially thicker than the old `5f` (test can look for `8f`, `9f`, `10f`, or a named `spokeStrokeWidth` value);
   - the spoke color uses near-black/black, not the old brown `0xFF2B2522` as the main spoke color;
   - the spindle/hub includes warm beige/tan colors and at least one asymmetric rotating detail such as `slot`, `notch`, `groove`, or `spindle` code.
8. Run unit/build verification and capture a modern screenshot if a device/emulator is available.

## Success criteria

- In Now Playing, both cassette reels show noticeably thicker black spokes than the current thin brown lines.
- The spokes have a subtle depth/emboss effect without visual noise or blur.
- The center hub reads as a retro beige spindle rather than a flat white/gray circle.
- At least one center-spindle detail rotates in sync with the spokes, and the left/right reels continue to rotate in opposite directions while playing.
- Stopped state remains stable and accessible semantics remain `Cassette reels stopped`.
- No intentional change to transport controls, cassette labels, handwriting renderer, library spine list, playback behavior, permissions, or legacy UI.
- Modern and legacy unit/build checks still pass.

## Verification checklist

```sh
cd /home/puzzleduck/x/android-mixtape
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
```

If a modern emulator/device is available with audio seeded:

```sh
./gradlew connectedModernDebugAndroidTest
```

Manual visual smoke:

1. Install/launch the modern debug APK on an Android 7+ device/emulator.
2. Grant audio permission and start playback on any track.
3. Confirm both reels spin slowly in opposite directions.
4. Confirm spokes are thick, black, and lightly embossed.
5. Confirm the beige spindle/hub detail visibly rotates with each reel.
6. Pause/stop and confirm the reels stop cleanly without layout/accessibility regressions.
7. Save evidence under `docs/evidence/card2236-*` if available.

## Search/reference terms

- `vintage cassette reel hub beige spindle`
- `audio cassette reel black spokes close up`
- `retro cassette tape reel center spindle`
- `molded plastic cassette reel spokes emboss`
- `transparent cassette reel black spokes beige hub`

## Risks / guardrails

- Avoid making the spokes so thick they cover the reel holes or look like a solid disk on small phone screens.
- Avoid high-contrast highlights that read as extra spokes or shimmer during animation.
- Do not change the animation duration/easing unless visual QA shows the thicker spokes create distracting motion.
- Keep source tests resilient; prefer named constants and intent checks over fragile whole-function snapshots.
