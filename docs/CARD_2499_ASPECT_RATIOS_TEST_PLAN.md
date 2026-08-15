# Card 2499 — Aspect-ratio TDD test plan

## Planning rollback update

Review rejected the previous pass because the focused tests allowed orphaned `CASSETTE_CASE_FRONT_ASPECT_RATIO`, `CASSETTE_SPINE_LABEL_ASPECT_RATIO`, and `CASSETTE_CASE_SPINE_ASPECT_RATIO` constants. The next Testing pass should tighten the source contracts so constants cannot be declared just to satisfy numeric tests; every cassette-like production surface must either use the relevant physical ratio or carry a named, test-recognized exception.

## Existing red contracts

Updated source-level unit contracts under `app/src/test/java/com/example/androidmixtape/ui/` before changing production UI code:

- `CassetteVisualAspectRatioContractTest`
  - requires separate physical constants:
    - `CASSETTE_SHELL_ASPECT_RATIO` in `1.57..1.60`
    - `CASSETTE_CASE_FRONT_ASPECT_RATIO` in `1.54..1.57`
    - `CASSETTE_SPINE_LABEL_ASPECT_RATIO` in `7.9..8.1`
    - `CASSETTE_CASE_SPINE_ASPECT_RATIO` in `6.2..6.5`
  - rejects the ambiguous generic `CASSETTE_VISUAL_ASPECT_RATIO`.
  - requires `CassetteTape` to use the shell-specific ratio only.
  - prevents `CassetteSpineRow` from borrowing shell or case-front ratios.
- `CassetteSpineReadabilityContractTest`
  - adds a regression for `NowPlayingModeToggleAndSpine` so the current-mixtape spine preview no longer forces `CassetteSpineRow` into the old `height(58.dp)` slot.
  - requires the preview to preserve at least a 64dp readable spine target or explicitly use a spine-label/case-spine ratio constant.
- `MixTapeLandscapeTwoColumnContractTest`
  - extends the landscape spine guard so spine rows cannot accidentally use the future shell-specific or case-front constants.

## Required tightened red contracts

Add focused assertions before the next implementation pass:

- `CassetteVisualAspectRatioContractTest`
  - fail when any `CASSETTE_*_ASPECT_RATIO` constant appears only at its definition unless the test also finds a specific exception marker explaining why the corresponding physical surface does not currently exist in production.
  - require `CASSETTE_SHELL_ASPECT_RATIO` in the `CassetteTape` modifier chain, not merely anywhere in the file.
  - require `CASSETTE_CASE_FRONT_ASPECT_RATIO` to be used by any front-facing cassette case/J-card preview if one exists; otherwise require a source comment/marker such as `NoProductionCassetteCaseFrontSurface` near the constant or helper docs.
  - require spine constants to be used by true previews, or require explicit markers such as `CassetteSpineTouchTargetRatioException` for full-width list rows and `StaticMixtapeStackIconRatioException` for the 64dp square icon.
- `CassetteSpineReadabilityContractTest`
  - require `NowPlayingModeToggleAndSpine` to call a ratio-aware helper or carry a named current-spine exception; `.weight(1f).heightIn(min = 64.dp)` by itself should fail as an undocumented contract.
  - keep rejecting the old `.height(58.dp)` slot.
- `MixTapeLandscapeTwoColumnContractTest`
  - keep rejecting shell/front ratios on list spines, but also require the two-column landscape path to preserve the same named spine exception/ratio contract used in portrait.
- Add or extend a contract for `CassetteCoverTrackList`:
  - classify it as scrollable J-card back/tracklisting paper and either use a back-flap ratio for bounded previews or require a named `TrackListScrollablePaperRatioException`.

## Current expected red failures

Against the current reviewed source, the tightened tests should fail because case-front/spine constants are defined but not used outside their declarations, `NowPlayingModeToggleAndSpine` has no explicit ratio/exception contract for its current-spine preview, `StaticMixtapeStackPreview` draws mini-spines in a square icon without a documented exception, and `CassetteCoverTrackList` is not classified as ratio-bound back-flap paper vs. scrollable paper.

Testing update 2026-07-08: added the tightened source contracts. The focused command is intentionally red with 4 failures:

- `CassetteVisualAspectRatioContractTest.cassetteAspectRatioConstantsAreNotOrphanedWithoutNamedExceptions`: `CASSETTE_CASE_FRONT_ASPECT_RATIO`, `CASSETTE_SPINE_LABEL_ASPECT_RATIO`, and `CASSETTE_CASE_SPINE_ASPECT_RATIO` are still orphaned unless production code uses them or adds recognized exception markers.
- `CassetteVisualAspectRatioContractTest.productionCassetteLikeSurfacesDeclareRatioUseOrNamedException`: `CassetteSpineRow`, `NowPlayingModeToggleAndSpine`, `StaticMixtapeStackPreview`, and `CassetteCoverTrackList` must each use a relevant ratio or named exception marker.
- `CassetteSpineReadabilityContractTest.nowPlayingCurrentSpinePreviewDoesNotSquashSpineBelowReadableTarget`: the current-spine preview can no longer rely on bare `.weight(1f).heightIn(min = 64.dp)` without a ratio/exception contract.
- `MixTapeLandscapeTwoColumnContractTest.cassetteSpineRowsKeepTheirCompactDimensionsInLandscape`: landscape spines must share the same spine-specific ratio or `CassetteSpineTouchTargetRatioException` contract.

Running update 2026-07-09: implemented the ratio/exception contracts. `CassetteSpineRow` paints its case-spine band and label strip from the spine ratios inside the 64dp touch target, `NowPlayingModeToggleAndSpine` names the flexible current-spine exception, `StaticMixtapeStackPreview` draws ratio-correct mini spines inside the square icon, `CassetteCoverTrackList` applies the J-card back ratio to bounded paper and names the scrolling-pane exception, and the focused modern source contracts now pass.

## Commands

Run the focused red contracts:

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest \
  --tests 'com.example.androidmixtape.ui.CassetteVisualAspectRatioContractTest' \
  --tests 'com.example.androidmixtape.ui.CassetteSpineReadabilityContractTest' \
  --tests 'com.example.androidmixtape.ui.MixTapeLandscapeTwoColumnContractTest'
```

After Running implements the source changes, also run:

```sh
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```

## Running-stage implementation hints

- Replace the generic ratio with named shell/case/spine constants and source comments referencing the physical cassette/J-card dimensions.
- Keep `CassetteTape` on `CASSETTE_SHELL_ASPECT_RATIO`.
- Keep spine rows/list previews off shell and front-cover ratios; use spine-specific constants for true spine previews, while retaining accessible 56–64dp minimum touch targets through a named exception helper/comment that tests can find.
- Do not leave `CASSETTE_CASE_FRONT_ASPECT_RATIO`, `CASSETTE_SPINE_LABEL_ASPECT_RATIO`, or `CASSETTE_CASE_SPINE_ASPECT_RATIO` orphaned; either apply them to production surfaces or document the absence/exception in source.
- Fix the Now Playing current-spine preview first, since it is the most visible spine surface and was the review failure focus.
