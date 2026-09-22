# Building the Android 16 modern app

The canonical working checkout is `/home/puzzleduck/x/android-mixtape`.
Modern uses application ID `org.puzzleduck.mixtape`, minimum API 24 and
compile/target API 36. Java and Kotlin output remains 17. Gradle runs on the
installed Java 21 runtime.

```sh
export JAVA_HOME=/home/puzzleduck/.asdf/installs/java/temurin-21.0.10+7.0.LTS
export ANDROID_HOME=/home/puzzleduck/Android/Sdk
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon --max-workers=2 testModernDebugUnitTest lintModernDebug assembleModernDebug assembleModernDebugAndroidTest
```

Toolchain is Gradle 8.13, AGP 8.13.2 and Kotlin/Compose plugin 2.0.21.
Libraries remain Compose BOM 2024.12.01, Activity 1.9.3, Lifecycle 2.8.7,
coroutines 1.9.0, Media3 1.5.1, Car App 1.8.0-beta01 and media compat 1.6.0.
No dependency upgrade was needed for the verified migration.

The current tested APK is `artifacts/card-4900/rework-modern-debug.apk`.
The API 29 delete-consent repair and API 29/36 recreation checks are documented
in `docs/card-4900/rework-results.md`. SHA256 receipts, real
build/test logs and emulator evidence are in `docs/card-4900/`. Use
`kunlun-sync.sh --apk artifacts/card-4900/rework-modern-debug.apk --install-app`
with an explicit owned emulator serial. The script now honors that serial's
port when launching and accepts `KUNLUN_DISPLAY_ID` for Automotive captures.
See AGENTS.md for deployment rules. Do not use a phone for this workflow.

## Working-tree dependency

This checkout had substantial uncommitted work before card 4900. The APK includes
that work. The migration commit deliberately does not absorb it, retire its
legacy files, or commit its untracked UI/membership/audio implementations.
A clean checkout of the migration commit alone is therefore not the complete
source of the tested APK. Preserve the canonical working tree.

`docs/card-4900/owned-dirty-tree.patch` records migration deltas against the
starting dirty files. Three existing untracked device tests needed fixture and
selector repairs. Their owned deltas are committed in
`untracked-test-adjustments.patch`, while their files remain untracked rather
than silently adopting another task's source. Do not apply either patch again
to the current working tree. Full source byte receipts accompany the results.

No temporary target-28 baseline was present in this canonical repository.
The migration changes its actual target from 35 to 36.
