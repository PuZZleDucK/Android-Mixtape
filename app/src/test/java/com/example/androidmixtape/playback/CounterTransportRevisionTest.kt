package com.example.androidmixtape.playback

import com.example.androidmixtape.data.Track
import org.junit.Assert.*
import org.junit.Test

class CounterTransportRevisionTest {
    private val tracks = listOf(
        Track(1, "One", "Artist", 60_000, "content://one"),
        Track(2, "Two", "Artist", 90_000, "content://two"),
    )
    private val engine = RevisionEngine()
    private val controller = MixtapeController(engine).apply { load(tracks); select(0) }

    private fun snapshot(position: Long = 0, discontinuity: Boolean = false, index: Int = 0) =
        ExternalPlaybackSnapshot(tracks, index, "track:${tracks[index].id}", true, position,
            tracks[index].durationMs, discontinuity)

    @Test fun ordinaryProgressAndPauseDoNotAdvanceRevision() {
        val revision = controller.state.counterRevision
        engine.emit(snapshot(100))
        engine.emit(snapshot(200))
        engine.emit(snapshot(200).copy(isPlaying = false))
        assertEquals(revision, controller.state.counterRevision)
        controller.togglePlayPause()
        assertEquals(revision, controller.state.counterRevision)
    }

    @Test fun adjacentLocalSeekCancelsRollWithoutChangingSeekSemantics() {
        val revision = controller.state.counterRevision
        controller.seekTo(150)
        assertEquals(150L, engine.lastSeek)
        assertEquals(150L, controller.state.positionMs)
        assertTrue(controller.state.counterRevision > revision)
        val wheel = CounterWheelState.settled(8, revision).update(9, true, controller.state.counterRevision)
        assertFalse(wheel.rolling)
        assertEquals(9, wheel.target)
    }

    @Test fun externalSeekPublishesRevisionEvenWithinSameTrackAndPlaybackState() {
        var published: PlayerUiState? = null
        controller.setOnPlaybackStateChanged { published = it }
        val revision = controller.state.counterRevision
        engine.emit(snapshot(150, discontinuity = true))
        assertEquals(revision + 1, published!!.counterRevision)
        assertEquals(150L, published!!.positionMs)
        engine.emit(snapshot(160))
        assertEquals(revision + 1, published!!.counterRevision)
    }

    @Test fun resetAndRepeatedLoadsNeverReuseRevision() {
        var revision = controller.state.counterRevision
        controller.stop()
        assertTrue(controller.state.counterRevision > revision)
        assertFalse(controller.state.isPlaying)
        assertEquals(0L, controller.state.positionMs)
        repeat(3) {
            revision = controller.state.counterRevision
            controller.load(emptyList())
            assertTrue(controller.state.counterRevision > revision)
            revision = controller.state.counterRevision
            controller.load(tracks)
            assertTrue(controller.state.counterRevision > revision)
        }
    }

    @Test fun trackAndQueueChangesInvalidatePresentation() {
        var revision = controller.state.counterRevision
        controller.select(1)
        assertTrue(controller.state.counterRevision > revision)
        revision = controller.state.counterRevision
        engine.emit(snapshot(index = 0))
        assertTrue(controller.state.counterRevision > revision)
        revision = controller.state.counterRevision
        controller.replaceQueuePreservingCurrentTrack(tracks.reversed())
        assertTrue(controller.state.counterRevision > revision)
        revision = controller.state.counterRevision
        controller.replaceQueueAfterCurrentRemoval(emptyList(), -1, false)
        assertTrue(controller.state.counterRevision > revision)
    }

    private class RevisionEngine : PlayerEngine {
        var snapshotListener: ((ExternalPlaybackSnapshot) -> Unit)? = null
        var lastSeek = -1L
        fun emit(snapshot: ExternalPlaybackSnapshot) { snapshotListener?.invoke(snapshot) }
        override fun loadPlaylist(tracks: List<Track>) {}
        override fun replacePlaylistPreservingPlayback(tracks: List<Track>, currentIndex: Int) {}
        override fun playIndex(index: Int) {}
        override fun play() {}
        override fun pause() {}
        override fun seekTo(positionMs: Long) { lastSeek = positionMs }
        override fun setOnCurrentIndexChanged(listener: ((Int) -> Unit)?) {}
        override fun setOnExternalPlaybackSnapshotChanged(listener: ((ExternalPlaybackSnapshot) -> Unit)?) {
            snapshotListener = listener
        }
        override fun release() {}
    }
}
