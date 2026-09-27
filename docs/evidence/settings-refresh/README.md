# Settings refresh

The modern settings UI now has three categories: Mixtapes, Appearance, and Library. The selected category and its scroll position survive visits to detail pages. Both on-screen and Android Back navigation return through Settings to the mixtape list.

## Layout and controls

- Tape length and artist grouping retain every existing choice. The UI explains that changing either rebuilds the tapes.
- Appearance keeps the deck, cassette, screw, sticker, case, sleeve, font, symbol, and handwriting-messiness controls. Current deck and enabled-option counts appear on the main page.
- Detail pages share a pinned header, warm paper/ink light palette, dark palette, and adaptive preview grid. Component cards are single accessible selection controls, retaining the last-enabled-option guard. Font cards still preview both a spine and track list.
- Mixtape names retain Edit and Randomize, with a local search field and a blank-name guard.
- Filename exclusions retain adding/removing patterns, examples, and blank-input protection. Help keeps the existing Android Auto instructions.
- Reset All remains available in Library, now behind a cancellable confirmation. No audio files are deleted by reset.
- Existing tape/spine-skin routes and callbacks are retained; their detail pages use the same layout components.
- `SettingsComponents.kt` contains shared presentation components. No settings stores or playback logic changed in this refresh.

## Verification

- 243 modern and 237 legacy unit tests passed. Both APK variants assembled: `build-and-unit-tests.txt`.
- Modern APK installed and launched through `kunlun-sync.sh` on the Kunlun API 24 emulator: `deploy.txt`.
- All 19 modern instrumentation tests passed: `ui-tests.txt`. Nine settings tests cover all 11 currently linked detail destinations, category restoration, hardware Back, every tape-length/grouping/messiness option, component/font/symbol callbacks, last-enabled guards, name search/edit/randomize, exclusions, reset cancellation/confirmation, empty-library navigation, portrait/landscape, 1.4x text scale, and dark mode.
- Screenshots use synthetic settings state at 360x620dp portrait, 900x360/440dp landscape, and a 320x560dp large-text check. `*-cropped.png` images omit unused emulator/test-window space; the full frames are retained alongside them.
- Legacy lint passed. Modern lint still reports the same 7 existing Android Auto errors and 31 warnings: `lint.txt`. No new settings errors or warnings.

The APK in `dist/` includes this refresh and both earlier fixes. No user device was accessed, no commits or pushes were made, and the task-owned emulator was stopped after verification. Gradle used single-use daemons.
