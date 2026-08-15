# Card 3152 — Full track names with horizontal track-list scrolling

## Goal

The Now Playing cassette-cover track list must never clip or ellipsize a track name because the phone or landscape pane is narrow. Keep each track row on one line and let the user pan the track-list paper horizontally to read the complete row, while preserving its existing vertical scrolling and current-track auto-centering.

## Current implementation facts

- The affected UI is `CassetteCoverTrackList(...)` in `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`; both portrait and landscape Now Playing call this composable with `scrollContent = true`.
- A row currently renders `"${track.title} — ${track.artist}  ${formatDuration(track.durationMs)}"` through `JitteredHandwritingText` with `maxLines = 1`, `TextOverflow.Clip`, and `Modifier.fillMaxWidth()`. The custom Canvas renderer stops adding tokens when it reaches the Canvas width, so the missing text is not merely off-screen—it is never drawn.
- The list already owns a vertical `ScrollState`, row bounds, and a `LaunchedEffect` that vertically centers the current track. Horizontal work should not replace or interfere with that behavior.
- `JitteredHandwritingTextRenderer.kt` has no intrinsic content width because it is Canvas-based. Simply adding `horizontalScroll()` around the current width-constrained Canvas is insufficient; the content must be measured and laid out at the complete rendered-text width.
- The repository currently has unrelated modified/untracked work. Running should isolate this card’s edits and avoid overwriting or committing unrelated changes.

## Implementation plan

1. Add focused regression coverage first.
   - Add a JVM source/contract test beside the other UI contracts asserting that the Now Playing track-list viewport has a horizontal `ScrollState`/`horizontalScroll`, while retaining `verticalScroll(contentScrollState)`.
   - Assert track rows no longer use viewport-width clipping as their rendering width and that full-width sizing is derived from the complete `rowText`, not a fixed magic width.
   - Preserve the full semantic string/content description so accessibility and UI tests can find a long title even when part of it is currently off-screen.

2. Give the scrollable paper a content width based on real text measurement.
   - Use Compose `rememberTextMeasurer()` with the same font family, size, weight, and token tracking inputs used by the row renderer to determine the widest complete row. Include the header if it can be wider.
   - Account for horizontal paper padding and the jitter renderer’s small x-offset/rotation safety margin so the first/last glyph is not clipped.
   - Resolve the viewport width with `BoxWithConstraints`; set content width to `max(viewportWidth, widestMeasuredTextWidth + padding/safety margin)`. Do not use a fixed width such as `2000.dp`, because that still truncates sufficiently long metadata and produces excessive blank scrolling for short lists.
   - If measurement logic is added to `JitteredHandwritingText`, expose a small reusable measured-width helper or opt-in width mode rather than changing every existing cassette label. Existing bounded labels should keep their current behavior.

3. Add one shared horizontal viewport for the whole track-list paper.
   - Create a remembered horizontal `ScrollState` and apply `horizontalScroll(horizontalScrollState)` at the viewport/content boundary.
   - Keep the existing vertical state independently attached so vertical swipes continue to browse tracks and horizontal swipes reveal long names. A shared horizontal position across rows is preferable to separate per-row states: the paper should pan as one physical J-card, preserve row alignment, and avoid many nested gesture targets.
   - Keep row click/double-click/long-press actions and dropdown anchoring functional after panning.
   - Reset or clamp horizontal offset when the mixtape/track set changes so a newly opened tape does not begin unexpectedly scrolled into blank/right-side content. Do not reset it on current-track changes.

4. Preserve vertical auto-scroll and visual behavior.
   - Keep `scrollViewportBounds` based on the visible Card/viewport, and continue collecting each row’s window-relative top, center, and bottom. Horizontal translation must not alter the existing y-axis calculations.
   - Preserve selected-row accent color, handwriting font/jitter seed, lined/grid paper drawing, header, artist, duration, and one-line row height.
   - Verify both portrait and landscape callers; do not introduce a second implementation for either orientation.

5. Verify on the required target.
   - Run the focused new contract and related modern UI contracts, then modern and legacy unit suites.
   - Build an x86/x86_64-compatible modern debug APK, install it on the Kunlun phone emulator (normally `emulator-5554`), and open a tape containing a deliberately long title.
   - In portrait and landscape, swipe horizontally to the far right and confirm the final title characters are visible; swipe back left, vertically scroll, and rapidly advance several tracks to confirm current-row vertical auto-centering still works.
   - Capture emulator screenshots at the left and right horizontal extents as evidence.

## Suggested test data

Use a title whose unique ending cannot fit in either orientation, for example:

`The Extremely Long Unabridged Midnight Cassette Recording — Final Visible Suffix XYZ3152`

The assertion/evidence must show `XYZ3152` after horizontal scrolling. Include a short title in the same tape to verify ordinary rows do not regress.

## Risks and mitigations

- **Canvas still clips after adding a scroll modifier:** measure and assign the complete content width before drawing; test the unique suffix visually.
- **Infinite-width Compose constraints:** calculate a finite width from `TextMeasurer` and the finite `BoxWithConstraints.maxWidth`; do not rely on `fillMaxWidth()` under an unbounded horizontal-scroll constraint.
- **Jitter clips edge glyphs:** include deterministic jitter/rotation safety padding in measured width.
- **Gesture conflicts:** use orthogonal shared scroll states and manually test diagonal, vertical, and horizontal gestures plus long-press/double-click.
- **Horizontal scroll breaks vertical centering:** retain the visible viewport bounds and y-only row measurement math; run the existing rapid-next scroll regression.
- **Unrelated dirty-tree work is mixed into this card:** inspect `git status`/diff before and after, stage only card-specific files, and verify from a coherent committed tree or isolated worktree.

## Success criteria

- Every row’s complete track title is rendered, with no ellipsis or token loss at the viewport edge.
- The user can horizontally pan the track-list paper until the final character of the longest row is visible, then return to the left edge.
- Short rows remain readable and the list is not given an arbitrary oversized blank width.
- Vertical scrolling and automatic current-track centering continue to work at any horizontal offset.
- Portrait and landscape Now Playing use the same behavior.
- Row selection/highlight, double-click jump, long-press actions, paper themes, handwriting/jitter, artist, and duration remain intact.
- Focused/full tests pass and Kunlun emulator screenshots demonstrate the long-title suffix at the right extent.
