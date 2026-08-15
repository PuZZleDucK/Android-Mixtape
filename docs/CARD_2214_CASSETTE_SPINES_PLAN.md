# Card 2214 — Cassette-spine mixtape list UI plan

## Goal

Restyle the modern Compose Mix Tapes list so it reads as rows of cassette spines filed in a carrying case/briefcase, matching the referenced image concept: many narrow cassette cases viewed from the spine, packed together in a box with subtle plastic/aged-paper details. This should replace the current large front-facing `CassetteCaseCard` list, while keeping tap targets and text readable on portrait phones.

Reference direction: Reddit post title “bought a cassette briefcase to store some of my…” from r/Emo. Direct fetch was blocked by Reddit challenge, so implementation should use the provided URL plus search terms below for visual reference.

## Current implementation touchpoints

- Main target: `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `MixTapeLibrary(...)` currently draws shelf lines and renders one large `CassetteCaseCard(...)` per group.
  - `CassetteCaseCard(...)` is front-facing, aspect-ratio-based, and previews three tracks.
- Existing contract tests to preserve/adjust:
  - `CassetteCaseReadabilityContractTest`
  - `CassetteVisualAspectRatioContractTest`
  - Compose UI tests in `app/src/androidTestModern/java/com/example/androidmixtape/ui/MixtapeAppTest.kt`
- Legacy API 19 Views UI can stay functional/simple unless product explicitly asks for parity; avoid risking legacy compatibility for this style pass.

## Visual design direction

- Background/case:
  - Draw a dark, shallow briefcase/box around the list using `drawBehind`: warm black/brown outer border, beveled side rails, subtle inner shadow.
  - Add a top lip/hinge line and bottom rail to make the list feel like cassettes sitting inside a case.
  - Use muted case colors: charcoal `#201A17`, aged brown `#4B3528`, brass highlights `#A77A35`, smoky transparent plastic `#2C3038`.
- Cassette spines:
  - Render each mixtape as a dense horizontal spine card, roughly 52–72.dp high, full width, with rounded/rectangular plastic case edges.
  - Alternate aged J-card paper colors: cream, faded pink, pale blue, mint, yellowed white.
  - Add thin vertical end caps/screws/notches to imply the plastic cassette box spine.
  - Include a narrow colored label strip or handwritten sticker area.
  - Keep text on the spine, not full cover art: mixtape name, `NO.##`, track count, and optionally first artist/first track.
  - Slight per-row variation is welcome but should be deterministic from index/name, not random at runtime.
- Typography:
  - Continue the handwritten/cursive vibe sparingly, but protect readability with bold sans/monospace fallback for key labels.
  - Use ellipsis for long names; avoid tiny unreadable decorative text.
- Interaction:
  - Entire spine row remains clickable and has a semantic description such as `Cassette spine <name>`.
  - Selected/current playing state, if exposed in the list later, could be a small play sticker or brighter spine accent; not required for first pass.

## Suggested implementation steps

1. Rename/refactor the list item composable from `CassetteCaseCard` to something like `CassetteSpineCard` or `CassetteSpineRow`.
2. Update `MixTapeLibrary` to draw the briefcase/case frame behind the `LazyColumn` and render tighter spine rows.
3. Preserve accessible semantics and a comfortable minimum touch target; use `heightIn(min = 56.dp)` or a fixed height above Material minimum.
4. Move old full-front cassette drawing out only if no tests depend on it; otherwise adapt tests to the new spine contract.
5. Add/update JVM source contract tests to assert:
   - Mix Tapes screen uses a cassette spine/card composable naming/semantic contract.
   - The previous large front-facing aspect-ratio case is no longer used for library rows.
   - Rows retain min height/touch target and do not require more than one optional preview line.
   - Briefcase/case-frame colors or drawBehind block are present.
6. Update modern connected Compose tests if they assert old content descriptions (`Cassette case ...`).
7. Run verification commands below and capture a screenshot on a modern emulator/device if available.

## Success criteria

- The Mix Tapes library visually reads as cassette spines packed in a carrying case/briefcase, not large front-facing cassette covers.
- At least 6–8 mixtapes can be visible on a typical portrait phone without feeling cramped.
- Mixtape titles and track counts remain readable and ellipsized gracefully.
- Tap behavior still opens the selected mixtape/Now Playing queue.
- Permission/loading/empty/error/settings/Now Playing flows are not regressed.
- Existing modern and legacy unit/build checks pass.
- Tests document the new spine-style contract so future changes do not revert to the old full-cover list.

## Verification checklist

```sh
cd /home/puzzleduck/x/android-mixtape
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
```

If a modern emulator/device is available with seeded audio:

```sh
./gradlew connectedModernDebugAndroidTest
```

Manual visual smoke:

1. Install `modernDebug` on an Android 7+ device/emulator.
2. Seed enough tracks to create multiple mixtapes.
3. Launch Mixtape and grant audio permission.
4. Confirm the Mix Tapes list looks like cassette spines in a case, scrolls smoothly, and opens a tape on tap.
5. Rotate portrait/landscape and confirm the case frame and spine text remain readable.

## Search/reference terms

- `cassette briefcase collection spines`
- `cassette tape storage case spines`
- `vintage cassette carrying case filled`
- `clear cassette cases spine labels`
- `cassette j-card spine label handwritten`

## Risks / guardrails

- Do not over-compress rows below usable tap size just to show more spines.
- Do not rely on nondeterministic visual randomness in Compose; deterministic palette by index is safer for tests and screenshots.
- Avoid adding heavy image assets for this pass; Canvas shapes and Compose layout should be enough.
- Keep the API 19 legacy build compiling even if only the modern Compose UI gets the visual style.
