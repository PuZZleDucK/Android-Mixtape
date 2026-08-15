# Card 2210 — Remove redundant now-playing section

## Goal

On the modern Compose now-playing screen, remove the section currently rendered between the cassette transport controls and the cassette cover track list. The remaining order should be:

1. top navigation row
2. `Now Playing` heading
3. cassette tape artwork
4. cassette/Walkman transport control buttons
5. cassette cover track list

## Implementation plan

- Target file: `app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt`.
- In `NowPlaying`, remove the `MiniPlayer(state, onTogglePlayPause, onPrevious, onNext, onSeekTo)` call after `WalkmanTransportControls(...)`.
- Delete the now-unused `MiniPlayer` composable, which contains the duplicate current-track card, seek slider, and `Previous` / `Play` or `Pause` / `Next` buttons.
- Clean up now-unused imports such as `Slider` and any helper imports only needed by `MiniPlayer` (`max` if no longer used).
- Keep `CassetteTape`, `WalkmanTransportControls`, `WalkmanButton`, `CassetteCoverTrackList`, and `formatDuration` intact.
- Keep the public `MixtapeApp` callback shape stable unless the compiler/tests make a smaller cleanup obvious; this card is UI removal, not a ViewModel/playback API refactor.

## Suggested contract test

Add or update a source-level modern UI contract under `app/src/test/java/com/example/androidmixtape/ui/` to assert:

- `MiniPlayer(` is no longer present in `MixtapeApp.kt`.
- The seek slider section is gone (`contentDescription = "Seek"` and/or `Slider(` absent from the modern UI source).
- The duplicate Material button row labels from the removed card are absent: `Text("Previous")`, `Text(if (state.isPlaying) "Pause" else "Play")`, and `Text("Next")`.
- Do **not** assert that every occurrence of "Pause" is absent, because the cassette transport controls intentionally retain their pause button/description.

## Success criteria

- The modern now-playing screen has no card/section between the Walkman-style control buttons and the track list.
- Playback controls still exist in `WalkmanTransportControls` and still wire to previous/play-pause/next/stop callbacks.
- Track list rows still show durations via `formatDuration`.
- Unit tests pass, at minimum:
  - `./gradlew testModernDebugUnitTest --tests 'com.example.androidmixtape.ui.*'`
- Recommended broader verification before release:
  - `./gradlew testModernDebugUnitTest testLegacyDebugUnitTest`
  - `./gradlew assembleModernDebug assembleLegacyDebug`

## Risks / notes

- Removing `MiniPlayer` removes seek-slider access from the visible UI. The request specifically targets that section, so this is expected unless later feedback asks to relocate seek elsewhere.
- The legacy API 19 UI is separate and should not be changed for this card.
- Existing screenshot evidence names mentioning "after-controls" can help compare before/after spacing, but no new image reference is required for planning.
