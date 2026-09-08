# Card 4835 counter wheels

## Scope and source findings

Planning handoff, 2026-09-08. No production code changed. Include the User's smaller toggle request. Leave case materials and spine typography alone, including unrelated dirty changes.

- `MixtapeApp.kt` calls `mixtapeCounterValue(mixtapeProgress)` in both orientations near lines 2597 and 2678. `DeckCassetteBay` owns the shared counter near line 2953. Replace only that rendering path.
- Current window is proportional to an 84 by 30 rectangle on a 560 by 400 deck. Text is centered, monospace, 14sp, 2sp letter spacing with three leading-zero digits and palette counterFace/counterInk. Preserve its settled geometry. Measuring the existing whole string avoids accidentally adding extra trailing letter spacing when splitting digits.
- `MixtapeTimeline.kt` computes rounded whole-mixtape progress times 999, not elapsed seconds. Leave it and playback polling unchanged. Existing `MixtapeTimelineTest` guards this behavior.
- `DeckViewToggle` currently allocates 64dp with 10dp internal padding. Reduce allocated clickable bounds to 48dp, scale artwork/padding to fit, retain both existing accessible descriptions and actions. Verify the released 16dp actually reaches the spine row, rather than remaining in a fixed parent slot.
- Supported deck themes are SilverfaceHiFi, BlackoutPortable, SunsetBoombox, NavyMicro, CrimsonMetal, SafetyYellow and GraphiteSlim. Cassette/case skins are independent, not extra counter implementations.
- Existing appearance reference is `docs/evidence/nothing-landscape-spine/Screenshot_20260908-205302.png`, already attached to the card. It documents settled 613, not animation direction.

## Implementation decisions

1. Add a small pure Kotlin transition reducer and frame geometry helper, then test them before wiring Compose. Inputs need the latest clamped value, normal-playback eligibility, a discontinuity identity, motion availability and active transition state. Keep the authoritative target separate from the currently visible outgoing digits.
2. Only animate adjacent forward changes while playing, with no discontinuity and no active transition. One shared progress drives all changed cells, including carry. For normalized progress p and cell height h, outgoing y = p*h and incoming y = (p-1)*h. Positive y is downward. Unchanged cells draw once at y=0. Clip each fixed cell; no fade, spring overshoot, size animation or rolling through intermediate numbers.
3. Start around 160ms with a non-overshooting tween. Draw only the target at completion. Initial composition and orientation recreation show the current target immediately. Expose one counter accessibility value, not separate outgoing/incoming semantics.
4. Duplicate target updates do not restart a transition. A new target while busy cancels and snaps to the newest value. Cancel stale completion callbacks with a generation token or structured coroutine cancellation. No queue. Pause settles the current target, never synthesizes an increment.
5. Reset, stop, seek, rewind/fast-forward, track/mixtape change, decreases and multi-count jumps snap immediately. A +1 seek must also snap: integer delta alone cannot establish normal playback. Trace `MixtapeController.seekTo`, stop and track selection through the UI state. Reuse an existing discontinuity signal if available, otherwise add a presentation revision on these events without changing timing or transport behavior. Handle external media-session actions, not just on-screen buttons. Ordinary position updates must not advance that revision.
6. Use Compose's duration-scale-aware animation mechanism. Scale zero must settle without pending animation; cancellation and reduced-motion paths must not leave an outgoing digit. Do not build a wall-clock ticker or a private animation setting. Test the platform scale path on the compatible emulator.
7. Extract a small wheel composable so instrumentation can supply values and a controlled Compose clock without playing a specially timed recording. Keep production state injection internal and avoid a user-visible debug screen.

## Deterministic tests to implement first

Use a reducer test table plus Compose mainClock with autoAdvance disabled. The source red guards added at Planning are only integration tripwires, not behavioral coverage.

| Input or event | Expected result |
| --- | --- |
| initial 0, 613, 999 | settled 000, 613, 999, no entrance motion |
| 008 to 009 | only units moves; hundreds/tens remain at zero offset |
| 009 to 010 | tens and units move together; settled 010 |
| 099 to 100 | all three move together; settled 100 |
| 998 to 999 | units only; no wrap past 999 |
| 999 to 0; 100 to 99; 613 to 700 | immediate target, no skipped-value playback |
| seek +1 or track change +1 | immediate target despite adjacent forward delta |
| update 009 twice during 008 to 009 | same progress/start generation, not restarted |
| 008 to 009, then 010 before completion | cancel old transition, settle 010; old completion cannot restore 009 |
| reset during carry, then old completion | remains 000 |
| pause during motion; repeated paused samples | settles latest target and remains still |
| animations disabled before or during motion | settled latest target, no queued work |
| out-of-range input | clamp to 000 or 999, exactly three digits |
| recreation/rotation during motion | current target only, no stale remembered source |

For 009 to 010 and 099 to 100, assert frames at p=0, 0.25, 0.5, 0.75 and 1. At halfway the old glyph is +h/2, new glyph is -h/2. Assert cell rectangles and overall width never change, only changed cells contain two clipped glyphs, and final frame contains exactly the target. Assert distinct stale completions cannot overwrite later updates. Instrumentation should verify clipping pixels as well as reducer math, since a passing reducer does not prove rendering uses it.

## Verification and evidence before Review

- Run focused reducer/frame tests, `testModernDebugUnitTest`, `assembleModernDebug` and `lintModernDebug`. Separate existing unrelated red tests from regressions, never fix other cards here.
- Deploy an x86_64-compatible updated modern APK with `kunlun-sync.sh` to the Kunlun emulator. Follow the emulator skill and repository workflow. This Planning stage changes no app code and does not claim an updated deployment.
- Run connected Compose tests on Kunlun. Capture ordered emulator frames for 008 to 009, 009 to 010 and 099 to 100 at a controlled clock. Annotate order/time outside the counter crop; include a full-screen context image. A still final numeral alone is not roll evidence.
- Check all seven deck themes in portrait and landscape for settled value, mid-carry containment and the smaller toggle. A contact sheet is sufficient for the theme/orientation survey. Verify a 48dp touch target and correct toggle action, and that spine width gained space without typography changes.
- Smoke normal playback, pause/resume, scrub seek in both directions, rewind/fast-forward, stop/reset, track/mixtape changes and rotation. Check zero animation scale, restore the original scale after testing, and record results. No physical device gate.
- Store essential media in `docs/evidence/counter-wheels/` through Git LFS and verify pointers before committing. Disposable frames/downloads belong in ignored `.work/card-4835/`. Attach the useful frame sequence/contact sheet to the card and give exact commands/results for state tests.

## Actual Planning red run

`CounterWheelUiContractTest` compiles and ran two tests; both fail for the expected missing work: static whole-string replacement and the 64dp toggle. This does not claim deterministic animation coverage yet.

Reproduce in this worker environment:

```sh
JAVA_HOME=/home/puzzleduck/.asdf/installs/java/temurin-21.0.10+7.0.LTS \
ANDROID_HOME=/home/puzzleduck/Android/Sdk \
./gradlew testModernDebugUnitTest --tests '*CounterWheelUiContractTest' --console=plain
```

The initial invocation had no Java on PATH; using the installed Java 21 resolved it. Real result: 2 tests completed, 2 failed, BUILD FAILED in 1m 24s. Durable console output: `docs/evidence/counter-wheels/planning-red-tests.txt`. No runtime or animation evidence exists from this stage. Running must add the deterministic tests above and make them pass with the implementation before Review.
