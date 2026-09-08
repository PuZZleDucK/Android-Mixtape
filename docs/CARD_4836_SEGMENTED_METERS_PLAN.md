# Card 4836 segmented meters

## Scope and source findings

Select Blackout Portable and Sunset Boombox only. Blackout's dark charcoal chassis and orange-red accent suit an LED ladder. Sunset's cream/brown chassis and burnt-orange accent suit an amber electronic ladder. Preserve the continuous meters on Silverface HiFi, Navy Micro, Crimson Metal, Safety Yellow and Graphite Slim. Do not change counters or volume controls.

Local references inspected:
- `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`, `DemoDeckPlayer` and `DeckLevelMeter`, around lines 2860–3000. Both channels already receive separate real levels. The shared meter currently animates a continuous fill over 70 ms.
- The same file's `deckPalette` maps Sunset to cream/brown with #D3542E accent and Blackout to charcoal with #FF5533 accent. Preserve these identities.
- `app/src/modern/java/com/example/androidmixtape/playback/AudioMeterScale.kt` maps PCM RMS logarithmically over -30 to 0 dB into 0..1. Do not apply another dB transform.
- `AudioLevelMonitor.kt` measures channel RMS, attacks immediately, releases by a 0.82 multiplier per processed buffer, and publishes at intervals of at least 33 ms. Additional long animation would obscure musical response.
- `app/src/test/java/com/example/androidmixtape/ui/DeckAudioLevelMeterContractTest.kt` already guards stereo wiring. Keep it passing when adding a theme argument.
- Existing reference `docs/evidence/nothing-landscape-spine/Screenshot_20260908-205302.png` and the card's attached Blackout screenshot establish current continuous-meter bounds. They are references, not evidence of this change.

## Behavior contract

Add pure helpers in the modern UI package, separate from Compose:
- `DeckTheme.usesSegmentedMeter(): Boolean` selects exactly the two skins.
- `litMeterSegments(level: Float?, isPlaying: Boolean, previousCount: Int): Int` returns 0..10.

Ten equal thresholds at 0.1 through 1.0 use the existing normalized audio signal. With no previous lights, 0.25 lights two, 0.55 five, 0.95 nine, and 1.0 ten. Light bottom cells first. Clamp finite inputs to 0..1; null, nonfinite values, nonpositive levels and inactive playback return zero even with previousCount=10. Clamp previousCount to 0..10.

Use a 0.02 normalized hysteresis band on falling levels only. A lit cell at threshold t stays lit at t-0.02 and extinguishes below it. Previously unlit cells require t. Compare using the same Float threshold arithmetic to avoid boundary rounding differences. At 0.49 a fifth light remains on; at 0.479 it switches off. A drop to 0.25 from full scale leaves two. Zero always clears. This is amplitude hysteresis, not a timer or artificial signal.

Keep previous counts separately for L/R. Reset on inactive playback, absent data, track replacement and theme changes. Consume the existing monitor; audit engine reset paths for pause, stop, ended, errors and unsupported/no PCM. Do not invent motion when data is missing. The current release envelope may decay toward zero during silence; the first cell must extinguish below 0.08. Paused UI should clear without waiting for another PCM callback.

## Rendering

Pass theme selection to the existing meter without changing its position, label, bounds or deck controls. Use a Canvas ladder within the existing black well, reserving symmetric inner padding. Divide available height into ten equal slots; give each gap about 20% of slot height so small landscape decks retain visible gaps without negative cell sizes. Keep fill width comparable to the current 58% strip. Draw all ten unlit cells first, then illuminate the bottom count. Use opaque muted warm cells for unlit contrast rather than disappearing black-on-black cells. Use the skin accent for the lower eight cells and a restrained brighter amber/red for the top two. No glow initially; add only if still visible at the smallest reviewed size and contained in the well.

Retain L/R labels and percent accessibility descriptions; optionally expose lit count in semantics for instrumentation. Do not interpolate the segment count with animateFloatAsState. Leave nonselected continuous rendering and its existing animation unchanged.

## TDD evidence and next steps

Added `app/src/testModern/java/com/example/androidmixtape/ui/SegmentedDeckMeterTest.kt` before implementation. It specifies opt-in selection, all ten threshold boundaries, low/mid/high counts, falling hysteresis, clamping, nonfinite/missing/inactive inputs, stereo separation and pause reset.

Executed `./gradlew testModernDebugUnitTest --tests '*SegmentedDeckMeterTest'` with the installed Java 21 path and Android SDK environment. Result: `:app:compileModernDebugUnitTestKotlin FAILED` because `usesSegmentedMeter` and `litMeterSegments` do not exist yet. This is a compile-red specification, not an assertion failure or a passing suite. Full disposable log: `.work/card-4836/red-test.log`. Initial invocation lacked Java on PATH; rerun used `/home/puzzleduck/.asdf/installs/java/temurin-21.0.10+7.0.LTS`.

Implementation order:
1. Implement the pure policy helpers and make these tests green.
2. Wire independent channel state and ladder rendering only for the selected skins. Add focused Compose checks for ten cells, bottom-first illumination and resets; pure count tests alone cannot establish visual wiring.
3. Run full modern unit tests, lint and an x86_64-compatible debug build. Preserve all unrelated existing working-tree edits.
4. Deploy the changed APK using `kunlun-sync.sh` to the compatible Kunlun emulator. No app code changed in Planning, so updated-app deployment and visual acceptance remain implementation-stage work, required before Review.
5. Seed a short real PCM fixture with silence, left-only, right-only and low/mid/high passages. Drive actual playback rather than substituting random levels. Capture a short recording or timestamped frame sequence showing rising and falling lights, pause, resume, stop and track replacement. Record fixture amplitudes and observe published levels, allowing for the existing RMS envelope.
6. Capture both skins in portrait/landscape and light/dark, including the smallest supported deck size. Inspect gaps, unlit contrast, highest and lowest cells, labels and unchanged touch targets. Include a nonselected-skin comparison to prove its continuous style remains intact. Missing-data checks can use a controlled monitor reset/instrumented path, clearly identified as such.

Attach the two selected-skin stills and playback sequence to the card before Review. Store durable image/video evidence through Git LFS per repository policy; use ignored `.work/` for fixtures, intermediates and temporary captures. No generated art or external design assets are needed. Emulator storage exhaustion is an environment limitation, not a product failure; record free /data, APK size and install output if encountered.
