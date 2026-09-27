# Native demo UI

The phone UI is a **native Compose/Canvas port** of the approved cassette studio. It does not contain a WebView, run JavaScript, fetch web assets or use prerecorded animation. Music still comes from Android's MediaStore and plays through the existing Media3 service.

## Components

All rendering files are in `app/src/modern/java/com/example/androidmixtape/ui/`.

- `DemoThemeCatalog.kt`: data-only theme resolution, layout geometry, area-conserving winding radii, optical transmission and readable ink.
- `DemoCassette.kt`: shell, sticker, printing, five screws, ribbon tangent geometry, wound tape, animated six-tooth hubs and hand-drawn marks.
- `DemoDeck.kt`: three fixed design-space chassis families, native counter wheels/seven-segment display, decoded-PCM stereo meters and six integrated transport keys.
- `DemoPackaging.kt`: one shared optical case compositor, linked paper artwork, flat spines, the responsive tape library, bounded-centering shelf and title-only track rows.
- `FilledSpineTitleRaster.kt`: aligns measured jittered ink and sizes one-line names by their printable height; normal alignment margins are independent of the paper-sized clip, so width overruns reach the sheet edge rather than stopping early.
- `DemoReelMotion.kt`: continuous anticlockwise playback, six-times-faster cue winding and clockwise rewind, with radius-dependent reel speeds.
- `DemoShelfLayout.kt`: bounded centering coordinates in real layout pixels; the current tape stays visible without adding empty content at either end.
- `DemoPlayerScreen.kt`: portrait stack / landscape two-pane layout. The same deck geometry is uniformly scaled in either orientation.
- `DemoThemeChoice.kt`: direct theme selection in the current-tape customizer, rather than cycling through dozens of entries.

The existing ViewModel remains the owner of music/library/settings operations. `MixtapeApp.kt` composes the new screen and retains the native settings, track-info and customization routes.

## Artist tokens

`app/src/modern/assets/demo-themes.json` contains the approved studio palettes and their typed native bindings:

| Component | Choices |
| --- | ---: |
| Deck | 11 |
| Cassette shell / shell artwork | 26 |
| Sticker / label artwork | 24 |
| Case plastic | 13 |
| Shared sleeve back + spine | 38 |

These supplement the existing screw finishes, handwriting fonts, colors, size/messiness settings and twenty hand-drawn marks.

Color and material changes belong in this asset, not in the Canvas drawing code. A new selectable theme also needs an enum entry and binding in `MixtapeVisualProperties.kt`; a new sleeve needs three contrasting ink slots in `SleeveInk.kt`. Existing material, paper-pattern, shell-art and spine-motif vocabularies are deliberately bounded. New geometric motifs belong in their reusable renderer, not a copied player screen. Six of the 38 bound sleeve themes opt into `catalogueBox: "left"` or `"right"`: a small printed one/two-character ID on the spine only. Other papers remain unbadged; the existing per-tape `decorativeId` supplies the text. A new deck layout family needs geometry in `demoDeckGeometry`; color-only themes can reuse an existing family.

Both sleeve faces resolve **one `SleeveTheme`**. The playlist uses its paper and ink under exactly the same transmission, absorption, haze and moulded reflections as the spine. Name/symbol ink is contrast-adjusted after considering that plastic. No independently selected back/spine can drift apart.

## Interaction

- The six keys are Eject/Load, Rewind, Stop, Play/Pause, Fast Forward and double-arrow/gear. Transport symbols are solid-filled; the opposing-arrow/gear symbol retains the demo artwork.
- The Play/Pause key always shows both symbols, changing only its label and pressed state.
- Idle launcher entries show the full-page tape shelf, without a player or section headers: one column in portrait, two columns in landscape. The narrower shelf beside the landscape player stays single-column. Active playback opens the player without changing its queue/position. Pause/Stop during a player session keep the player visible; Eject returns to the full-page shelf. Explicit media-notification taps still open their player. Rotation does not count as a new launcher entry.
- Arrow/gear tap toggles the playlist and tape shelf; hold opens Settings. Long-pressing a spine in either shelf opens that tape’s editor without selecting it or interrupting playback. The editor has an App settings gear, keeping settings reachable from the idle library without a header.
- Selecting a tape returns to its playlist. Entering the player shelf centers the current tape when there is enough real content on each side. Near the beginning/end it clamps to the content boundary instead, keeping the tape visible without half-screen blank gaps. Short lists stay top-aligned. It does not recenter during ordinary playback updates or manual scrolling.
- A cassette long-press or current spine tap also opens customization. A dice button at the top generates a fresh name and every spine/case appearance field (font, handwriting seed, mark, independent inks, alignment, mark position, printed ID, case, linked paper and track ink) from the enabled choices. It previews an unsaved draft: Cancel/back leaves the saved tape untouched; Save updates only the addressed stable tape key. The cassette shell, screws, sticker, deck and music membership are not rerolled. Draft controls survive rotation. Spine text alignment (Left/Center/Right), six side/corner symbol positions, and separate name/symbol inks are saved per tape. New tapes receive stable varied combinations; an existing pre-variation default Navy/Navy pair is diversified once without changing its name, font, themes or library. Previously customized non-default inks are retained.
- Large lettering uses most of the spine’s printable height; Medium and Small retain their size multipliers. Tape names always remain on one line. They are not shrunk or wrapped to fit their width: overruns are clipped only at the physical paper edge, inside the plastic rim, as requested for the hand-lettered cassette character. Left, centre and right origins also apply to oversized names. Cassette label titles use the same one-line path, and full names remain available to accessibility services and the customizer. The handwriting font gallery uses the native spine and track components. Normal-size names retain alignment space around the symbol; oversized handwriting may cross that space rather than being trimmed early. The paper clip prevents ink reaching the plastic.
- Track rows use 32 sp base handwriting (one third larger than the previous 24 sp), a shared measured-ink baseline/row height, 1 dp vertical padding and a 26 dp minimum touch row, with one-line ellipsis and only `Track.title`. This removes font-family line-box whitespace rather than simply cramming larger glyphs into the old rows. Single- and double-tap retain the native cue/playback path; long-press retains Track info, Remove from mixtape and Delete from device.
- Rewind/Forward retain the Android app's five-second previous/next-track cue behavior, not the web demo's ten-second seek simulation. Explicit cue direction drives the reels even while audio playback is stopped during the cue: playback and fast-forward are anticlockwise, rewind clockwise, winding six times normal speed. Stop/eject, Play or selecting another tape cancels the pending cue and motion. Animation phase is continuous and respects reduced-motion settings.
- There is no player-name header, section header, side/flip display, separate seek strip or mute control on the listening screen.
- Spines use one shared 6.5:1 ratio in the library and above the tracks. Portrait player-to-spine spacing is 6 dp. The seven compact Android decks use a 560 x 422 design space so their keys remain inside the face panel; classic deck ratios are unchanged. Counter digit windows are larger with only a slim border, and geometry tests prevent cassette/transport overlap.
- System appearance controls light/dark mode. Safe drawing insets include system bars and display cutouts.

The native Android Auto data/playback integration is retained; this phone UI port does not redesign car templates.

## Verification

Evidence and build logs for the original Nothing-phone development installation are in `docs/evidence/demo-ui-port/`; the library-first/spacing/control follow-up is in `docs/evidence/demo-ui-refinement/`; the landscape library and bounded shelf follow-up is in `docs/evidence/tape-shelf-layout/`; varied lettering and tighter track rows are in `docs/evidence/spine-lettering/`; paper-edge lettering, optional catalogue marks, dice editing, larger tracks and corrected reel motion are in `docs/evidence/paper-edge-dice/`. Geometry unit tests cover all eleven decks at multiple widths, both exact winding endpoints and constant tape area, tint transmission and ink contrast. Integration contracts follow the new renderer rather than the retired footer/mini-preview layout. Existing playback, media/library and ViewModel tests remain part of the unit suite.

This is a development build. The review installation uses fresh app settings and mixtapes; it does not delete or modify the device's audio files. No release signing or store publishing is part of this change.
