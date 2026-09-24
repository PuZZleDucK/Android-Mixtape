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

@OptIn(ExperimentalCoroutinesApi::class)
class MediaNotificationNavigationTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val tracks = listOf(Track(1, "Song", "Artist", 100_000L, "content://track/1"))

    @Test fun readyAndPausedQueueOpenWithoutPlaybackCommands() = runTest {
        val engine = FakePlayerEngine()
        val vm = makeViewModel(engine)
        vm.onPermissionResult(true)
        engine.simulateExternalPlaybackSnapshot(tracks, 0, false, 24_000, 100_000)
        vm.showMixTapes()
        val before = engine.playCount to engine.lastSeek
        vm.openNowPlayingFromNotification()
        assertEquals(MixtapeScreen.NowPlaying, vm.uiState.value.screen)
        assertEquals(false, vm.uiState.value.isPlaying)
        assertEquals(24_000L, vm.uiState.value.positionMs)
        assertEquals(before, engine.playCount to engine.lastSeek)
    }

    @Test fun waitsForBothScanAndSessionSnapshot() = runTest {
        val scan = CompletableDeferred<List<Track>>()
        val engine = FakePlayerEngine()
        val vm = MixtapeViewModel(object : AudioRepository {
            override suspend fun loadTracks() = scan.await()
        }, MixtapeController(engine))
        vm.onPermissionResult(true)
        vm.openNowPlayingFromNotification()
        engine.simulateExternalPlaybackSnapshot(tracks, 0, true, 9_000, 100_000)
        assertEquals(MixtapeScreen.MixTapes, vm.uiState.value.screen)
        scan.complete(tracks)
        assertEquals(MixtapeScreen.NowPlaying, vm.uiState.value.screen)
        assertEquals(9_000L, vm.uiState.value.positionMs)
        assertEquals(0, engine.playCount)

        val lateEngine = FakePlayerEngine()
        val lateVm = makeViewModel(lateEngine)
        lateVm.onPermissionResult(true)
        lateVm.openNowPlayingFromNotification()
        assertEquals(MixtapeScreen.MixTapes, lateVm.uiState.value.screen)
        lateEngine.simulateExternalPlaybackSnapshot(tracks, 0, false, 0, 100_000)
        assertEquals(MixtapeScreen.NowPlaying, lateVm.uiState.value.screen)
    }

    @Test fun emptyQueueAndDeniedPermissionStayOffPlayer() = runTest {
        val engine = FakePlayerEngine()
        val vm = makeViewModel(engine)
        vm.onPermissionResult(true)
        vm.openNowPlayingFromNotification()
        engine.simulateExternalPlaybackSnapshot(emptyList(), -1, false, 0, 0)
        assertEquals(MixtapeScreen.MixTapes, vm.uiState.value.screen)
        vm.onPermissionResult(false)
        vm.openNowPlayingFromNotification()
        assertEquals(LibraryStatus.PermissionRequired, vm.uiState.value.status)
        assertEquals(MixtapeScreen.MixTapes, vm.uiState.value.screen)
    }

    private fun makeViewModel(engine: FakePlayerEngine) = MixtapeViewModel(
        object : AudioRepository { override suspend fun loadTracks() = tracks },
        MixtapeController(engine),
    )
}
