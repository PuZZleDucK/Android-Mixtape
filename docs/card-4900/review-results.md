# Review requires scoped-delete rework

Reviewed local implementation 472ed69 against the fixed card-4900 plan. All 159
recorded source/build hashes still match. Both APK hashes and the implementation's
11 new APK/PNG LFS pointers verify; git lfs fsck passes. See
review-verification.txt. Existing successful build, lint, JVM and emulator logs
remain applicable to those bytes. Review did not change application code or
start an emulator. The existing car-host.png was independently inspected and
still shows unavailable media, not a successful browse result.

## Reproduced defect

API 29's RecoverableSecurityException consent grants access to retry an operation.
It does not perform the deletion. AudioRepository.kt lines 87-108 returns the
same RequiresUserAction result for that consent and API 30's createDeleteRequest,
which does perform deletion after approval. MainActivity's result callback sends
both to confirmTrackDeletedFromDevice. That handler at MixtapeViewModel.kt lines
868-877 never retries repository.deleteTrack. It filters the target out of fresh
provider results and reports "Deleted" even while the file remains on the device.
A later refresh can bring the supposedly deleted track back.

Review added ModernizationDeleteConfirmationTest.kt. Its fake provider models
API 29 accurately: initial delete requests consent, approval grants access, and
only a subsequent delete removes the media. The opaque IntentSender is allocated
without calling Android methods; this is a JVM ViewModel regression, not claimed
device verification.

Actual command using BUILDING.md's installed environment:

```sh
./gradlew --no-daemon --max-workers=2 testModernDebugUnitTest \
  --tests '*ModernizationDeleteConfirmationTest' --console=plain
```

Compilation succeeds. One test runs and fails one assertion, zero test errors:
`API 29 approval must retry provider deletion expected:<2> but was:<1>`.
See review-delete-regression.txt and review-delete-regression.xml. The first
attempt at the new test did not compile because the Android compile classpath
hides sun.misc.Unsafe; reflective lookup fixed the test setup before this real
assertion failure was recorded.

## Acceptance for the next attempt

Preserve the existing successful migration and unrelated dirty work. Distinguish
API 29 retry-required approval from API 30+ completed-delete approval. Perform the
actual API 29 provider deletion before reporting success; retain the track and
show failure if the retry fails. Do not blindly retry API 30's consent request.
Add behavioral tests for both paths, retry failure and nondestructive cancellation.
Complete the plan's pending-confirmation recreation check using synthetic media
on an owned compatible Kunlun emulator, with deployment through kunlun-sync.sh.
Rerun affected tests, build and lint, and record the final source/APK receipts.

This is already covered by the planning handoff's API 29 retry and scoped-delete
lifecycle requirements, not a new feature request. Existing source-string contracts
only assert that recovery symbols exist and generic callbacks are wired. They do
not verify the second provider call or actual deletion. The results document calls
this untested behavior a limitation, but it is an application logic defect, not
an Android platform restriction. Those checks explain why 295 passing tests did
not establish deletion correctness.

Car-host browse and noisy-headset evidence retain their stated limitations; Review
does not convert them into passes or require phone access. No push, phone use,
application edits, dependency edits or service restart. No Gradle/Kotlin process
remained after the focused test. Review owns only the new regression and this
review evidence; pre-existing tracked/untracked changes remain untouched.
