package com.example.androidmixtape.playback

import android.media.AudioManager
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.ui.AndroidMixtapeTheme
import com.example.androidmixtape.ui.MixtapeApp
import com.example.androidmixtape.viewmodel.LibraryStatus
import com.example.androidmixtape.viewmodel.MixtapeViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real deck gestures and AudioTrack cues; a silent song engine isolates cue output. */
class DeckTransportCueUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bothDeckButtonsProduceAudioBeforeChangingTracks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audio = context.getSystemService(AudioManager::class.java)
        val store = ViewModelStore()
        val cue = AudioTrackTransportCuePlayer()
        val tracks = (1..3).map { Track(it.toLong(), "Track $it", "Cue check", 60_000, "preview://cue/$it") }
        lateinit var vm: MixtapeViewModel
        compose.runOnUiThread {
            vm = MixtapeViewModel(
                repository = object : AudioRepository { override suspend fun loadTracks() = tracks },
                controller = MixtapeController(SilentSongEngine()),
                transportCuePlayer = cue,
            )
            store.put("cue-check", vm)
            vm.onPermissionResult(true)
        }
        try {
            compose.waitUntil(5_000) { vm.uiState.value.status == LibraryStatus.Ready }
            compose.runOnUiThread { vm.selectMixTapeGroup(0) }
            compose.setContent {
                AndroidMixtapeTheme {
                    MixtapeApp(
                        state = vm.uiState.collectAsState().value,
                        onRequestPermission = {}, onRefresh = {},
                        onTogglePlayPause = vm::togglePlayPause,
                        onPrevious = vm::previousWithCue, onNext = vm::nextWithCue,
                        onStop = vm::stop, onSeekTo = vm::seekTo,
                    )
                }
            }
            compose.waitUntil(3_000) { !audio.isMusicActive }
            for ((button, target) in listOf("Fast-forward" to 1, "Rewind" to 0)) {
                val oldIndex = vm.uiState.value.currentIndex
                compose.onNodeWithContentDescription(button).performClick()
                compose.waitUntil(2_000) { audio.isMusicActive }
                assertFalse("Song must pause during $button cue", vm.uiState.value.isPlaying)
                assertEquals("$button must not skip its cue", oldIndex, vm.uiState.value.currentIndex)
                compose.waitUntil(8_000) { vm.uiState.value.currentIndex == target && vm.uiState.value.isPlaying }
                compose.waitUntil(3_000) { !audio.isMusicActive }
            }
        } finally {
            compose.runOnUiThread { store.clear() }
        }
    }

    private class SilentSongEngine : PlayerEngine {
        override fun loadPlaylist(tracks: List<Track>) = Unit
        override fun replacePlaylistPreservingPlayback(tracks: List<Track>, currentIndex: Int) = Unit
        override fun playIndex(index: Int) = Unit
        override fun play() = Unit
        override fun pause() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun setOnCurrentIndexChanged(listener: ((Int) -> Unit)?) = Unit
        override fun setOnExternalPlaybackSnapshotChanged(listener: ((ExternalPlaybackSnapshot) -> Unit)?) = Unit
        override fun release() = Unit
    }
}
