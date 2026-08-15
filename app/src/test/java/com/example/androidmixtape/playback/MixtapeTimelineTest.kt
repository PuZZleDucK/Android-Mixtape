package com.example.androidmixtape.playback

import com.example.androidmixtape.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeTimelineTest {
    @Test
    fun completeMixtapeTimelineStartsAtZeroAndEndsAt999() {
        val tracks = listOf(track(1, 60_000), track(2, 180_000))

        val start = mixtapePlaybackProgress(tracks, currentIndex = 0, currentTrackPositionMs = 0)
        val end = mixtapePlaybackProgress(tracks, currentIndex = 1, currentTrackPositionMs = 180_000)

        assertEquals(0f, start, 0.0001f)
        assertEquals(0, mixtapeCounterValue(start))
        assertEquals(1f, end, 0.0001f)
        assertEquals(999, mixtapeCounterValue(end))
    }

    @Test
    fun progressIncludesCompletedTracksBeforeTheCurrentTrack() {
        val tracks = listOf(track(1, 100_000), track(2, 300_000))

        val halfway = mixtapePlaybackProgress(tracks, currentIndex = 1, currentTrackPositionMs = 100_000)

        assertEquals(0.5f, halfway, 0.0001f)
        assertEquals(500, mixtapeCounterValue(halfway))
    }

    @Test
    fun shorterMixtapeAdvancesCounterAndReelsFasterForSameElapsedTime() {
        val shortTape = listOf(track(1, 100_000), track(2, 100_000))
        val longTape = listOf(track(1, 300_000), track(2, 300_000))

        val shortProgress = mixtapePlaybackProgress(shortTape, currentIndex = 0, currentTrackPositionMs = 50_000)
        val longProgress = mixtapePlaybackProgress(longTape, currentIndex = 0, currentTrackPositionMs = 50_000)

        assertTrue(shortProgress > longProgress)
        assertTrue(mixtapeCounterValue(shortProgress) > mixtapeCounterValue(longProgress))
    }

    @Test
    fun invalidOrUnknownTimelineStaysAtStart() {
        val unknownDurations = listOf(track(1, 0), track(2, -1))

        assertEquals(0f, mixtapePlaybackProgress(emptyList(), 0, 10_000), 0.0001f)
        assertEquals(0f, mixtapePlaybackProgress(unknownDurations, 0, 10_000), 0.0001f)
        assertEquals(0f, mixtapePlaybackProgress(listOf(track(1, 100_000)), -1, 10_000), 0.0001f)
    }

    private fun track(id: Long, durationMs: Long) = Track(
        id = id,
        title = "Track $id",
        artist = "Artist",
        durationMs = durationMs,
        uri = "content://track/$id",
    )
}
