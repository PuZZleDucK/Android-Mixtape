# Delete-consent repair

Card 4900 review rework, canonical checkout. Baseline, migration 472ed69 and
review 1cc90ee remain intact. No push or phone use.

## Implementation

AudioRepository now marks RecoverableSecurityException consent as requiring a
provider retry. MediaStore.createDeleteRequest consent retains its API 30+
meaning: Android performs the deletion. PendingDelete retains this distinction
across Activity recreation in the existing ViewModel.

After API 29 approval, the ViewModel retries repository deletion once. Only
Success removes the track from app state. Failure or another consent response
keeps the track visible and reports failure without reopening a prompt. Cancel
never retries. Consuming the pending operation before retry also prevents a
duplicate Activity result from deleting twice.

## Executed checks

- Real debug APK build and all 301 JVM tests pass, zero failures/errors.
  rework-build.txt and rework-delete-tests.xml record the run. Six focused
  cases cover API 29 success, failure, repeated consent and cancellation,
  API 30+ approval/cancellation, plus duplicate-result handling.
- Final lint passes with zero errors, 42 warnings and two hints. No suppression
  was added. See rework-lint.txt and rework-lint-report.txt.
- Owned Kunlun API 29 and API 36 AVDs each pass the new real-provider device
  test. It uses only a shell-seeded, generated 30-second sine wave named
  Card4900Delete.wav. The test opens Android consent, recreates the Activity
  behind the prompt, cancels, verifies the MediaStore row still exists, then
  repeats recreation and approves. It verifies the actual provider row is gone
  and the app no longer lists it. Both platform paths pass, not just mocks.
- API 29 also passes the existing lifecycle and full CounterTransportServiceTest
  checks on a clean app fixture. This covers service playback, seek, rotation,
  stop/eject and library reselection. rework-regression29-clean.txt records both
  passes. A fresh script-captured playing.png and media-session dump show actual
  playback of the synthetic file.

The test fixture initially used a nonexistent Cancel label on API 36, whose
buttons say Deny and Allow. It now uses Android's stable button IDs.
ActivityScenario.recreate forces RESUMED and cannot operate behind a system
consent Activity. The test calls the real Activity.recreate, observes the new
Activity through lifecycle callbacks and verifies the same ViewModel survives.
No production workaround or prompt bypass was introduced.

An initial additional playback run reused membership state from deleting and
re-seeding the same media fixture. Library reselection failed in that dirty
fixture. Clearing only the owned emulator app's test data and rerunning the
same two tests passed without source changes. Both logs are retained.

## Evidence and ownership

Current APKs are artifacts/card-4900/rework-modern-debug.apk and
rework-modern-debug-androidTest.apk. Hashes are in rework-apk-sha256.txt.
All new PNGs and APKs use Git LFS. Prior artifacts remain as historical evidence.
rework-tested-source-sha256.txt records the full canonical build inputs.

All application deployments used kunlun-sync.sh. Device instrumentation ran
through explicit emulator-5580 commands on Kunlun, sequentially. The new device
test captures the actual pending dialogs; final UI and playback captures also
went through kunlun-sync.sh. Screenshots in rework-api29 and rework-api36 were
inspected. Both AVDs and their remote runtime directory were removed. No owned
Gradle/Kotlin daemon remains. The installed API 29 SDK image is a reusable SDK
package, not a running emulator or temporary extracted tree.

Only card-owned changes are staged. MixtapeViewModel's pre-existing edits remain
unstaged; rework-owned-vm.patch records the exact additional changes. Other dirty
and untracked canonical work is preserved. No other Mixtape worker or build was
active. As before, the APK includes the canonical dirty sources and is not
reproducible from this card's commits alone.

Compile/target 36, Java/Kotlin 17, AGP 8.13.2, Gradle 8.13 and all library versions
in BUILDING.md are unchanged. The earlier physical-headset, predictive-gesture
and car-host limitations still apply. Pending deletion across process death is
not claimed; this test covers real Activity recreation with a retained ViewModel.
The API 29 retry defect and pending-consent recreation gap from Review are fixed
and verified.
