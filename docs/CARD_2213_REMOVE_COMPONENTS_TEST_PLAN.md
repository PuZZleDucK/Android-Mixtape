# Card 2213 testing plan: remove library page and header titles

## Expected behavior

- The modern Compose app must not render the `TopAppBar` title or the top-level `state.message` banner above the screen navigation/content area.
- Permission, empty-library, loading, and error guidance must remain inside their respective states.
- The modern app must not expose a reachable standalone Library page.
- No visible `Library` navigation button should remain on mixtape, settings, or now-playing screens.
- The ViewModel contract should no longer include `MixtapeScreen.Library`, `backToLibrary()`, or transitions to `MixtapeScreen.Library`.
- Mixtape-first flow must remain intact: permission grant/refresh opens `MixtapeScreen.MixTapes`; selecting a mixtape queues only that mixtape; settings/reset/grouping continue to work.

## Failing spec added for Running

Added `app/src/test/java/com/example/androidmixtape/ui/RemoveLibraryComponentsContractTest.kt` with source-contract coverage for:

1. Header removals: no `TopAppBar`, no `topBar =`, no top-level `text = state.message` banner.
2. Library UI removal: no `MixtapeScreen.Library` branch, no `TrackLibrary` composable, no visible `Text("Library")` navigation button.
3. ViewModel contract removal: no `Library,` enum entry, no `backToLibrary()`, no transitions to `MixtapeScreen.Library`.

## Verification commands for Running

```sh
cd /home/puzzleduck/x/android-mixtape
export PATH="$HOME/.asdf/shims:$HOME/.local/bin:$PATH"
export JAVA_HOME="$(asdf where java)"
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew test
./gradlew assembleModernDebug assembleLegacyDebug
```

The new contract test is expected to fail before implementation. Running should make it pass while preserving the existing mixtape playback, grouping, settings, and legacy compile checks.
