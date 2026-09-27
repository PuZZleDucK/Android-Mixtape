# Track removal without regrouping or renaming

## Cause and fix

- “Remove from mixtape” swapped the selected track with a random track from another tape. It now removes only that tape's membership entry, without deleting the audio file, swapping songs, or filling the gap.
- Device deletion filtered the library before reconstructing groups, moving later songs between tapes. It now filters saved memberships without repacking them.
- Name/design keys were recomputed from the group's starting position and remaining track IDs. Each tape now retains its original key. Initial keys still match existing saved names and designs.
- Membership is saved separately in SharedPreferences. Refresh, reordered library results, and player recreation retain edits. Empty tapes retain their identities. Newly discovered songs get new groups rather than filling edited tapes. Explicit Reset All or grouping-setting changes can still rebuild the collection.
- The car catalog reads the same membership and exclusion settings instead of independently regrouping device audio. This is a catalog-data change, not a car UI change.

## Verification

- All 4 initial behaviour regressions failed against the old implementation: `baseline-tests.txt`.
- 243 modern and 237 legacy unit tests passed: `build-and-tests.txt`. These include 14 removal regressions covering names, designs, other tape memberships, playback continuity, single/empty tapes, refresh/recreation, new files, legacy name keys, shared tracks, exclusion toggles, failed deletion, and Android confirmation after switching tapes.
- A car catalog unit test verifies the phone's edited membership and names are retained.
- Both debug APKs assembled. Modern APK installed and launched through `kunlun-sync.sh` on the Kunlun API 24 emulator.
- All 10 modern instrumentation tests passed: `ui-tests.txt`. The new test operates the actual long-press “Remove from mixtape” menu and recreates the ViewModel with fresh SharedPreferences-backed store instances. Test audio and preferences are isolated from the installed app's user data.
- `after-removal.png` is the full emulator screenshot; `fixed-player.png` crops it to the test viewport. Test song 2 was removed from Road songs; Night bus and Sunday mix retain their songs, names and designs.
- Legacy lint passed. Modern lint has the same 7 pre-existing Android Auto errors and 31 warnings as before this change; no new membership/removal diagnostics: `lint.txt`.

No user device was accessed. No destructive device operation was performed during testing; repository deletion and Android confirmation were simulated in unit tests. The emulator was started for this task and stopped afterwards. No commits or pushes were made.
