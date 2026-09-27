package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HandwritingFontSizeTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun largePreservesCurrentSizeAndIsTheDefault() {
        assertEquals(HandwritingFontSize.Large, MixtapeSettings().handwritingFontSize)
        assertEquals(1f, HandwritingFontSize.Large.scale, 0f)
        assertEquals(28f, 40f * HandwritingFontSize.Small.scale, 0.001f)
        assertEquals(34f, 40f * HandwritingFontSize.Medium.scale, 0.001f)
    }

    @Test fun changingSizePersistsWithoutChangingPlaybackOrMixtapes() = runTest {
        val tracks = (1..4).map { Track(it.toLong(), "Track $it", "Artist", 180_000, "content://track/$it") }
        val engine = FakePlayerEngine()
        val store = InMemoryMixtapeSettingsStore()
        fun create() = MixtapeViewModel(
            repository = object : AudioRepository { override suspend fun loadTracks() = tracks },
            controller = MixtapeController(engine), settingsStore = store,
        )
        val vm = create()
        vm.onPermissionResult(true)
        vm.selectMixTapeGroup(0)
        vm.seekTo(27_000)
        vm.showSettings()
        val before = vm.uiState.value
        val commands = listOf(engine.playCount, engine.pauseCount, engine.playedIndex, engine.lastSeek, engine.replacePreservingCount)
        for (size in HandwritingFontSize.entries) {
            vm.updateHandwritingFontSize(size)
            val after = vm.uiState.value
            assertEquals(size, after.mixtapeSettings.handwritingFontSize)
            assertEquals(size, store.settings().handwritingFontSize)
            assertEquals(size, create().uiState.value.mixtapeSettings.handwritingFontSize)
            assertEquals(before.queueTracks, after.queueTracks)
            assertEquals(before.currentTrack, after.currentTrack)
            assertEquals(before.positionMs, after.positionMs)
            assertEquals(before.isPlaying, after.isPlaying)
            assertEquals(before.mixTapeGroups, after.mixTapeGroups)
            assertEquals(commands, listOf(engine.playCount, engine.pauseCount, engine.playedIndex, engine.lastSeek, engine.replacePreservingCount))
        }
    }
}
