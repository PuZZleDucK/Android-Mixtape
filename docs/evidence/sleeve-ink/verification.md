# Per-mixtape sleeve ink

Each of the nine sleeve themes now has its original ink plus two named alternatives. Newly created mixtapes choose a saved palette slot using the existing jitter index modulo three, without consuming another random number or changing the jitter. Existing tapes without a saved ink slot retain their original colour. Unknown stored slots also fall back to the original.

The mixtape editor cycles the three options under Ink color, with a swatch and handwriting preview. Save persists the selection; Cancel leaves it untouched. Changing sleeve themes keeps the chosen slot and uses that theme's corresponding colour. The selection applies to sleeve/spine titles, ordinary tracks and neighbouring-track previews. Selected-track accent colours are unchanged.

Verification:
- 290 unit tests passed, including all 27 colours meeting 4.5:1 contrast against their sleeve base backgrounds, unchanged original inks, creation, persistence and editing without playback/jitter changes.
- Two connected tests passed on Kunlun API 24: preference round trips and upgrade fallback; real editor cycling/save/cancel plus pixel checks showing normal ink changes while selected-track pixels stay identical.
- Modern app and instrumentation APKs built successfully and were installed through `kunlun-sync.sh` before testing. Screenshots show the running implementation.
- Initial UI captures caught the soft keyboard/window transition. Closing the keyboard and waiting for settled frames fixed the test timing; final direct and connected runs both passed.
- A redundant reinstall after connected testing stalled. Its orphaned ADB install process was terminated, along with the owned emulator and SSH forward. The idle Gradle daemon was also stopped. The earlier deployment and tests completed successfully.
- No phone was accessed. No commit or push was made.
