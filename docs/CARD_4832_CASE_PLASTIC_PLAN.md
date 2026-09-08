# Card 4832 case plastic plan

## Scope and source findings

Change only case plastic. Preserve theme enum identities and persistence, paper/sticker artwork, handwriting, hit regions, scroll state and playback. Card 4831 is Done. Keep its title fitting and centering checks intact. The working tree already has unrelated edits, including MixtapeApp.kt; do not reset or stage them wholesale.

Primary references are `docs/evidence/prototype-case-plastic/README.md` and its four already-attached captures. Inspected prototype sources are `/home/puzzleduck/x/astra6/tape/js/components/packaging.js` and `js/themes.js`.

The prototype draws paper before plastic. A broad translucent fill is independent of the edge, inset seam and white rim. Full cases add hinges, latch and a 0.10-alpha diagonal reflection. Spines retain only long-edge highlights and two seam lines. Its 3-unit SVG stroke spans a 600-unit case; copying that as 3 Android dp would exaggerate thickness. Clear/Amber/Rose/Smoke fill alpha is 0.17/0.31/0.26/0.30. Borrow the separation of optical layers, not literal colors or typography.

Android inventory in `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`, line numbers at planning time:

| Entry | Existing difference | Work |
| --- | --- | --- |
| CaseComponentPreview, 846 | Opaque dark outer backing, full-strength tint/haze, 2 dp edge | Leave parent visible outside inset paper; share material policy |
| HandwritingFontContextPreview, 1157 | Hardcoded CrystalClear, 2 dp border and separate tint/edge drawing | Keep its theme choice; share material |
| CasePlasticPalette/plasticPalette, 1799 | Tint, haze, edge, hinge; nine existing identities | Separate optical values from geometry; preserve every identity |
| CassetteSpineRow, 1820 | 0.62 tint multiplier, 1.8 dp inset edge, circular hinges | Share optical layers; retain title metrics and interaction |
| CurrentTrackPreview, 2367 | 3 dp Card border plus tint/haze in scaled content | Remove redundant plastic border; account for graphicsLayer scaling |
| CassetteCoverTrackList, 4177 | 3 dp border plus tint/haze and another 1.5 dp edge | One plastic rim, paper borders left alone |

SpineSkinPreview delegates to CassetteSpineRow. Search again for `plasticPalette`, `casePlastic`, `plastic.tint` and `CASE_PLASTIC_TINT_STRENGTH` after extraction to catch missed paths. Do not modify retired renderers unless a live call site requires it.

## Implementation sequence

1. Before edits, capture matched baseline emulator views. Record APK hash, source diff hash, emulator API/density, orientation, font/size, case and sleeve IDs, background and fixed track/title fixture. Preserve these settings for after captures.
2. Introduce a small shared DrawScope material renderer with full-case/spine geometry modes. Draw paper and its text first, then one tint/haze policy, thin edge, inset seam, rim light and restrained reflection. No opaque whole-case backing. Keep overlays draw-only, with no pointer or semantics nodes.
3. Centralize resolved tint/haze strength. Avoid applying the 0.62 multiplier twice or leaving previews unscaled. Treat CloudyClear's haze as intentional, but test combined opacity. Start with interior transmission of at least 65 percent for clear and 60 percent for other themes away from highlights. These are acceptance budgets, not copied prototype constants.
4. Make rim thickness proportional to the short dimension with density-aware limits. Start near 0.5 to 1.25 visible dp. Clamp tiny geometry to nonnegative dimensions. Skip hinge/reflection detail when it cannot fit without covering the label. Account for CurrentTrackPreview's outer scaling rather than assuming source dp equals displayed dp.
5. Route all five active drawing entries through the same material policy. Remove plastic-only Card outlines; do not strip printed sleeve lines or album artwork borders.
6. Add compositing and integration tests below, run modern unit/build/lint, deploy using kunlun-sync.sh, then capture and attach matched before/after evidence. Do not call the material finished from source guards alone.

## Expected behavior and regression coverage

The added `CasePlasticMaterialContractTest.kt` guards three known defects. Its Ruby companion, `ruby scripts/check_case_plastic_material.rb`, permits real source checks without Android tooling. These negative source tests are an initial red fence, not proof of a correct renderer. They can be bypassed by moving bad code, so supplement them during implementation:

- Pure material tests for all nine themes: finite channel/alpha values, combined transmission `(1 - tintAlpha) * (1 - hazeAlpha)` within the budgets, stable theme identity and identical resolved optical values for case/spine/preview. Test zero and very small bounds, tall/wide aspect ratios and densities 1, 2 and 3.
- Compose bitmap tests: draw a known two-color paper and an exposed parent border; sample away from seams/reflections. Verify source-over output against `alpha * tint + (1 - alpha) * paper` with rounding tolerance, and verify changing the parent changes exposed plastic pixels. Include CloudyClear's second layer. Do not apply alpha to the entire paper/content group.
- Compare full/spine interior samples with the same theme and paper within two channel levels. Assert an inset seam/rim is distinguishable on both light and dark backgrounds, and that no second heavy outline remains.
- UI tests keep title text, clickable bounds and saved theme IDs unchanged. Rerun CassetteSpineTitleSizingContractTest and existing component preview, landscape and scroll contracts. Check long title, large font, empty track list and rotation during playback. No track-list redesign.

## Visual acceptance matrix

Use CrystalClear, AmberTint, ElectricBlueClear and SmokeTint with Album and Lined paper. Capture each in portrait and landscape against both light and dark surroundings, 32 combinations in a compact evidence sheet. Review case and spine together where possible. Separately check all nine settings thumbnails, CloudyClear over grid, and the handwriting/current-track previews. If the app cannot select both surroundings, use a test-only Compose scene with the production renderer; label that evidence separately from runtime screenshots.

Pass when paper rules/art remain visible, labels are no less readable than baseline, color identities remain recognizable, rims suggest thickness without a dark frame, and reflection does not obscure text. Compare identical fixtures side by side, not unrelated screenshots. Preserve scroll/tap/seek/playback behavior and selections across relaunch.

Use the compatible Kunlun emulator only. Read the emulator skill before deployment. Build an x86_64-compatible modern APK and use `./kunlun-sync.sh --all PATH` or its install/start/screenshot options with `--apk PATH`. Never use the Nokia for this card. Record storage failures as environment limitations, not product failures. Keep disposable APK copies and captures under ignored `.work/card-4832/`; store selected durable PNGs through Git LFS under `docs/evidence/case-plastic/`, with settings/hash receipts, and attach matched images to the card. Existing prototype references do not substitute for Android before/after captures.

## Planning execution evidence

2026-09-08: `ruby scripts/check_case_plastic_material.rb` executed and exited 1 with 3 runs, 6 assertions, 3 failures, 0 errors, 0 skips. Failures correspond to the opaque dark preview backing, independent 2/3 dp case border and preview's direct unscaled tint. Raw disposable output is `.work/card-4832/source-red.log`.

Attempted `./gradlew testModernDebugUnitTest --tests '*CasePlasticMaterialContractTest' --console=plain`. It could not start: JAVA_HOME is unset and java is absent from PATH. No JUnit execution is claimed. Output is `.work/card-4832/red-tests.log`. No Gradle daemon started. Restore the documented Java 21/SDK build environment before Android verification.

No production renderer changed in Planning. No updated APK exists from this stage, and no build, deployment or Android screenshot verification is claimed. Those remain required before implementation completion. Refreshed card before handoff; no newer User direction was present.
