package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Card 4900: completion order and permission changes must not revive stale scans. */
@OptIn(ExperimentalCoroutinesApi::class)
class ModernizationScanLifecycleTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private class DeferredRepository : AudioRepository {
        val requests = mutableListOf<CompletableDeferred<List<Track>>>()
        override suspend fun loadTracks(): List<Track> {
            val request = CompletableDeferred<List<Track>>()
            requests += request
            return request.await()
        }
    }

    private fun track(id: Long) = Track(id, "Synthetic $id", "Test", 10000, "content://media/external/audio/media/$id")
    private fun model(repository: AudioRepository) = MixtapeViewModel(
        repository = repository,
        controller = MixtapeController(FakePlayerEngine()),
    )

    @Test fun permissionRevocationInvalidatesAnOutstandingScan() = runTest {
        val repository = DeferredRepository()
        val vm = model(repository)
        vm.onPermissionResult(true)
        assertEquals(1, repository.requests.size)
        vm.onPermissionResult(false)
        repository.requests.single().complete(listOf(track(1)))
        assertEquals(LibraryStatus.PermissionRequired, vm.uiState.value.status)
        assertEquals(emptyList<Track>(), vm.uiState.value.tracks)
    }

    @Test fun olderScanCannotOverwriteTheMostRecentRefresh() = runTest {
        val repository = DeferredRepository()
        val vm = model(repository)
        vm.onPermissionResult(true)
        vm.refresh()
        assertEquals(2, repository.requests.size)
        repository.requests[1].complete(listOf(track(2)))
        repository.requests[0].complete(listOf(track(1)))
        assertEquals(listOf(2L), vm.uiState.value.tracks.map { it.id })
    }

    @Test fun refreshWithoutPermissionDoesNotQueryTheMediaProvider() = runTest {
        val repository = DeferredRepository()
        val vm = model(repository)
        vm.onPermissionResult(false)
        vm.refresh()
        val queries = repository.requests.size
        repository.requests.forEach { it.complete(emptyList()) }
        assertEquals(0, queries)
        assertEquals(LibraryStatus.PermissionRequired, vm.uiState.value.status)
    }
}
