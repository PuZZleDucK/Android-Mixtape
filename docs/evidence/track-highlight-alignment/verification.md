# Track highlight alignment

The track renderer used a fixed-height canvas and clamped the text's vertical origin to zero. Fonts with tall line metrics painted below the row's pressed indication and touch bounds. The existing per-font offsets apply to cassette spines, not these rows.

The track list now measures a shared alphabet's visible ink once per font/weight, centers that ink within the row, and reserves height for handwriting jitter. Selected and unselected rows use the union of their weight metrics so selection does not move the baseline. Scroll pitch estimates use the same padded height. Other handwriting contexts retain their previous layout.

## Verification

- `testModernDebugUnitTest`: 276 tests passed, no failures or skips.
- Modern debug app and instrumentation APKs built successfully.
- `connectedModernDebugAndroidTest`, restricted to `TrackHighlightAlignmentTest`: passed on Kunlun API 24.
- Direct instrumentation runs passed for all eight fonts with handwriting Off, Low and High; High also passed at system font scale 1.3. The original font scale, 1.0, was restored.
- An initial large-font run completed its assertions but failed in ActivityScenario teardown. After allowing the configuration change to settle and stopping the app, the repeat passed. Both logs are retained.
- The test measures selected ink against the production row's touch bounds and captures its real pressed indication. `before-after.png` compares four fonts; full captures and measurements include all eight.
- Seven fonts failed the old alignment check. With the fix, all eight fit inside their rows and satisfy the centering tolerance.

App deployment used `kunlun-sync.sh --apk app/build/intermediates/apk/modern/debug/app-modern-debug.apk --install-app --start-app`. Explicit paths matter: the old `build/outputs/apk/` files in this checkout are stale. No user phone was accessed.

The temporary Kunlun emulator and SSH ADB tunnel used for this check were stopped after verification. No commit or push was made.
