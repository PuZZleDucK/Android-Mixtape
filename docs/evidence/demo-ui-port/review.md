# Native demo UI - Nothing-phone development review

Date: 2026-09-27 (Australia/Melbourne)

## Scope and implementation

The canonical Android project on Artigas now uses native Compose/Canvas components adapted from the approved cassette demo. This is not a WebView wrapper. The existing MediaStore library, Media3 audio service, settings and Android Auto data routes remain native. No new dependencies were required.

The bundled artist catalog supplies 11 decks, 26 cassette shells, 24 stickers, 13 case materials and 38 linked sleeve-back/spine choices. New component and artist-token documentation is in `../../native-demo-ui.md`.

The user explicitly requested a development installation without preserving old tapes or settings. The modern app's data was cleared once, its audio permission was restored, and its real audio library was rescanned into 13 mixtapes. Audio files were not deleted or modified. A precautionary private backup was retained outside the repository. The separately installed historical application ID was not modified. No release was signed or published.

## Build and automated verification

Executed on Artigas with Java 21 and the installed Android SDK:

```
./gradlew --no-daemon --max-workers=2 assembleModernDebug testModernDebugUnitTest lintModernDebug --console=plain
```

Final log: `verification-3.log`.

- Build successful (10m 45s).
- 330 unit tests; 0 failures, 0 errors, 0 skipped (summed from the JUnit XML).
- Lint: 0 errors/fatal findings, 44 warnings, 2 hints. Warnings remain; this is not a zero-warning build. New-port advisory warnings include modifier parameter order and the Color parsing KTX suggestion.
- Geometry checks cover all 11 deck themes at widths 280 through 700, constant family aspect ratios, contained tape/counter bounds, area-conserving full tape winding endpoints, case transmission and readable ink.
- Updated source contracts follow the new native renderer/list layout; existing playback, library and ViewModel tests remain enabled.
- No instrumentation suite was run on the personal phone. API 24 emulator behavior and Android Auto presentation were not newly device-tested for this phone UI port.
- No Gradle/Kotlin build or idle daemon remained after final verification.

## Installed artifact

- Package: `org.puzzleduck.mixtape` (modern debug).
- Remote distributable: `dist/android-mixtape-native-demo-debug.apk`.
- Kunlun review copy: `/home/puzzleduck/x/remote-mixtape-testing/android-mixtape-native-demo-review.apk`.
- APK SHA256: `8a10bda7dacc39579476bd025b7fef751ba5eb9a5fc6235e8b67a4e7cac77d8e`.
- Installed `base.apk` SHA256 was independently read on the phone and matched the distributable.
- Existing debug signing identity matched; installation used `adb install -r`, not uninstall/reinstall.

## Physical-device checks

Target was explicitly pinned throughout: Nothing A001T / Galaxian, serial `00252359V002047`, Android 16 / API 36, 1080 x 2392, density 420. No other phone was used.

Verified manually on the phone:

- Cold launch and real music scan; native audio plays.
- Counter advances; reels rotate/wind; real decoded-PCM stereo meters respond independently. Pause stops transport animation and keeps both play/pause symbols visible.
- Clear Study can expose the complete stickerless reel mechanism.
- Safety Yellow has compact narrow vertical levels to the right of the cassette with its small counter below. No clipped counter or duplicate deck drive squares show through the tape.
- Graphite Tall retains its tall design-space aspect and enlarged cassette; landscape places the playlist alongside it without stretching the chassis.
- Light and dark system appearance, safe system-bar/cutout insets and legible status icons.
- Arrow/gear switches playlist and shelf; selecting the current shelf cassette returns to the playlist. Long-press opens settings.
- Current tape is centered on shelf entry, with no separate current-tape copy/header. In the final landscape XML, the list center is Y=571.5 and the first/current spine center is Y=572.0 (half-pixel rounding).
- Safety Yellow player aspect measured from final XML: portrait 1016/726 = 1.3994; landscape 1106/790 = 1.4000, differing only by pixel rounding.
- Direct current-tape customization: Smoke C90 cassette, Magnetic Studio sticker, Archive Crystal case and Studio Index shared sleeve selected and saved. Back and spine remain linked and use the same case-plastic treatment.
- Name-only track rows scroll normally. A track long-press exposes Track info, Remove from mixtape and Delete from device; the menu was dismissed without invoking destructive actions.
- No AndroidRuntime fatal entry was present for the final running process.

Theme galleries use the same renderer as playback. Representative themes were visually exercised; not every possible combination of the 112 core theme choices was manually inspected.

## Handoff and evidence

The app is left foregrounded, paused, with Graphite Tall and the track list visible. Original phone settings were restored: night mode yes, automatic rotation on, user rotation 0, media volume 11/16. The old settings/tapes were deliberately not migrated.

Clean screenshots and UI hierarchies here show:

- `final-portrait-light.*`: Safety Yellow, clear/stickerless reel review, light system mode.
- `final-landscape-dark.*`: Safety Yellow with the saved customized cassette and linked sleeve/case.
- `final-centered-shelf.*`: current cassette centered, with no duplicate fixed current spine.
- `final-graphite-portrait.*` and `final-graphite-landscape.*`: Graphite Tall and title-only playlist in both orientations.
- `final-track-menu.xml`: native non-destructive inspection of long-press actions.

Earlier build logs are retained for traceability; `verification-3.log` is the successful final result. Screenshots containing unrelated picture-in-picture content were excluded from this repository evidence. No source was copied to Kunlun, no commits were pushed, and publishing storage was untouched.
