# Android Mixtape Agent Notes

- The canonical project source is `/home/puzzleduck/x/android-mixtape` on Artigas.
- Push commits to `origin` only when the user explicitly requests a push in the current conversation. Never push automatically as part of completing or committing work.
- Use `./kunlun-sync.sh` for all routine testing on the Kunlun Android emulator. Do not manually copy or synchronize project source to Kunlun. The script sends only the modern debug APK to `kunlun.local:~/x/remote-mixtape-testing/`.
- Main script options:
  - `--launch-emulator` plays an audible three-note warning, then launches the visible API 24 emulator.
  - `--install-app` installs the synchronized APK on `emulator-5554`.
  - `--start-app` launches Android Mixtape on the emulator.
  - `--screenshot PATH` captures the emulator to the specified local PNG.
  - `--all PATH` performs all four actions in order.
  - `--apk PATH` overrides the APK selected from standard local build outputs.
- Always use the Android emulator on `kunlun.local` for emulator deployment, connected testing, and emulator screenshots. Never start or use an Android emulator on the local Artigas host for this project.
- The Nokia is a user device. Do not inspect, install to, launch, test, capture from, or otherwise access it unless the user directly requests Nokia/device work in the current conversation.
- Do not treat emulator storage exhaustion (for example `INSTALL_FAILED_INSUFFICIENT_STORAGE`) as a product failure, test failure, or reason to fail/move cards backward.
- When storage blocks install or connected/manual verification, document the environment facts (device/emulator, free `/data`, APK size, command output), keep any source/unit/build evidence, and either use another compatible target or move forward with the storage limitation clearly noted.
- For any Kanban-card work on this project, build and deploy the updated app to the Kunlun Android emulator before reporting completion, then capture screenshots of the updated UI/evidence from the emulator. The Kunlun emulator is usually `emulator-5554`; because it is `x86_64,x86`, build an emulator-compatible APK when the default APK is `arm64-v8a` only.
- Kunlun also has the visible Android Automotive emulator `android-auto-mixtape`, normally running as `emulator-5556` on API 35 with the `x86_64` ABI. For Android Auto/car UI changes, build and install an `x86_64`-compatible modern APK on this emulator, review the affected UI there, and capture a screenshot that clearly highlights the update in the car environment before reporting completion. Where behavior specifically depends on Android Auto projection rather than Android Automotive OS, also verify with the Desktop Head Unit or a connected head unit when available and document any host limitation.
- Keep `README.md` user-facing. Put agent workflows, host details, test procedures, device serials, and deployment commands in this file instead.
- For bundled mixtape-name revisions, check the whole asset for repeated templates, broken grammar, and sentences cut down to meet the four-word limit. A passing length/uniqueness test and two emulator samples do not establish that the full list is entertaining. Read every title in batches before handoff.

## Build and test

The project uses Java 21 and the Android SDK. Modern is the only flavor, with application ID `org.puzzleduck.mixtape` and minimum API 24. Kotlin's internal namespace remains `com.example.androidmixtape`; explicit launch components use `org.puzzleduck.mixtape/com.example.androidmixtape.MainActivity`. Historical evidence and card plans may mention the retired legacy flavor or old application ID.

The main local verification commands are:

```bash
./gradlew testModernDebugUnitTest
./gradlew assembleModernDebug
./gradlew lintModernDebug
```

Connected tests must run on the appropriate Kunlun emulator after deploying through `kunlun-sync.sh`:

```bash
./gradlew connectedModernDebugAndroidTest
```

Reuse a recorded successful test run when its covered source and test bytes are unchanged. Review notes, screenshots and LFS receipts do not invalidate that result or require another APK build/deployment. If a unit task genuinely needs fresh execution, use `./gradlew testModernDebugUnitTest --rerun --console=plain`, which reruns that task while retaining up-to-date compilation. Avoid `--rerun-tasks` for this purpose: card 4835's Review forced the whole dependency chain and hit a 200-second timeout, while the task-only rerun completed in 65 seconds with all 267 tests passing. That interrupted build is not a product failure or a reason to move the card backward.

After card-owned Artigas builds/tests finish, check that no Gradle client or other Android build is still active. If the task's Gradle/Kotlin daemons are then only idle keepalive processes, run `./gradlew --stop` and verify those daemons exited; never stop them while another build is active.

To refresh distributable APKs after a successful build:

```bash
mkdir -p dist
cp app/build/outputs/apk/modern/debug/app-modern-debug.apk dist/android-mixtape-v0.1.0-modern-debug.apk
sha256sum dist/android-mixtape-v0.1.0-modern-debug.apk > dist/android-mixtape-v0.1.0-modern-debug.apk.sha256
```

For manual emulator smoke testing, seed local audio on the emulator, grant the relevant media permission, and verify scanning, playback, rotation continuity, seeking, transport controls, permission handling, and empty-library behavior. Use `kunlun-sync.sh` rather than treating any copied Kunlun directory as project source.

## Projected Android Auto on the Nokia

- This section applies only when the user directly requests Nokia or projected Android Auto testing in the current conversation. Never use the Nokia as a general test target.
- The Android Automotive AVD is not the same as phone-projected Android Auto. When explicitly requested, use the Nokia G60 5G attached to Kunlun over ADB. Its usual serial is `AQ7505H032N92400458`; override it with `ANDROID_AUTO_SERIAL` if it changes.
- Kunlun has DHU 2.1 at `~/Android/Sdk/extras/google/auto/desktop-head-unit` and the required `libc++`/`libc++abi` runtime extracted under `~/.local/lib/android-auto-dhu`. The checked-in launcher is `scripts/run-android-auto-nokia.sh`.
- Android Auto developer mode is enabled on the Nokia. Before launching DHU, open Android Auto settings on the phone, use the three-dot menu, and select **Start head unit server**. Confirm the **Android Auto Developer — Head unit server running** notification.
- Run the launcher on Kunlun's visible desktop. From an Artigas checkout, copy and start it with:

  ```bash
  scp scripts/run-android-auto-nokia.sh scripts/android-auto-1280x600.ini kunlun.local:/tmp/
  ssh -t kunlun.local 'chmod +x /tmp/run-android-auto-nokia.sh && DISPLAY=:0 /tmp/run-android-auto-nokia.sh'
  ```

- The launcher verifies the Nokia's ADB connection, forwards `tcp:5277`, loads the local runtime libraries, and starts DHU with `scripts/android-auto-1280x600.ini` (1280x720 transport cropped to an effective 1280x600 at 160 DPI). A direct Kunlun checkout can simply run `./scripts/run-android-auto-nokia.sh`. Override the preset with `ANDROID_AUTO_DHU_CONFIG=/path/to/config.ini` when needed.
- If the local runtime is missing and sudo is unavailable, install it without root on Kunlun:

  ```bash
  mkdir -p ~/.cache/android-auto-dhu-debs ~/.local/lib/android-auto-dhu
  cd ~/.cache/android-auto-dhu-debs
  apt-get download libc++1 libc++abi1
  for deb in ./*.deb; do dpkg-deb -x "$deb" ~/.local/lib/android-auto-dhu; done
  ```
