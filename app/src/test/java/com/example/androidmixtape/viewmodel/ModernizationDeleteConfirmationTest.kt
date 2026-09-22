package com.example.androidmixtape.viewmodel

import android.content.IntentSender
import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.DeleteTrackResult
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** API 29 consent grants write access; unlike API 30 it does not perform deletion. */
@OptIn(ExperimentalCoroutinesApi::class)
class ModernizationDeleteConfirmationTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun approvedApi29ConsentMustActuallyDeleteBeforeReportingSuccess() = runTest {
        // Opaque callback token only. No Android method is invoked in this JVM test.
        val allocatorClass = Class.forName("sun.misc.Unsafe")
        val allocator = allocatorClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
        val token = allocatorClass.getMethod("allocateInstance", Class::class.java)
            .invoke(allocator, IntentSender::class.java) as IntentSender
        val track = Track(1, "Synthetic", "Test", 10000, "content://media/external/audio/media/1")
        var existsOnDevice = true
        var consentGranted = false
        var deleteCalls = 0
        val repository = object : AudioRepository {
            override suspend fun loadTracks(): List<Track> = if (existsOnDevice) listOf(track) else emptyList()
            override suspend fun deleteTrack(track: Track): DeleteTrackResult {
                deleteCalls++
                if (!consentGranted) return DeleteTrackResult.RequiresUserAction(token)
                existsOnDevice = false
                return DeleteTrackResult.Success
            }
        }
        val vm = MixtapeViewModel(repository, MixtapeController(FakePlayerEngine()))
        vm.onPermissionResult(true)
        vm.deleteTrackFromDevice(track.id)
        assertEquals(1, deleteCalls)
        consentGranted = true
        vm.confirmTrackDeletedFromDevice()
        assertEquals("API 29 approval must retry provider deletion", 2, deleteCalls)
        assertEquals(false, existsOnDevice)
        assertEquals(emptyList<Track>(), vm.uiState.value.tracks)
    }
}
