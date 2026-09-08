# Card 4833 plan and red evidence

## Scope and findings

Planning only. No production code changed. Reuse card 4831 at 1fe0346 and its evidence in `../cassette-spine-title/implementation.md` and `review.md`. Its 2,160 passing raster cases establish vertical fitting for eight fonts at five widths and two densities. Do not rerun that unchanged matrix merely for the handoff. If the raster implementation changes, its covered bytes change and the matrix becomes a relevant regression run.

Local source research found the remaining horizontal defect in `SpineTitleRaster.kt`. Drawing starts at cursor zero into a bitmap exactly as wide as the title viewport. Negative italic/jitter overhang is therefore clipped before alpha measurement. Only top and bottom are measured. `JitteredHandwritingTextRenderer.kt` then draws the full-width bitmap at x=0. Short titles cannot center their ink this way. This does not imply that every font still has a vertical defect.

`CassetteSpineRow` already reserves proportional padding, a 28-unit badge and a 10-unit gap. At the reference 280-unit width the title viewport is 206 units wide. Center against that viewport, not the case or number badge. Keep the proportional size and vertical raster fitting. The old baseline-offset use is in the legacy row, not the accepted production spine path.

The list, Now Playing and `HandwritingFontContextPreview` call `CassetteSpineRow`. The font settings preview uses `Night Drive` and a font-derived jitter seed, so current previews alone do not isolate font effects. Component theme previews also reuse the row. Track-list handwriting is a separate path and must retain its current behavior.

## Failing behavior defined first

Added `SpineTitleHorizontalPlacementTest.fittingInkCentersWithinReservedTitleViewport`. It measures real Compose glyph pixels from the production raster helper. Eight bundled families, widths 180/280/480dp, seeds 971/42, titles `W` and `gj`, italic and the existing effective ExtraBold weight, normal spine jitter, density/fontScale 1. These deliberately short strings fit. Ink must have one-pixel side clearance and its horizontal bounding-box midpoint must be within 0.5px of the title viewport midpoint.

Executed on Kunlun API 24 emulator-5554. All 96 cases failed the placement assertion, not compilation or installation. Example: Kalam, 180dp, `W`, seed 971 has ink [5,31) inside a 132px viewport. Full per-font measurements are in `planning-red.xml`; Gradle output is in `planning-red.txt`. This is helper-level evidence, not a completed visual audit or proof of intact overhang. The test currently specifies centering inside the returned raster. If implementation returns a positioned crop instead, update the test to measure the composed result rather than weakening the expected position.

Both modern debug and test APK assembly succeeded. App APK SHA-256 was `e117823ef233efe3fd7ff67b3235c03607b5a9499132d52b00c817bc5815d8c1`. Built from the existing dirty tree with x86_64 and testOnly=false, not an isolated release build. Deployed through `kunlun-sync.sh`, then ran only the new connected class using a verified SSH ADB forward on port 15037. Connected testing removed the app afterward, so reinstalled through the sync script and granted emulator audio permission before the final capture.

`planning-baseline.png` is a raw 1920x1080 Kunlun library capture after that reinstall. It confirms the deployed app and current spine layout, not a controlled multi-font comparison. Discarded launcher/permission captures. Build/deployment logs and disposable diagnostics are under ignored `.work/card-4833/`. The PNG is tracked through Git LFS.

## Implementation sequence

1. Capture a controlled before audit on Kunlun. Use the same ID, paper, case, title, jitter seed and row size while changing only font. Use Kalam, PatrickHand, Caveat, NanumPenScript, IndieFlower, GloriaHallelujah, ArchitectsDaughter and ShadowsIntoLight. Prefer a blank high-contrast paper and clear case to distinguish ink from grid lines and gloss. Do not change case styling under this card.
2. Use `W`, `Agjpqy`, `Night Drive`, `ÉÅgj`, `restless neon weekend`, and `long spaced title `.repeat(8). Include a long unbroken title and whitespace. Record per-font visible bounds and high/low/left/right classification for each fitting title. Keep the long-title results separate; their clipping is intentional. Do not infer font identity from the existing Nothing-phone screenshots.
3. Extend the existing raster pipeline to discover complete transformed ink bounds for fitting candidates, including negative bearings and rotation. Translate fitting ink into the title-only viewport. Do not use advance width as an ink bound, hard-code family offsets, replace fonts, or shrink to fit horizontally. Bound scratch allocation and work for arbitrarily long titles. Avoid allocating a bitmap proportional to the entire unbounded title.
4. For overflowing titles retain the current left-anchored, single-run clip behavior. No new scrolling exists in the current spine path; preserve any scrolling in other callers. After horizontal translation, measure the actually visible ink for the existing vertical fit. Reuse the existing rerender-on-shrink loop and one-pixel vertical clearance.
5. Make the new red test pass. Add a padded-reference comparison that proves negative italic/jitter overhang is retained for fitting titles, plus just-fits/just-overflows cases. Add focused checks for long-run anchoring, unchanged chosen size, blank input and seeds that shift the first glyph left. The current red check alone cannot detect lost pixels that are subsequently centered.
6. Verify final decorated rows in list, Now Playing and font settings previews in portrait and landscape. Capture all eight fonts with identical short and long fixtures. A compact labeled contact sheet may accompany raw screenshots, but keep it separate from raw emulator captures. Exercise representative small preview and large list widths, plus one nondefault fontScale. Record actual density, fontScale, seed, title and dimensions. Compare badge coordinates and reserved gaps before/after. Confirm track-list font identity and playback continuity during rotation.

## Completion criteria for later stages

- New placement test passes with actual glyph pixels and no fitting-title side clipping.
- Focused overhang, threshold and long-title checks pass; any changed vertical raster behavior passes the existing matrix.
- Per-font before/after table and comparative raw captures show centered visible ink in every applicable context and both orientations. Document remaining optical differences without erasing font character.
- Modern unit tests and x86_64 debug build pass. Deploy final app via `kunlun-sync.sh` before Review and attach comparative evidence. Reuse unchanged test receipts; do not claim the planning screenshot satisfies the final visual matrix.
- Preserve unrelated working-tree edits. Durable PNGs use Git LFS; scratch assets stay in `.work/`. No physical-device access or push is needed.

Run-owned emulator, SSH forward and idle build daemons are stopped at the end of planning. The outstanding audit and implementation belong to this card, not another active dependency.
