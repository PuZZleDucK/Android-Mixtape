# Card 2499 — Cassette/case/spine aspect-ratio bug plan

## Goal

Remove stretched or squashed cassette visuals by making every cassette-like surface use an explicit, named physical aspect-ratio contract instead of ad-hoc fixed heights, incidental `fillMaxWidth()` sizing, or reused ratios from a different object.

Review rollback clarification: constants alone are not enough. The next test/implementation pass must prove each shell/case-front/back/spine ratio is either applied to a production layout that draws that physical surface, or has a narrow documented production exception explaining why that surface is viewport/touch-target constrained and cannot use a strict ratio.

## Research notes and target ratios

Primary references checked:

- Wikipedia, “Cassette tape”: compact cassette shell is described as 4 × 2.5 × 0.5 in / 10.2 × 6.35 × 1.27 cm at largest dimensions.
- Wikipedia, “J-card”: cassette J-card dimensions are 4 in high; front flap 2 9/16 in; title/spine flap 1/2 in; tracklisting flap 1 1/16 in.

Use these as the app’s canonical ratios, with small tolerance for rounded corners/perspective:

| Surface | Physical dimensions | Target W:H ratio | App use |
| --- | ---: | ---: | --- |
| Bare compact cassette shell | 4.0 × 2.5 in; exact references often around 100.4 × 63.8 mm | 1.57–1.60 | Now Playing `CassetteTape` full cassette drawing |
| Cassette case / J-card front cover | 4.0 × 2 9/16 in visible front art | 1.56 | Any front-facing cassette case/card/cover view |
| Plastic Norelco-style storage case outer front | common cases are roughly 108 × 70 mm | ~1.54 | If the app draws the full plastic case rather than just paper art |
| J-card title/spine label | 4.0 × 0.5 in | 8.0 | Horizontal cassette-spine rows / spine preview surfaces |
| Full plastic case spine/depth | roughly 108 × 17 mm when representing the whole box spine | ~6.35 | Acceptable if the row intentionally includes thick plastic rails/end caps |
| J-card tracklisting/back flap | 4.0 × 1 1/16 in | 3.76 | Track list panel if styled as the folded J-card back |

Planning decision: do not force all cassette UI to `CASSETTE_VISUAL_ASPECT_RATIO`. The bug likely comes from using one ratio or fixed height across shell, case, and spine surfaces. Add separate constants and tests.

## Current local touchpoints

- `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`
  - `CASSETTE_SHELL_ASPECT_RATIO`, `CASSETTE_CASE_FRONT_ASPECT_RATIO`, `CASSETTE_SPINE_LABEL_ASPECT_RATIO`, and `CASSETTE_CASE_SPINE_ASPECT_RATIO` now exist, but the review rollback found only the shell ratio is actually used by production code.
  - `CassetteTape(...)` uses `.aspectRatio(CASSETTE_SHELL_ASPECT_RATIO)`, which is correct for Now Playing full cassette shell art.
  - `CassetteSpineRow(...)` is row-height based (`heightIn(min = 64.dp)`). It may remain viewport-width/touch-target driven only if the code and tests name that exception explicitly; otherwise spine previews should use `CASSETTE_CASE_SPINE_ASPECT_RATIO` or `CASSETTE_SPINE_LABEL_ASPECT_RATIO`.
  - `NowPlayingModeToggleAndSpine(...)` no longer uses the old `height(58.dp)` slot, but it still needs an explicit production ratio/exception contract because it renders the most visible current-mixtape spine preview.
  - `StaticMixtapeStackPreview(...)` draws mini-spines inside a square 64dp toggle. Either draw those mini-spines near a spine ratio inside the icon, or document/test an icon-only exception so reviewers do not count it as an accidental squash.
  - `CassetteCoverTrackList(...)` is styled as a J-card tracklisting/back panel and fills the remaining Now Playing pane. The next pass must decide whether it should use the J-card back-flap ratio (`4 / 1.0625 = 3.76`) in any non-scrolling/static preview, or document the scrolling-paper exception for the production track list.
  - No obvious front-facing cassette case/J-card cover surface currently uses `CASSETTE_CASE_FRONT_ASPECT_RATIO`. If one exists in settings/previews it must apply that ratio; if none exists, keep the constant only with a documented “no current production front-cover surface” exception.
- Existing tests to extend:
  - `CassetteVisualAspectRatioContractTest`
  - `CassetteSpineReadabilityContractTest`
  - `MixTapeLandscapeTwoColumnContractTest`
  - Connected/manual screenshot tests around Mix Tapes and Now Playing portrait/landscape.

## Implementation plan

1. Add/rename constants in `MixtapeApp.kt` with source comments:
   - `CASSETTE_SHELL_ASPECT_RATIO = 100.4f / 63.8f` or `4f / 2.5f` (acceptable target 1.57–1.60).
   - `CASSETTE_CASE_FRONT_ASPECT_RATIO = 4f / 2.5625f` (1.56) for front-facing case/J-card art.
   - `CASSETTE_SPINE_LABEL_ASPECT_RATIO = 4f / 0.5f` (8.0) and optionally `CASSETTE_CASE_SPINE_ASPECT_RATIO = 108f / 17f` (6.35) for full plastic spine rows.
2. Replace generic `CASSETTE_VISUAL_ASPECT_RATIO` call sites with the surface-specific constant. Keep Now Playing on shell ratio.
3. Audit every cassette/case/spine composable for fixed heights paired with width fill:
   - full cassette art should use shell ratio;
   - front-facing case/card should use case-front ratio;
   - tracklisting/back paper should use the J-card back-flap ratio where it is a bounded card/preview, or document that the production Now Playing list is scrollable paper filling the remaining pane;
   - spine rows should either maintain a ratio within 6.35–8.0 where they are preview cards, or document a scroll-list/touch-target exception where width is determined by phone viewport but the visual details still read as a spine.
4. Fix the most suspicious compression points first:
   - `NowPlayingModeToggleAndSpine` current-spine preview should have an explicit ratio-aware or exception-named modifier; a bare `.weight(1f).heightIn(min = 64.dp)` should not be the final undocumented contract.
   - `StaticMixtapeStackPreview` should either draw ratio-correct mini spines inside the square icon or expose a named icon-only exception.
   - any front-cover/track-list panels that look stretched should get `aspectRatio(CASSETTE_CASE_FRONT_ASPECT_RATIO)`, a new J-card back-flap ratio, or a documented scrolling-paper exception as appropriate.
5. Update/extend source contract tests before another implementation attempt:
   - assert the named constants exist and are used in production where their physical surfaces exist;
   - fail if any aspect-ratio constant is orphaned except for an explicitly documented “no current production surface” exception;
   - assert `CassetteTape` does not use case/spine ratios;
   - assert spine preview/list code does not use the shell/front ratio accidentally;
   - assert `NowPlayingModeToggleAndSpine`, `StaticMixtapeStackPreview`, `CassetteSpineRow`, and `CassetteCoverTrackList` each carry either a production ratio use or a named/documented exception;
   - add numeric tolerances so future changes stay near the researched physical dimensions.
6. Add a connected/manual visual check using the modern build:
   - Mix Tapes portrait list with 6+ spines;
   - Mix Tapes landscape two-column list;
   - Now Playing portrait cassette;
   - Now Playing landscape left cassette + current-spine toggle area;
   - Settings previews for tape skins and spine skins.

## Success criteria

- Bare cassette drawings look like compact cassettes: wide but not flattened, ratio ~1.58.
- Front-facing cassette case/J-card surfaces use ~1.54–1.56, not arbitrary wide/short layouts.
- Spine surfaces read as long, thin labels/box spines and avoid accidental cassette-shell proportions.
- The current-mixtape spine preview in Now Playing no longer squashes symbol/title artwork in narrow panes.
- Tests encode shell/case/spine/back ratios separately so regressions are caught before screenshots.
- Source audit shows no orphaned aspect-ratio constants except documented “no current production surface” constants.
- Every production cassette-like view (`CassetteTape`, `CassetteSpineRow`, `NowPlayingModeToggleAndSpine`, `StaticMixtapeStackPreview`, `CassetteCoverTrackList`, and any settings previews) has a ratio use or named exception.
- Modern and legacy unit tests/builds still pass.

## Verification commands

```sh
cd /home/puzzleduck/x/android-mixtape
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleModernDebug assembleLegacyDebug
```

If a compatible emulator/device is available, also capture screenshots in portrait and landscape for Mix Tapes, Now Playing tracks mode, Now Playing mixtapes mode, Tape Skin settings, and Spine Skin settings.

## Risks / guardrails

- Do not “fix” spines by making list rows too short for touch targets; preserve at least the existing 56–64 dp minimum target.
- Do not change playback, grouping, skin persistence, or settings behavior while fixing layout geometry.
- Avoid pixel-perfect assertions on flexible Compose lists; use ratio ranges and screenshot/manual checks.
- Keep the API 19 legacy build compiling even if only modern Compose receives visual polish.
