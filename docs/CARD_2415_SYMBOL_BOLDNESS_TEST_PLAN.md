# Card 2415 — Symbol boldness testing notes

## Expected behavior

- `HandDrawnEmbellishment` remains the single Canvas renderer for all mixtape symbols.
- All symbols use shared bold marker-weight linework: at least `2.8.dp`, replacing the old faint `1.6.dp` stroke.
- Heavier strokes retain round caps so the symbols still look hand-drawn.
- Filled-note symbols (`MusicNote`, `QuarterNote`, `EighthNote`, `BeamedEighthNotes`, `SixteenthNote`) use an explicit `filledNoteHead` helper rather than outline-only notehead circles.
- Open-note symbols (`WholeNote`, `HalfNote`) remain open noteheads; they should be made bolder with thicker outlines/interior marks, not solid-filled heads.

## Added source contract

`app/src/test/java/com/example/androidmixtape/ui/HandDrawnEmbellishmentUiContractTest.kt` now includes failing TDD checks for:

1. `handDrawnSymbolsUseBoldMarkerWeight`
2. `solidMusicNoteSymbolsUseFilledNoteheads`

These are intentionally red against the current implementation and should pass after Running updates `MixtapeApp.kt`.

## Verification command

```sh
JAVA_HOME=$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS ANDROID_HOME=$HOME/Android/Sdk ./gradlew testModernDebugUnitTest --tests com.example.androidmixtape.ui.HandDrawnEmbellishmentUiContractTest
```

Expected current failure: the renderer still declares `val strokeWidth = 1.6.dp.toPx()` and has no `filledNoteHead` helper.
