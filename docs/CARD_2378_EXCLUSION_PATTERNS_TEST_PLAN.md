# Card 2378: exclusion pattern testing contract

## TDD scope

These failing tests define the next Running implementation contract for filename exclusion settings. They intentionally fail before implementation.

## Added tests

- `app/src/test/java/com/example/androidmixtape/viewmodel/FilenameExclusionMatcherTest.kt`
  - `*.tmp.mp3` matches `Song.TMP.mp3` case-insensitively.
  - `?` matches exactly one filename character.
  - Patterns without `*` or `?` behave as contains matches.
  - Regex metacharacters like `(`, `)`, `[`, `]` are treated literally.
  - `displayName` is used before title; title is only a fallback when display name is missing.
- `app/src/test/java/com/example/androidmixtape/viewmodel/MixtapeExclusionSettingsContractTest.kt`
  - Shared settings model, matcher, in-memory store, and SharedPreferences store exist.
  - ViewModel exposes exclusion settings state, navigation, add/remove mutations, and store injection.
  - ViewModel keeps a raw/unfiltered library so removing patterns can restore matching tracks.
- `app/src/test/java/com/example/androidmixtape/ui/MixtapeExclusionSettingsScreenContractTest.kt`
  - Main Settings links to an Exclusion settings page.
  - Dedicated page has a text field, Add action, current pattern list, Remove controls, and examples.
  - Modern `MainActivity` wires persistent store plus navigation/add/remove callbacks.
- `app/src/test/java/com/example/androidmixtape/viewmodel/MixtapeViewModelTest.kt`
  - Adding a filename exclusion removes matching tracks from `tracks`, `mixTapeGroups`, and open `queueTracks`.
  - Removing the pattern makes raw library tracks eligible again.
  - Blank and trim-equivalent duplicate patterns are ignored.

## Verification evidence

Command run:

```sh
cd /home/puzzleduck/x/android-mixtape
export JAVA_HOME="$HOME/.asdf/installs/java/temurin-21.0.10+7.0.LTS"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
./gradlew testModernDebugUnitTest \
  --tests '*Exclusion*' \
  --tests '*MixtapeViewModelTest.addingFilenameExclusionPatternFiltersLibraryGroupsAndOpenQueue' \
  --tests '*MixtapeViewModelTest.removingFilenameExclusionPatternMakesRawLibraryTracksEligibleAgain' \
  --tests '*MixtapeViewModelTest.blankAndDuplicateFilenameExclusionPatternsAreIgnored'
```

Result: expected red state, 14 failing tests. Full output saved at:

- `docs/evidence/card2378-testing-failing-specs.txt`

## Running success criteria

The implementation should turn the targeted tests green without weakening the contracts, then run:

```sh
./gradlew testModernDebugUnitTest --tests '*Exclusion*' --tests '*MixtapeViewModelTest'
./gradlew testModernDebugUnitTest testLegacyDebugUnitTest
```
