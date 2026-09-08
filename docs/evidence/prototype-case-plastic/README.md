# Prototype case-plastic references

Captured from the running Form & Frequency prototype on 2026-09-08.

Prototype directory: `/home/puzzleduck/x/astra6/tape`
LAN URL: <http://artigas.local:8088/>

Screenshots:

- `workbench-clear.png`: Workbench, Tape & case, Clear plastic, Album paper.
- `workbench-amber.png`: Same composition and paper, changing only case plastic to Amber. Compare the transmitted tint and edge definition without a paper/theme change.
- `case-library.png`: Component library, Cases & sleeves filter. Clear, Amber, and Rose cases and matching spines over the transparency checkerboard.
- `smoke-case-and-spine.png`: Same library scrolled to Smoke case and matching spine; also shows the separate sleeve paper components.

Implementation references:

- `js/components/packaging.js`: `caseShell(theme, { spine })`, `cassetteCase`, and `objectsView`. The paper is rendered first, then the plastic overlay. Case surfaces use translucent tint; edges, inset rims, hinges, latch, fine white highlight strokes, and a low-opacity diagonal reflection remain distinct.
- `js/themes.js`: `cases` tokens separate `tint`, `edge`, and `opacity`. Prototype surface opacity values are Clear 0.17, Amber 0.31, Rose 0.26, and Smoke 0.30. These are reference values, not mandatory Android tuning constants.
- `README.md`: component coordinate system and theme API.

Transfer the plastic material treatment, not the entire prototype design. Preserve Android Mixtape's sleeve art, handwriting, selections, controls, and layouts. Its printed artwork borders should not be confused with heavy plastic outlines. Keep this work separate from spine-title centering/scaling card #4831.

All four screenshots came from real browser rendering. The existing shared `cassette-studio.service` was reused, not started or modified for this capture.
