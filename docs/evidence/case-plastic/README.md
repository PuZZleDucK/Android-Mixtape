# Case plastic, card 4832

## Implementation

`CasePlasticMaterial.kt` resolves the existing nine theme palettes once and draws the shared sheet, rim, seam and molded details. All five live paths in `MixtapeApp.kt` use it. Runtime tint values remain unchanged. Settings previews now use that same opacity instead of the stronger raw tint.

Removed the opaque backing from case thumbnails and runtime spines, the independent 2/3 dp Card borders, the second dark spine outline and the broad spine reflection. The rim is limited to between 0.5 and 1 displayed dp, with smaller bounds clamped safely. Full cases have a 4.5 percent diagonal reflection and small hinge/latch details. Current-track preview drawing compensates for its graphics-layer scale. Paper artwork, printed borders, title layout, theme IDs and interactions were not edited.

## Matched emulator evidence

The eight named theme/orientation sheets pair before and after captures. Each row uses one sleeve paper. Columns are before spine, after spine, before playback view, after playback view. Themes are CrystalClear, AmberTint, ElectricBlueClear and SmokeTint. Papers are AlbumPrint and RuledNotebook. There are 32 before/after pairs across portrait and landscape. `previews.png` shows the updated settings thumbnails while scrolling.

Target was Kunlun's API 24 emulator-5554, x86_64/x86, 1080 × 1920 at 420 dpi. The app was built locally and installed, launched and captured with `kunlun-sync.sh --apk ...`. No physical device was accessed. The default build is emulator-compatible without an injected ABI flag.

Both builds used the same pre-existing two-mixtape library, names, fonts, jitter seeds and deck. For each theme/paper pair, the test changed only the emulator's case_theme and sleeve_theme preferences, then restarted the app. Rotation was 0 or 1 with automatic rotation disabled during capture. Spines sit over the dark tray; playback views sit over the light app background. AlbumPrint is dark and RuledNotebook is light. Background transmission on both light and dark parents is also covered by the bitmap test, rather than claiming a full background-toggle screenshot matrix.

Playback remained active in the captures, so meter values, counters and status-bar times are not pixel-matched. Material, paper, title and layout comparisons are matched. Existing long-title clipping is visible in both versions and was not changed under this card.

Visual review found thinner frames, visible paper rules and unchanged label contrast away from the removed reflection. Clear spines now reveal their actual surroundings instead of an opaque black frame. Warm and cool identities remain visible over both papers; smoke retains its dark sheet without a second dark outline. The settings examples show exposed parent color around the inset paper.

## Verification

- `testModernDebugUnitTest`: 276 tests, zero failures/errors. Includes the four material integration guards and the existing spine-title sizing contracts.
- `assembleModernDebug` and `assembleModernDebugAndroidTest`: successful.
- `ruby scripts/check_case_plastic_material.rb`: 3 tests, 6 assertions, zero failures.
- Kunlun instrumentation `CasePlasticCompositingTest`: 3 tests passed. It renders the production DrawScope into Android bitmaps for all nine themes, full case/spine, densities 1/2/3, light/dark parents and two contrasting papers. Interior source-over samples match within two channel values. Transmission is at least 65 percent. Rims remain distinguishable; zero/tiny/tall/wide bounds do not throw.
- Kunlun instrumentation `MixtapeAppTest`: 6 tests passed, covering navigation, permissions, ready state and settings callbacks.
- `lintModernDebug`: ran and failed with seven errors in unchanged media-service and Android Auto diagnostics code. One WrongConstant error is at MixtapeMediaLibraryService.kt:132; six UnsafeOptInUsageError findings are at that file's lines 114/118/135 and AndroidAutoDiagnostics.kt:123/124/125. No lint error points to the material changes. These unrelated files were not edited or suppressed.

The first combined lint run exceeded its 200-second command timeout. The next completed and produced the lint findings above. Initial ABI-injected APK attempts either selected stale outputs or failed with INSTALL_FAILED_TEST_ONLY. Those captures and failed deployments were discarded. Both final screenshot sets were recaptured after successful normal builds and installations. The final APK hashes below identify the builds actually used.

After testing, the original visual preferences were restored byte-for-byte. The updated app was relaunched. The emulator started by this run was then stopped; Gradle/Kotlin daemons exited after `gradlew --stop`. No production service was changed.

## Hash receipts

| Artifact | SHA-256 |
| --- | --- |
| Before APK | c9aa6b1caa183ce0d5a1ff67ed7872698a80500921b5f790952bf92c26b1c03b |
| After APK | a897c8e72e7a0dc609134de816da8427fd7ea3e319acc0647fd30d2e7d85da7f |
| Before MixtapeApp.kt working-tree bytes | 6b6ead2d2102126e272c9f6be428ec377cfc1102c2f3e3527d94012729b9388f |
| After MixtapeApp.kt working-tree bytes | b810796f139d800f368ef7f87feb67942d3eb994530a0e9395c43d6445d9b79a |
| CasePlasticMaterial.kt | c3342d106b1045e8b9fef0fdfec5a8dc300bd6dd4e13aa833f05afd680492dd1 |

Build/test evidence covers the existing working tree, which contains unrelated changes. Only card-owned renderer hunks, tests, planning and evidence are committed. PNG evidence and prototype references use Git LFS. The APKs and intermediate captures were disposable local test artifacts, not release builds.

Java was installed but absent from the worker PATH. Commands used `JAVA_HOME=/home/puzzleduck/.asdf/installs/java/temurin-21.0.10+7.0.LTS` and `ANDROID_HOME=/home/puzzleduck/Android/Sdk`; no tooling installation was necessary.
