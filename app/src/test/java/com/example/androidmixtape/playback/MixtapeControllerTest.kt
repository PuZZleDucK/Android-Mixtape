package com.example.androidmixtape.playback

import com.example.androidmixtape.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeControllerTest {
    private val tracks = listOf(
        Track(1, "One", "Artist", 60_000, "content://one"),
        Track(2, "Two", "Artist", 90_000, "content://two"),
    )

    @Test
    fun loadingNonEmptyTrackListInitializesPlaylistAtIndexZero() {
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)

        controller.load(tracks)

        assertEquals(tracks, fake.loaded)
        assertEquals(0, controller.state.currentIndex)
        assertEquals(tracks.first(), controller.state.currentTrack)
        assertFalse(controller.state.isPlaying)
    }

    @Test
    fun selectingTrackSeeksToIndexAndStartsPlayback() {
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)
        controller.load(tracks)

        controller.select(1)

        assertEquals(1, fake.playedIndex)
        assertTrue(controller.state.isPlaying)
        assertEquals(tracks[1], controller.state.currentTrack)
        assertEquals(90_000, controller.state.durationMs)
    }

    @Test
    fun togglePlayPauseUpdatesStateAndPlayer() {
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)
        controller.load(tracks)
        controller.select(0)

        controller.togglePlayPause()
        assertFalse(controller.state.isPlaying)
        assertEquals(1, fake.pauseCount)

        controller.togglePlayPause()
        assertTrue(controller.state.isPlaying)
        assertEquals(1, fake.playCount)
    }

    @Test
    fun nextAndPreviousStayWithinBounds() {
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)
        controller.load(tracks)
        controller.select(0)

        controller.previous()
        assertEquals(0, controller.state.currentIndex)

        controller.next()
        assertEquals(1, controller.state.currentIndex)
        assertFalse(controller.state.canGoNext)

        controller.next()
        assertEquals(1, controller.state.currentIndex)
    }

    @Test
    fun automaticPlayerTransitionUpdatesCurrentTrackWithoutManualNext() {
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)
        controller.load(tracks)
        controller.select(0)

        fake.simulateAutomaticTransitionTo(1)

        assertEquals(
            "When playback advances by itself, the controller should update the UI currentIndex without a manual next() call.",
            1,
            controller.state.currentIndex,
        )
        assertEquals(tracks[1], controller.state.currentTrack)
        assertTrue(controller.state.isPlaying)
        assertEquals(0L, controller.state.positionMs)
        assertEquals(90_000L, controller.state.durationMs)
    }

    @Test
    fun automaticPlayerTransitionIgnoresOutOfRangeIndexes() {
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)
        controller.load(tracks)
        controller.select(0)

        fake.simulateAutomaticTransitionTo(99)

        assertEquals(0, controller.state.currentIndex)
        assertEquals(tracks[0], controller.state.currentTrack)
    }

    @Test
    fun externalPlaybackSnapshotReplacesStalePhoneQueueAndPlayState() {
        val phoneMixtape = listOf(
            Track(101, "Phone A1", "Phone Artist", 61_000, "content://phone/a1"),
            Track(102, "Phone A2", "Phone Artist", 62_000, "content://phone/a2"),
        )
        val carMixtape = listOf(
            Track(201, "Car B1", "Car Artist", 71_000, "content://car/b1"),
            Track(202, "Car B2", "Car Artist", 72_000, "content://car/b2"),
        )
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)
        controller.load(phoneMixtape)
        controller.select(0)

        fake.simulateExternalPlaybackSnapshot(
            tracks = carMixtape,
            currentIndex = 1,
            isPlaying = true,
            positionMs = 12_345L,
            durationMs = 72_000L,
        )

        assertEquals(
            "A car-initiated session queue replacement must replace the phone-visible queue, not map the car index into the stale phone mixtape.",
            carMixtape,
            controller.state.tracks,
        )
        assertEquals(1, controller.state.currentIndex)
        assertEquals(carMixtape[1], controller.state.currentTrack)
        assertTrue(controller.state.isPlaying)
        assertEquals(12_345L, controller.state.positionMs)
        assertEquals(72_000L, controller.state.durationMs)
    }

    @Test
    fun replacingQueueAfterDeletingDifferentTrackUpdatesEnginePlaylistAndRemapsCurrentTrack() {
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)
        val threeTracks = listOf(
            Track(1, "One", "Artist", 60_000, "content://one"),
            Track(2, "Two", "Artist", 90_000, "content://two"),
            Track(3, "Three", "Artist", 120_000, "content://three"),
        )
        controller.load(threeTracks)
        controller.select(1)
        controller.seekTo(25_000)

        controller.replaceQueuePreservingCurrentTrack(listOf(threeTracks[1], threeTracks[2]))

        assertEquals("The engine playlist must be replaced, not just the controller's UI state.", listOf(threeTracks[1], threeTracks[2]), fake.activePlaylist)
        assertEquals(listOf(threeTracks[1], threeTracks[2]), fake.replacedTracks)
        assertEquals("The preserved current track should be remapped from old index 1 to new index 0.", 0, fake.replacedCurrentIndex)
        assertEquals(1, fake.replacePreservingCount)
        assertEquals(0, controller.state.currentIndex)
        assertEquals(2L, controller.state.currentTrack?.id)
        assertTrue(controller.state.isPlaying)
        assertEquals("Queue replacement must not rewind the still-playing track.", 25_000L, controller.state.positionMs)

        controller.next()

        assertEquals("Next after deletion should advance to the remaining next track, not a deleted/stale index.", 1, fake.playedIndex)
        assertEquals(3L, controller.state.currentTrack?.id)
    }

    @Test
    fun seekClampsToTrackDuration() {
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)
        controller.load(tracks)
        controller.select(0)

        controller.seekTo(-10)
        assertEquals(0L, fake.lastSeek)
        controller.seekTo(99_999)
        assertEquals(60_000L, fake.lastSeek)
    }

    @Test
    fun stopPausesPlaybackRewindsCurrentTrackAndKeepsTrackSelected() {
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)
        controller.load(tracks)
        controller.select(1)
        controller.seekTo(25_000)

        controller.stop()

        assertEquals(1, fake.pauseCount)
        assertEquals(0L, fake.lastSeek)
        assertFalse(controller.state.isPlaying)
        assertEquals(0L, controller.state.positionMs)
        assertEquals(90_000L, controller.state.durationMs)
        assertEquals(tracks[1], controller.state.currentTrack)
    }

    @Test
    fun releaseHappensExactlyOnce() {
        val fake = FakePlayerEngine()
        val controller = MixtapeController(fake)

        controller.release()
        controller.release()

        assertEquals(1, fake.releaseCount)
        assertTrue(controller.state.isReleased)
    }
}

class FakePlayerEngine : PlayerEngine {
    var loaded: List<Track> = emptyList()
    var activePlaylist: List<Track> = emptyList()
    var replacedTracks: List<Track>? = null
    var replacedCurrentIndex: Int? = null
    var replacePreservingCount = 0
    var playedIndex: Int? = null
    var playCount = 0
    var pauseCount = 0
    var lastSeek: Long? = null
    var releaseCount = 0
    private var currentIndexChangedListener: ((Int) -> Unit)? = null
    private var externalPlaybackSnapshotListener: ((ExternalPlaybackSnapshot) -> Unit)? = null

    fun simulateAutomaticTransitionTo(index: Int) {
        currentIndexChangedListener?.invoke(index)
    }

    fun simulateExternalPlaybackSnapshot(
        tracks: List<Track>,
        currentIndex: Int,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
    ) {
        externalPlaybackSnapshotListener?.invoke(
            ExternalPlaybackSnapshot(
                tracks = tracks,
                currentIndex = currentIndex,
                currentMediaId = tracks.getOrNull(currentIndex)?.let { "track:${it.id}" },
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
    }

    override fun loadPlaylist(tracks: List<Track>) {
        loaded = tracks
        activePlaylist = tracks
    }

    override fun replacePlaylistPreservingPlayback(tracks: List<Track>, currentIndex: Int) {
        replacedTracks = tracks
        replacedCurrentIndex = currentIndex
        replacePreservingCount += 1
        activePlaylist = tracks
    }

    override fun playIndex(index: Int) {
        playedIndex = index
    }

    override fun play() {
        playCount += 1
    }

    override fun pause() {
        pauseCount += 1
    }

    override fun seekTo(positionMs: Long) {
        lastSeek = positionMs
    }

    override fun setOnCurrentIndexChanged(listener: ((Int) -> Unit)?) {
        currentIndexChangedListener = listener
    }

    override fun setOnExternalPlaybackSnapshotChanged(listener: ((ExternalPlaybackSnapshot) -> Unit)?) {
        externalPlaybackSnapshotListener = listener
    }

    override fun release() {
        releaseCount += 1
    }
}
