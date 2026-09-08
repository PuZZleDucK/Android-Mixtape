package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import com.example.androidmixtape.playback.PlayerEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingSettingsNavigationTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun playingRoundTrip() = roundTrip(false)
    @Test fun pausedRoundTrip() = roundTrip(true)

    private fun roundTrip(paused: Boolean) = runTest {
        val tracks = (1..4).map { Track(it.toLong(), "Track $it", "Artist", 180_000L, "content://track/$it") }
        val engine = FakePlayerEngine()
        val commandLog = mutableListOf<String>()
        val recordingEngine = object : PlayerEngine by engine {
            override fun loadPlaylist(tracks: List<Track>) {
                commandLog += "loadPlaylist"
                engine.loadPlaylist(tracks)
            }
            override fun playIndex(index: Int) {
                commandLog += "playIndex:$index"
                engine.playIndex(index)
            }
            override fun seekTo(positionMs: Long) {
                commandLog += "seekTo:$positionMs"
                engine.seekTo(positionMs)
            }
            override fun release() {
                commandLog += "release"
                engine.release()
            }
        }
        val store = InMemoryMixtapeSettingsStore()
        val vm = MixtapeViewModel(
            repository = object : AudioRepository { override suspend fun loadTracks() = tracks },
            controller = MixtapeController(recordingEngine),
            settingsStore = store,
        )
        vm.onPermissionResult(true)
        vm.selectMixTapeGroup(0)
        vm.next()
        vm.seekTo(27_000L)
        if (paused) vm.togglePlayPause()
        val before = vm.uiState.value
        val commands = listOf(engine.playCount, engine.pauseCount, engine.playedIndex, engine.lastSeek, engine.replacePreservingCount)
        val recordedCommands = commandLog.toList()
        vm.showSettings()
        vm.showDeckThemeSettings()
        vm.updateDeckTheme(DeckTheme.BlackoutPortable)
        vm.showSettings()
        assertEquals(MixtapeScreen.Settings, vm.uiState.value.screen)
        vm.showHandwritingFontSettings()
        vm.showSettings()
        val messiness = HandwritingMessiness.entries.first { it != before.mixtapeSettings.handwritingMessiness }
        vm.updateHandwritingMessiness(messiness)
        vm.exitSettings()
        val after = vm.uiState.value
        assertEquals(MixtapeScreen.NowPlaying, after.screen)
        assertEquals(before.queueTracks, after.queueTracks)
        assertEquals(before.currentMixtapeIndex, after.currentMixtapeIndex)
        assertEquals(before.currentTrack, after.currentTrack)
        assertEquals(before.currentIndex, after.currentIndex)
        assertEquals(before.positionMs, after.positionMs)
        assertEquals(before.isPlaying, after.isPlaying)
        assertEquals(commands, listOf(engine.playCount, engine.pauseCount, engine.playedIndex, engine.lastSeek, engine.replacePreservingCount))
        assertEquals("Settings must not reload, reselect, seek or release playback", recordedCommands, commandLog)
        assertEquals(messiness, store.settings().handwritingMessiness)
        assertEquals(DeckTheme.BlackoutPortable, after.mixtapeThemeSettings.deckTheme)
        vm.showMixTapes()
        vm.showSettings()
        vm.showHelp()
        vm.showSettings()
        vm.exitSettings()
        assertEquals(MixtapeScreen.MixTapes, vm.uiState.value.screen)
        vm.selectMixTapeGroup(0)
        vm.showSettings()
        vm.exitSettings()
        assertEquals(MixtapeScreen.NowPlaying, vm.uiState.value.screen)
    }
}
