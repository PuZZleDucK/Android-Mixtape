# Card 2212 — fresh transparent logo testing update

## Current rework target

The Review note says: "just generate a new transparent logo graphic". The next Running pass should create fresh transparent cassette logo source art, not continue masking/cropping the prior opaque square asset.

## Added failing TDD coverage

`LauncherIconResourceContractTest` now requires:

- `app/icon-source/android_mixtape_logo_transparent_1024.png` exists as a freshly generated 1024x1024 transparent-logo source.
- That source PNG has alpha, transparent corners, and a cassette visible-width ratio between 80% and 92% of the canvas.
- The source is not a byte-for-byte copy of `app/icon-source/archive/ic_launcher_cassette_1024_card2212_original.png`.
- `scripts/generate_launcher_icons.rb` consumes `android_mixtape_logo_transparent_1024.png` and no longer uses the old archived opaque source or hard-coded mask polygon/CopyOpacity workflow.
- Existing launcher export contracts still cover transparent PNG backgrounds, larger visible cassette bounds, legacy density sizes, adaptive foreground density sizes, and transparent adaptive backgrounds.

## Expected status before Running

These tests are expected to fail until Running generates the fresh transparent logo source and rewires the icon generator/resources:

```sh
./gradlew testModernDebugUnitTest --tests com.example.androidmixtape.icons.LauncherIconResourceContractTest
./gradlew testLegacyDebugUnitTest --tests com.example.androidmixtape.icons.LauncherIconResourceContractTest
```

Expected first failures on the current tree:

- missing `app/icon-source/android_mixtape_logo_transparent_1024.png`
- generator still references the old archived opaque source and mask workflow

Running should make those tests pass, then run `assembleModernDebug assembleLegacyDebug` and capture checkerboard/logo evidence for Review.
