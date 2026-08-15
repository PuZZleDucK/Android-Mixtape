# Card 2241 — Landscape two-column mixtape list plan

## Goal

Make the modern Mix Tapes library screen adapt in landscape so the cassette-spine list reads as a two-wide cassette carrying case instead of one over-stretched full-width column. Preserve the cassette/spine proportions: landscape should add a second column inside the case frame, not stretch each cassette row wider or flatten the visual cassette details.

## Current implementation touchpoints

- Main target: `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `MixTapeLibrary(...)` currently renders a header plus a single `LazyColumn` with the dark briefcase `drawBehind` frame.
  - `CassetteSpineRow(...)` already provides compact cassette-spine rows with 64.dp+ height, end caps, label strip, screws, and `Cassette spine <name>` semantics.
  - `NowPlaying(...)` already uses `BoxWithConstraints` and `maxWidth > maxHeight`; use that as the preferred orientation pattern.
- Likely tests to add/update in Testing:
  - New JVM source contract such as `MixTapeLandscapeTwoColumnContractTest`.
  - Existing modern Compose test `MixtapeAppTest` if it can assert landscape-sized rendering.
  - Existing `CassetteVisualAspectRatioContractTest` should continue to protect the Now Playing full cassette aspect ratio and spine-vs-case distinction.

## Visual / layout direction

- Portrait behavior should remain the existing single-column cassette briefcase/spine list.
- Landscape behavior should:
  - Detect landscape with `BoxWithConstraints` inside `MixTapeLibrary` (`val isLandscape = maxWidth > maxHeight`).
  - Keep the same warm dark/brown/brass briefcase frame, but make the inner filed area feel like a two-column cassette case.
  - Render mixtapes in two columns with balanced spacing, preferably using `LazyVerticalGrid(columns = GridCells.Fixed(2))` or a small helper composable that places rows in two equal columns.
  - Keep each cassette item at its intended spine height/touch target (`heightIn(min = 64.dp)` or a named constant), with max width bounded by the column rather than stretching across the entire screen.
  - Preserve cassette details: end caps, screws, label strip, track count, `NO.##`, one first-track hint, ellipsis, and `Cassette spine ...` semantics.
  - Avoid changing Now Playing landscape behavior unless unavoidable.

## Suggested implementation steps

1. Add an orientation-aware wrapper to `MixTapeLibrary` using `BoxWithConstraints`.
2. Extract the briefcase frame modifier/drawing into a small reusable helper or private modifier function so portrait `LazyColumn` and landscape grid share the same frame palette.
3. Keep the current portrait `LazyColumn` path unchanged apart from sharing the frame helper.
4. Add a landscape path:
   - Prefer `LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp))`.
   - Keep item keys stable (`index` is acceptable for the current generated grouping, but a name-based key is better if available).
   - Pass the global side number as `index + 1` so numbering remains ordered left-to-right/top-to-bottom.
5. If `LazyVerticalGrid` imports add dependency friction, use two weighted `LazyColumn`s fed by even/odd indexed groups; preserve global numbering and click index.
6. Add/update source contract tests in the Testing stage to fail until the landscape branch exists.
7. Verify both variants still build; this is modern UI code, but legacy compilation must not regress.

## Success criteria

- In portrait, Mix Tapes still displays the existing one-column cassette-spine briefcase list.
- In landscape, Mix Tapes displays cassette spines in two columns inside one case/briefcase frame.
- Landscape columns do not stretch any cassette row across the entire screen; cassette/spine height and drawn details remain visually proportional and readable.
- Mixtape row semantics and tap behavior still open the correct mixtape.
- Long names and first-track hints still ellipsize without increasing row height.
- Now Playing cassette dimensions/aspect-ratio behavior remains unchanged.
- Modern unit/source contract tests document the new two-column landscape requirement.
- `testModernDebugUnitTest`, `testLegacyDebugUnitTest`, `assembleModernDebug`, and `assembleLegacyDebug` pass after Running.

## Testing plan for the next stage

Add failing tests before implementation:

- Source contract: `MixTapeLibrary` uses `BoxWithConstraints` or equivalent constraint inspection and checks `maxWidth > maxHeight` / `isLandscape`.
- Source contract: landscape branch uses `LazyVerticalGrid` with `GridCells.Fixed(2)` or an explicit two-column `Row` of weighted lists.
- Source contract: portrait path still contains the one-column `LazyColumn` and shared briefcase `drawBehind` palette.
- Source contract: `CassetteSpineRow` keeps a minimum height/touch target and does not use `aspectRatio(CASSETTE_VISUAL_ASPECT_RATIO)`.
- Optional Compose UI test: render the Mix Tapes screen under a landscape-sized root and assert at least two cassette spine semantics are present and tappable.

## Verification commands after implementation

```sh
cd /home/puzzleduck/x/android-mixtape
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
```

If a modern device/emulator is available with enough seeded audio:

```sh
./gradlew connectedModernDebugAndroidTest
```

Manual visual smoke:

1. Install `modernDebug` on an Android 7+ device/emulator.
2. Seed enough tracks to create at least 6 mixtapes.
3. Open Mix Tapes in portrait and confirm the existing one-column cassette briefcase remains readable.
4. Rotate to landscape and confirm the list becomes two cassette-spine columns inside the case frame.
5. Tap mixtapes from left and right columns and confirm each opens the expected Now Playing queue.

## Risks / guardrails

- Do not use screen-width stretching as the landscape solution; the requested behavior is two columns while preserving cassette dimensions.
- Do not reduce touch targets below the current 64.dp-ish minimum just to fit more rows.
- Do not accidentally apply the two-column behavior to Now Playing’s track list.
- Keep changes in the modern Compose UI unless tests reveal shared model/state changes are needed.
- Avoid large new assets; this should be achievable with Compose layout and existing Canvas drawing.
