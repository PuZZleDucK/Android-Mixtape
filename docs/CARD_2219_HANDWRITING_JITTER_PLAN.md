# Card 2219 — handwriting jitter renderer plan

## Dependency check

The handwritten fonts dependency has landed and is usable as the base layer for this card:

- All eight TTFs exist under `app/src/modern/res/font/`.
- `app/src/modern/java/com/example/androidmixtape/ui/HandwrittenCassetteFonts.kt` maps `MixtapeHandwritingFont` to Compose `FontFamily(Font(R.font...))` and applies readable effective weights.
- `MixtapeApp.kt` already passes `group.handwritingFont` / `state.currentMixtapeHandwritingFont` through the cassette spine, cassette tape, and cassette cover track list.
- Verification run: `JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest` passed on 2026-07-01.

## Implementation direction

Build the jitter as a small modern-UI renderer layer, not as new random state in the ViewModel. Rework from the 2026-07-01 connected-test failure must preserve Compose text semantics: replacing `Text` with pure `Canvas` plus only `contentDescription` removed the `onNodeWithText(...)` nodes that existing UI tests and accessibility tooling expect.


1. Add a UI helper file, for example `app/src/modern/java/com/example/androidmixtape/ui/JitteredHandwritingText.kt`.
2. Implement a `@Composable` such as `JitteredHandwritingText(...)` backed by `Canvas` + `rememberTextMeasurer` / `drawText` so glyphs or words can be translated and rotated individually.
3. Add a pure jitter model/generator that can be unit-tested without Compose:
   - `data class HandwritingJitterSample(val dxEm: Float, val dyEm: Float, val rotationDegrees: Float, val trackingEm: Float)`
   - `fun handwritingJitterSamples(seedMaterial: String, tokenCount: Int, strength: HandwritingJitterStrength): List<...>`
4. Seed from stable render identity, never wall-clock time:
   - include surface id: `spine-title`, `spine-first-track`, `cassette-name`, `cassette-current-track`, `track-list-header`, `track-row-$index`, `track-list-empty`
   - include mixtape identity: `MixTapeGroup.name`, `startIndex`, ordered track ids, and/or selected group font
   - include rendered text itself so different titles differ subtly
   - convert the seed material to a deterministic `Long` with a local stable hash (FNV-1a 64-bit or SHA-256 first 8 bytes), then use `Random(seed)`
5. Keep text semantics intact because Canvas text is not automatically exposed to Compose testing or accessibility as text. Prefer setting the `SemanticsProperties.Text` value on the jitter node, or keep exactly one hidden/transparent `Text(text, ...)` semantics carrier inside `JitteredHandwritingText`. Do **not** rely on `contentDescription = text` alone for labels that tests query with `onNodeWithText(...)`. Preserve existing row/card content descriptions separately.

## Suggested jitter limits

Use conservative defaults with per-surface tuning:

| Surface | Tokenization | dx | dy/baseline | rotation | tracking |
| --- | --- | --- | --- | --- | --- |
| Tape spine name | per character | +/- 0.04em | +/- 0.07em | +/- 2.0 deg | +/- 0.015em |
| Tape spine first track | per word | +/- 0.03em | +/- 0.05em | +/- 1.2 deg | 0 |
| Cassette tape name | per character | +/- 0.05em | +/- 0.08em | +/- 2.5 deg | +/- 0.02em |
| Cassette current track | per word | +/- 0.03em | +/- 0.05em | +/- 1.2 deg | 0 |
| Track list rows | per word | +/- 0.025em | +/- 0.04em | +/- 1.0 deg | 0 |

Clamp all rotation to roughly +/- 3 degrees. For long track names, prefer word-level jitter and keep `maxLines = 1`/ellipsis behavior or pre-truncate before Canvas drawing so readability wins.

## Rework plan after failed review

The review failure was not about the deterministic jitter math; it was about testable/accessible text disappearing from cassette/list surfaces. The next implementation pass should:

1. Refactor `JitteredHandwritingText` so each instance exposes the original string through Compose text semantics while the visual glyphs are still drawn by Canvas.
   - First-choice implementation: add explicit `semantics { text = AnnotatedString(text) }` to the Canvas modifier if the project Compose version supports that setter.
   - Fallback implementation: wrap Canvas and a `Text` semantics carrier in a `Box`, keep the carrier one line high and transparent/alpha-hidden, and mark only one node with the text so `onNodeWithText("Mix Tape 1")` counts remain unchanged.
   - Avoid adding both text semantics and an extra visible `Text` for the same string; duplicate nodes would break tests such as the `Smoke Track` count assertion.
2. Keep parent row descriptions like `Cassette spine Mix Tape 1`, `Cassette tape`, and `Cassette cover track ...` unchanged. These descriptions are separate from text-node semantics and are already used by UI tests.
3. Consider backing off Canvas replacement on non-cassette controls/settings if any were accidentally routed through `JitteredHandwritingText`; the jitter renderer should be limited to cassette handwriting surfaces only.
4. Add/adjust contract tests so the renderer source requires text semantics, not just `contentDescription`, and add a regression source test that forbids duplicate visible `Text` nodes for jittered labels.
5. Connected-test gate for the next Review: run `connectedModernDebugAndroidTest` on any available compatible emulator/device, not just unit tests. Passing `testModernDebugUnitTest` alone is insufficient for this rework.

## Latest rework plan after truncated-spine visual review

The 2026-07-01 review failure shows a layout bug rather than a jitter-seed bug: the Canvas renderer now preserves text semantics, but `JitteredHandwritingText` only adds a height internally. When a caller omits a real width modifier inside a `Column`/`Row`, the Canvas can measure at or near zero width, so the visual layer truncates to fragments such as `M.` / `01.` while semantics and connected tests still pass.

Next pass should make layout legibility a first-class contract:

1. Allocate real width for every cassette handwriting surface.
   - Add explicit `modifier = Modifier.fillMaxWidth()` to both spine labels (`spine-title`, `spine-first-track`).
   - Confirm existing cassette tape labels keep `Modifier.fillMaxWidth()`.
   - Add/confirm `Modifier.fillMaxWidth()` on `track-list-header`, every `track-row-*`, and `track-list-empty`.
   - Prefer caller-visible width modifiers over a hidden global default so narrow intentional uses are obvious in code review.
2. Harden `JitteredHandwritingText` against collapsed constraints.
   - Keep the internal line-height allocation, but do not treat a zero/near-zero `size.width` as a valid drawable text lane.
   - If useful, skip drawing until width is positive rather than drawing only the first token; semantics should still expose the full text.
3. Add bounds/legibility regression coverage that would have caught the visual failure.
   - Extend `HandwritingJitterRendererSourceContractTest` or `CassetteSpineReadabilityContractTest` so each cassette surface call near `surface=...` must include a width modifier (`fillMaxWidth`, `weight`, or an explicit width) before Review.
   - Add/extend a connected Compose UI test to assert the `Mix Tape 1` and `01. Track 1` text-semantics nodes have non-trivial width (for example at least 140-180.dp on the spine) and remain displayed; include the cassette cover header/track rows if the matcher can disambiguate them.
   - The prior `onNodeWithText(...).assertIsDisplayed()` checks are not enough because semantics can pass while the Canvas draw area is collapsed.
4. Require visual evidence before Review.
   - Run `connectedModernDebugAndroidTest` on a compatible device/emulator.
   - Launch the modern APK, grant audio permission/load demo media if needed, capture the mixtape-list screen, and verify the spine labels are visually readable at normal device size before moving to Review.
   - Save evidence under `docs/evidence/card2219-*` and attach/comment it if Review needs visual proof.

## Concrete source touchpoints

- `JitteredHandwritingText`: keep deterministic Canvas rendering and explicit Compose text semantics, but make collapsed-width behavior safe so a bad caller does not draw misleading fragments.
- `CassetteSpineRow`: replace the two handwriting `Text` calls for `group.name` and `01. ...` with `JitteredHandwritingText`. Use seed material from `group.stableHandwritingKey()` equivalent plus surface id and text. If the current `stableHandwritingKey` is private to the ViewModel, duplicate a UI-only stable identity helper using `group.name`, `startIndex`, and `tracks.map { it.id }`. Ensure both jitter calls receive `Modifier.fillMaxWidth()` inside the weighted label column.
- `CassetteTape`: replace `tapeName` and `currentTrack?.title ?: "Select a track"` handwriting `Text` calls. Add a `mixtapeJitterKey: String` parameter if needed; otherwise derive from tape name + current track id/title for now.
- `CassetteCoverTrackList`: replace header, track row, and empty-state handwriting text. Track row seed should include `index`, `track.id`, `track.title`, `track.artist`, and row text.
- Preserve `fontFamily = handwritingFont.cassetteHandwritingFontFamily()` and `fontWeight = handwritingFont.effectiveCassetteWeight(...)` in the new renderer API.

## Test plan

Unit/contract tests to add or update:

1. Pure generator tests:
   - same seed/text/token count returns identical sample list across repeated calls
   - different mixtape seed or different text returns at least one changed sample
   - all values stay within the configured max dx/dy/rotation/tracking bounds
2. UI/source contract tests:
   - cassette handwriting paths call `JitteredHandwritingText` rather than plain `Text` for the handwritten surfaces
   - no `Random.Default`, `System.currentTimeMillis`, or `Clock` usage in jitter generation
   - Canvas-rendered labels expose Compose text semantics so `onNodeWithText(...)` continues to find `Mix Tape 1`, `01. Track 1`, cassette names, and current-track text
   - existing parent content descriptions remain available for cassette rows, cassette tape, reels, and track rows
   - all cassette handwriting call sites allocate real width; spine title/first-track, track-list header, track rows, and empty state must not rely on a height-only Canvas modifier
   - connected UI assertions check non-trivial layout bounds for the spine handwriting labels, not just semantic discoverability
3. Build/device verification:
   - `JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest`
   - `JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew connectedModernDebugAndroidTest` on any available compatible device/emulator
   - visually compare repeated renders of the same mixtape and verify the pattern is stable; then reset/create different mixtapes and confirm subtle differences.

## Risks and guardrails

- Compose `Text` handles ellipsis, layout, and text semantics for free; Canvas text does not. Keep the first pass simple: single-line surfaces only, width-constrained, conservative, and with an explicit semantics carrier. For track rows, pre-truncate or draw word tokens until available width is exhausted.
- Per-character measurement can be expensive for long rows. Use word-level jitter for track rows and smaller text.
- Do not introduce randomness in composables with `Random.Default`; recomposition must not change the pattern.
- Do not overdo rotation or baseline wobble. The goal is cassette-label imperfection, not ransom-note typography.

## Acceptance criteria for implementation

- Same mixtape/surface/text renders produce identical jitter samples and visual placement across recompositions and app sessions given the same library metadata.
- Different mixtape identities or text produce subtly different samples.
- All handwritten cassette surfaces remain readable at current font sizes on list spine, cassette label, and track list.
- Unit tests pass and no wall-clock/random-default source is used by the jitter generator.
