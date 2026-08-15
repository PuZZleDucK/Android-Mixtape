# Card 2214 — Cassette-spine briefcase UI test plan

## Expected behavior

- The modern Mix Tapes screen renders dense horizontal cassette spine rows filed inside a dark briefcase/case frame.
- `MixTapeLibrary` no longer uses the large front-facing `CassetteCaseCard` rows or sparse shelf-line background.
- Each row exposes `Cassette spine <mix tape name>` semantics, stays tappable at 56.dp or taller, and preserves tap-to-open behavior.
- Row copy is compact and readable: mix tape name, `NO.##`, track count, ellipsized one-line labels, and at most one optional first-track/artist hint.
- The full cassette aspect-ratio contract remains only for the Now Playing cassette drawing, not for Mix Tapes spine rows.
- Legacy/API 19 build behavior is not intentionally changed by this visual pass.

## Added/updated failing checks for Running

- `CassetteSpineReadabilityContractTest`
  - Requires `MixTapeLibrary` to render `CassetteSpine*` rows instead of `CassetteCaseCard`.
  - Requires spine semantics, 56.dp+ touch target, ellipsis, compact metadata, and no old three/four-song preview block.
- `CassetteVisualAspectRatioContractTest`
  - Requires a warm dark/brown/brass briefcase frame in `MixTapeLibrary`.
  - Requires Mix Tapes spine rows not to use `CASSETTE_VISUAL_ASPECT_RATIO`.
  - Keeps the Now Playing cassette aspect-ratio guard.
- `MixtapeAppTest.mixTapesScreenRendersCassetteSpineRowsWithCompactReadableLabels`
  - Updates modern Compose UI expectations from `Cassette case ...` to `Cassette spine ...` semantics and compact one-preview behavior.

## Edge cases to cover in implementation

- Long mix tape names must ellipsize within one row instead of growing row height.
- A single-track final tape still shows a track count and first-track hint without empty preview slots.
- Enough rows should be visible on portrait phones without dropping below minimum touch target size.
- Now Playing cassette visuals, permissions, empty/error states, settings, playback controls, and legacy build remain unaffected.

## Verification commands

Expected immediately after this Testing stage: modern unit checks fail because the UI has not been refactored yet.

```sh
cd /home/puzzleduck/x/android-mixtape
./gradlew testModernDebugUnitTest --tests '*CassetteSpineReadabilityContractTest' --tests '*CassetteVisualAspectRatioContractTest'
```

After Running implementation:

```sh
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
./gradlew assembleModernDebug assembleLegacyDebug
```

If an emulator/device is available:

```sh
./gradlew connectedModernDebugAndroidTest
```
