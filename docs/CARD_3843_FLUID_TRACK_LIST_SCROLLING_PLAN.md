# Card 3843 — Fluid two-dimensional track-list scrolling

## Goal

Make the Now Playing cassette-cover track list behave like one freely movable sheet: a single drag may pan horizontally and vertically at the same time, including diagonally, while long names, row actions, and current-track centering continue to work. Investigate the reported scroll jank, apply the low-risk causes found in the current implementation, and leave a concrete pre-release performance item only if measured jank remains after those fixes.

## Current implementation facts

- The affected production composable is `CassetteCoverTrackList(...)` in `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`. Both portrait and landscape Now Playing branches call the same composable with `scrollContent = true`.
- Card 3152 already added a shared `contentHorizontalScrollState`, a vertical `contentScrollState`, finite paper width based on the longest complete row, and horizontal reset when the track set changes. Preserve those full-name guarantees.
- Horizontal and vertical movement are currently implemented by nested `horizontalScroll(...)` and `verticalScroll(...)` modifiers. Their independent one-axis drag recognizers can direction-lock/compete after touch slop, so they do not provide intentional simultaneous x/y panning.
- Compose Foundation 1.7.6, already resolved by this project, contains the experimental `draggable2D` / `rememberDraggable2DState` API. `CassetteCoverTrackList` is already opted into `ExperimentalFoundationApi`, so no dependency upgrade is required for a focused prototype.
- Every track is eagerly composed in a `Column`. More immediately, each `JitteredHandwritingText` currently calls `textMeasurer.measure(...)` for every character token and allocates token/layout collections inside its `Canvas` draw block. Those operations can repeat during redraws caused by scrolling and are a plausible simple source of jank.
- `CassetteCoverTrackList` also remeasures every complete row to find the widest row whenever that section recomposes. The result depends on the tracks, font/style, and density and can be remembered rather than recalculated for unrelated current-row changes.
- There is no benchmark module or existing frame-performance gate. `publish-todo.md` is the existing pre-release checklist.

## Implementation plan

### 1. Establish focused regressions before changing gestures

- Add a JVM source/contract test beside `TrackListFullNameHorizontalScrollContractTest` that requires one two-dimensional drag owner for the scrollable paper and rejects two simultaneously user-enabled one-axis drag recognizers.
- Preserve assertions for finite measured paper width, full semantic row text, horizontal reset on a new track set, and independent horizontal/vertical state values.
- Keep `CassetteFastForwardTrackListScrollContractTest`, double-click, and long-press contracts in the focused suite because gesture ownership must not break automatic vertical centering or row actions.
- If practical, isolate the offset/sign/clamping logic in a small internal helper and unit-test diagonal input (`x != 0`, `y != 0`) plus edge clamping. Do not rely only on a brittle source-string assertion for the movement math.

### 2. Replace direction competition with one 2D drag path

- Attach one `draggable2D` gesture modifier to the visible track-list viewport and drive both existing `ScrollState`s from every drag delta. Use the horizontal component for `contentHorizontalScrollState` and the vertical component for `contentScrollState`, with finger/content direction handled consistently.
- Retain the existing horizontal and vertical layout modifiers/states for clipping, placement, programmatic reset, and current-track auto-centering, but disable their separate touch-drag recognizers when the 2D handler is active. This avoids duplicating scroll layout code while eliminating axis locking.
- Let each `ScrollState` clamp at its own `0..maxValue`; when one axis reaches an edge, the other axis must continue moving.
- Preserve inertial motion. Prototype the Foundation 1.7.6 `draggable2D` stop callback with the platform spline decay/fling behavior so x and y velocities decay together. Cancel an active fling on a new touch and on automatic current-track vertical centering. If the experimental API cannot provide stable two-axis fling semantics on API 24, use one viewport-level pointer/velocity implementation rather than restoring nested competing recognizers.
- Keep touch slop so taps, double-clicks, and long presses still reach `combinedClickable`; consume movement only after a drag is recognized. Track menus must remain anchored to their rows after panning.
- Keep horizontal position unchanged when the current track advances; reset it only when the track set changes, as today. Programmatic current-track centering changes only the y state.

### 3. Apply the simple, evidenced performance fixes

- Move character-token text measurement and immutable token layout preparation out of the `Canvas` draw lambda in `JitteredHandwritingTextRenderer.kt`. Cache it with `remember(...)` using all rendering inputs that affect layout (text/tokenization, font family/style/weight/size, overflow, and density/font scale). The draw pass should iterate cached layouts rather than call `TextMeasurer.measure` and rebuild collections on each frame.
- Ensure color-only current-row highlighting does not invalidate/recompute geometry for every row. Keep layout and deterministic jitter samples stable when only selection color changes.
- Remember the track-list row strings and widest-paper measurement in `CassetteCoverTrackList`, keyed to tracks, header visibility/text, font/style, density/font scale, and relevant padding. A current-index update must not remeasure every track.
- Avoid a speculative full rewrite first. After caching, measure again. If eager composition remains the dominant cost on large lists, convert the vertical body to a keyed `LazyColumn`/`LazyListState` while preserving shared finite horizontal paper width and current-track centering. This is a second step, not mixed into the initial low-risk patch, because it changes row measurement and auto-scroll behavior.

### 4. Measure before and after on Kunlun

- Use `./kunlun-sync.sh` for APK synchronization/install/start on the Kunlun phone emulator. Build an x86/x86_64-compatible modern debug APK as required by the project instructions.
- Seed a deterministic large tape (target at least 100 tracks, including several long names) and warm the screen before measurement.
- On the Kunlun host, reset and collect `adb shell dumpsys gfxinfo com.example.androidmixtape` around the same repeated vertical, horizontal, and diagonal swipe sequence. Record three runs before and after; retain total/janky frames and 90th/95th/99th percentile frame times in `docs/evidence/`.
- Treat emulator numbers as comparative evidence, not universal device guarantees. The patch passes the focused performance check when the median post-change run has no visible multi-frame stalls and either (a) janky frames are at most 10% with 95th-percentile frame time at most 32 ms, or (b) it improves janky-frame rate by at least 30% from a worse baseline and no regression appears in any gesture direction.
- If the simple caching patch misses that bar and a safe lazy-list conversion is not completed in this card, add this unchecked item under `publish-todo.md` → **Final release preparation** before handoff: `Profile and optimize Now Playing track-list scrolling on a representative physical device with a 100+ track tape; resolve sustained >10% janky frames or >32 ms 95th-percentile frame time.` Include the measured evidence path in the checklist/card comment.

### 5. Functional and visual verification

- Run the focused track-list contracts, then `./gradlew testModernDebugUnitTest testLegacyDebugUnitTest`, `assembleModernDebug assembleLegacyDebug`, and `lintModernDebug lintLegacyDebug`.
- Deploy the updated modern APK through `./kunlun-sync.sh`; run applicable connected tests on the Kunlun emulator after deployment.
- In portrait and landscape, verify:
  - a single continuous diagonal drag changes both visible row position and long-title horizontal position;
  - nearly horizontal and nearly vertical drags remain predictable;
  - reaching the right/top/bottom edge on one axis does not freeze the other;
  - horizontal and diagonal flings coast and can be interrupted;
  - the unique end of a long title remains reachable;
  - rapid Next operations still center the highlighted row vertically without resetting x;
  - tap/double-click/long-press actions and the dropdown menu still work;
  - switching to a different tape resets x and does not retain stale row bounds.
- Capture Kunlun emulator screenshots showing the same long-title tape at the initial position and after a diagonal pan in both portrait and landscape. A short screen recording is preferable evidence for simultaneous movement if the test harness can capture one without leaving a recorder running; screenshots remain required by project policy.

## Suggested test data and gesture

- Long row suffix: `The Extremely Long Unabridged Midnight Cassette Recording — Final Visible Suffix XYZ3843`.
- Place it below the initial viewport in a 100+ track tape. From the upper-left position, perform one continuous up-left finger drag. Success requires later rows and `XYZ3843` to become visible without lifting and beginning a second axis-specific gesture.
- Search terms for implementation work: `Compose Foundation 1.7.6 draggable2D rememberDraggable2DState`, `Compose two dimensional fling splineBasedDecay Offset`, `ScrollState dispatchRawDelta`, `Compose Canvas cache TextLayoutResult draw performance`, and `adb dumpsys gfxinfo framestats janky frames`.

## Risks and mitigations

- **2D handler breaks clicks/long presses:** preserve touch slop and add row-action regression/manual checks; do not consume the initial down event as a drag.
- **Drag direction is reversed:** cover finger-to-content sign behavior with a small offset test and emulator smoke before adding fling.
- **One edge blocks both axes:** dispatch/clamp components independently and test diagonal movement while x is already at max.
- **Fling fights current-track auto-centering:** cancel the viewport fling before `animateScrollTo` and keep that effect y-only.
- **Cached text becomes stale after font-scale/theme changes:** include density/font scale and every geometry-affecting style input in cache keys; color should remain a draw-time input.
- **Lazy conversion regresses fast-forward centering:** only take the second-stage lazy path with keyed items and explicit `LazyListState` centering tests; otherwise record the objective release checklist item.
- **Performance result is emulator-noisy:** use a warmed deterministic tape, identical gestures, three runs, and report raw frame data rather than claiming a single run proves device performance.

## Success criteria

- One continuous drag can change x and y simultaneously in portrait and landscape; no lift/re-grab is needed to inspect long names while moving through the list.
- Dragging/flinging remains smooth and interruptible, and either the stated frame threshold/relative improvement is met or the exact measured pre-release checklist item is added.
- Complete long row text remains rendered and reachable, including `XYZ3843`.
- Current-track vertical auto-centering works at any horizontal offset and does not reset x.
- Track double-click, long-press menu actions, selection colors, paper themes, handwriting jitter, artist, and duration remain intact.
- Focused/full tests, builds, lint, Kunlun deployment, connected checks, and required emulator screenshots complete successfully.
- No binary asset is needed for implementation; disposable traces/recordings go under ignored `.work/`, while compact text results and selected screenshots go under `docs/evidence/`.
