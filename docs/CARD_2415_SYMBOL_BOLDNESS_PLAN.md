# Card 2415 — Bolder mixtape symbols plan

## Goal

Make every hand-drawn mixtape symbol read as bolder cassette-label marker art. All symbols should have thicker strokes and/or solid fills so they remain visible in the 44.dp cassette spine, 96.dp Now Playing cassette label, settings previews, and the long-press selector.

## Current implementation touchpoints

- `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `HandDrawnEmbellishment(...)` is the shared Canvas renderer for all symbols.
  - It currently uses `val strokeWidth = 1.6.dp.toPx()` and a shared `Stroke(width = strokeWidth, cap = StrokeCap.Round)`.
  - Existing helper functions `line(...)` and `path(...)` pass the same thin stroke to most symbols.
  - The twenty branches currently cover: `Star`, `Heart`, `LightningBolt`, `Sparkles`, `Smiley`, `Flower`, `MusicNote`, `Moon`, `Swirl`, `Crown`, `QuarterNote`, `EighthNote`, `BeamedEighthNotes`, `SixteenthNote`, `WholeNote`, `HalfNote`, `TrebleClef`, `BassClef`, `SharpSign`, and `FlatSign`.
- `app/src/test/java/com/example/androidmixtape/ui/HandDrawnEmbellishmentUiContractTest.kt`
  - Already asserts the reusable Canvas renderer and branch coverage.
  - Best place to add a source contract requiring explicit bolder ink/stroke/fill behavior.

## Implementation plan

1. Add a failing source-level contract test for bolder symbols.
   - Extract `HandDrawnEmbellishment` and assert the shared stroke is clearly above the old `1.6.dp` value, for example `2.8.dp` or `3.dp`.
   - Assert the renderer uses an explicit bold/filled style helper rather than relying only on thin outline strokes.
   - Keep the test focused on `HandDrawnEmbellishment` so reel, button, and cassette-outline strokes are not accidentally constrained.
2. Update `HandDrawnEmbellishment` shared drawing style.
   - Increase the base symbol stroke from `1.6.dp` to roughly `2.8.dp`/`3.dp`.
   - Add round joins if supported by the current Compose graphics version so path corners stay marker-like.
   - Keep `StrokeCap.Round` for all line endpoints.
3. Add solid fills or heavier passes where outlines alone still look weak.
   - Filled noteheads: `MusicNote`, `QuarterNote`, `EighthNote`, `BeamedEighthNotes`, and `SixteenthNote` should use filled ellipses/circles or filled path heads rather than outline-only heads.
   - Heavy geometric marks: `Star`, `LightningBolt`, `Crown`, and `SharpSign` can remain hand-drawn but should use the thicker shared stroke; fill only if it does not make them unreadable at 44.dp.
   - Dots/eyes: enlarge `Smiley` eyes and `BassClef` dots slightly if the global stroke makes them look too small by comparison.
   - Open-note symbols (`WholeNote`, `HalfNote`) should stay recognizable as open notes; use thicker outlines plus an interior slash, not full solid fill.
4. Preserve existing data flow and sizes.
   - Do not rename or reorder `MixtapeEmbellishment` enum values.
   - Do not change the 44.dp spine symbol size or 96.dp Now Playing symbol size from card 2335.
   - Do not alter settings persistence, enabled-symbol logic, long-press selector behavior, playback, or tape skins.
5. Run unit/source tests first, then assemble both variants.
6. Optional manual visual check on a compatible device/emulator: compare Settings > Mixtape symbols and Now Playing before/after; every symbol should read as bold marker ink without clipping.

## Success criteria

- `HandDrawnEmbellishment` uses a visibly thicker shared stroke than the current 1.6.dp linework.
- All twenty symbols render bolder; line-only symbols use thicker strokes and notehead/dot symbols use solid fills or enlarged filled marks where appropriate.
- Symbols remain within their Canvas bounds and do not clip at 44.dp, 96.dp, settings preview, or selector sizes.
- Existing persisted symbol names/order and all symbol settings/selector behavior remain unchanged.
- Modern and legacy unit tests pass, and both debug APK variants assemble.

## Suggested verification commands

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```

## Manual acceptance checklist

1. Settings > Mixtape symbols shows all twenty symbols with noticeably thicker marker strokes.
2. Now Playing cassette label symbol is bold at 96.dp and not clipped.
3. Library spine symbol is bold at 44.dp and still leaves room for the mixtape title to ellipsize cleanly.
4. Filled noteheads look intentional and do not obscure stems/flags.
5. Open noteheads (`WholeNote`, `HalfNote`) remain open-note shapes, just heavier.
6. Long-press selector still previews and selects every enabled symbol.

## Risks and guardrails

- A single global stroke increase may make dense symbols such as `TrebleClef`, `BassClef`, and `Sparkles` crowded; adjust only their coordinates/dot sizes if needed, not unrelated UI.
- Full solid fills can destroy recognizability for open-note symbols; reserve fills for symbols that are normally solid or currently too faint.
- Avoid adding bitmap assets, emoji, or font glyphs; keep the existing Canvas-drawn handmade style.
- Avoid touching generated build outputs, APKs, playback logic, storage/scanning logic, or model/title-generation behavior.

## Creative direction / search terms

- `bold marker doodle music note icon`
- `filled notehead hand drawn icon`
- `thick line cassette label doodle symbols`
- `Jetpack Compose Canvas Stroke width round cap drawPath fill`
- `music notation icon simplified filled notehead`
