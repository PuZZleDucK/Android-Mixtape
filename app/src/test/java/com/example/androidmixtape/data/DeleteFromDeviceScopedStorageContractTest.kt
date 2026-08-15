package com.example.androidmixtape.data

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteFromDeviceScopedStorageContractTest {
    @Test
    fun deleteResultExposesScopedStorageUserActionResult() {
        val source = audioRepositorySource()

        assertTrue(
            "DeleteTrackResult should expose a user-action/pending result instead of collapsing Android scoped-storage confirmation into Failure.",
            source.contains("RequiresUserAction") || source.contains("NeedsConfirmation") || source.contains("PendingUserAction"),
        )
        assertTrue(
            "The user-action delete result should carry an IntentSender so the Activity can launch Android's delete confirmation UI.",
            source.contains("IntentSender"),
        )
    }

    @Test
    fun mediaStoreDeleteUsesApiGatedScopedStorageRequests() {
        val source = audioRepositorySource()
        val deleteBody = functionBody(source, "override suspend fun deleteTrack(track: Track): DeleteTrackResult")

        assertTrue(
            "Modern MediaStore deletion should branch on Build.VERSION.SDK_INT for scoped-storage behavior.",
            deleteBody.contains("Build.VERSION.SDK_INT"),
        )
        assertTrue(
            "API 30+ deletes should use MediaStore.createDeleteRequest(...) and return its intentSender for Activity launch.",
            deleteBody.contains("MediaStore.createDeleteRequest") && deleteBody.contains("intentSender"),
        )
        assertTrue(
            "API 29 deletes should handle RecoverableSecurityException and expose its userAction intentSender.",
            source.contains("RecoverableSecurityException") && source.contains("userAction"),
        )
        assertTrue(
            "Pre-scoped-storage devices should keep the direct ContentResolver.delete fallback.",
            deleteBody.contains("resolver.delete"),
        )
    }

    @Test
    fun api29RecoverableSecurityExceptionAccessIsHiddenBehindSdkGuard() {
        val source = audioRepositorySource()

        assertFalse(
            "Shared minSdk source must not directly catch RecoverableSecurityException; catch SecurityException first, then inspect the API-29 subtype behind an SDK guard so lintModernDebug/lintLegacyDebug do not report NewApi errors.",
            Regex("""catch\s*\([^)]*:\s*RecoverableSecurityException\s*\)""").containsMatchIn(source),
        )
        assertTrue(
            "RecoverableSecurityException.userAction/actionIntent access should live behind Build.VERSION_CODES.Q or @RequiresApi(Build.VERSION_CODES.Q).",
            source.contains("Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q") ||
                source.contains("@RequiresApi(Build.VERSION_CODES.Q") ||
                source.contains("@RequiresApi(29"),
        )
    }

    @Test
    fun modernActivityLaunchesScopedStorageDeleteRequestAndFinalizesResult() {
        val source = mainActivitySource()

        assertTrue(
            "Modern MainActivity should register an ActivityResultContracts.StartIntentSenderForResult launcher for MediaStore delete confirmation.",
            source.contains("StartIntentSenderForResult"),
        )
        assertTrue(
            "Successful Android delete confirmation should route RESULT_OK back to the ViewModel finalization path.",
            source.contains("RESULT_OK") && (source.contains("confirmTrackDeletedFromDevice") || source.contains("finalizeTrackDeletedFromDevice") || source.contains("refreshAfterDeviceDelete")),
        )
        assertTrue(
            "The delete menu callback should launch pending user action instead of only calling viewModel.deleteTrackFromDevice(track.id).",
            source.contains("IntentSenderRequest") || source.contains("launchDelete") || source.contains("pendingDelete"),
        )
    }

    @Test
    fun viewModelDefersMutationUntilScopedStorageConfirmation() {
        val source = viewModelSource()
        val deleteBody = functionBody(source, "fun deleteTrackFromDevice(trackId: Long)")

        assertTrue(
            "ViewModel should store pending delete context when the repository returns a scoped-storage user-action result.",
            source.contains("pendingDelete") || source.contains("PendingDelete"),
        )
        assertTrue(
            "deleteTrackFromDevice should branch on the user-action result without removing the track from in-memory lists yet.",
            (deleteBody.contains("RequiresUserAction") || deleteBody.contains("NeedsConfirmation") || deleteBody.contains("PendingUserAction")) && deleteBody.contains("return"),
        )
        assertTrue(
            "ViewModel should expose a finalization method that rebuilds tracks/groups/queue after Android confirms device deletion.",
            source.contains("confirmTrackDeletedFromDevice") || source.contains("finalizeTrackDeletedFromDevice") || source.contains("refreshAfterDeviceDelete"),
        )
    }

    private fun functionBody(source: String, startMarker: String): String {
        val start = source.indexOf(startMarker)
        assertTrue("Expected to find $startMarker in source", start >= 0)
        val nextFun = source.indexOf("\n    fun ", start + startMarker.length)
        val nextOverride = source.indexOf("\n    override fun ", start + startMarker.length)
        val nextPrivateFun = source.indexOf("\nprivate fun", start + startMarker.length)
        val candidates = listOf(nextFun, nextOverride, nextPrivateFun).filter { it > start }
        val end = candidates.minOrNull() ?: source.length
        return source.substring(start, end)
    }

    private fun audioRepositorySource(): String = readFirstExisting("app/src/main/java/com/example/androidmixtape/data/AudioRepository.kt")

    private fun mainActivitySource(): String = readFirstExisting("app/src/modern/java/com/example/androidmixtape/MainActivity.kt")

    private fun viewModelSource(): String = readFirstExisting("app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt")

    private fun readFirstExisting(vararg paths: String): String {
        val candidates = paths.flatMap { path ->
            val withoutAppPrefix = path.removePrefix("app/")
            listOf(File(path), File(withoutAppPrefix), File("../$path"), File("../$withoutAppPrefix"))
        }
        val file = candidates.firstOrNull { it.exists() }
        assertTrue("Expected one of ${candidates.map { it.path }} from ${System.getProperty("user.dir")}", file != null)
        return file!!.readText()
    }
}
