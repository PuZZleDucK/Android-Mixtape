package com.example.androidmixtape.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTrackMapperTest {
    @Test
    fun mapsRowsToStableContentUrisWithFallbacks() {
        val tracks = AudioTrackMapper.mapRows(
            sequenceOf(
                AudioRow(id = 42, title = " ", artist = null, displayName = "mixtape-smoke.mp3", durationMs = 95_000),
            ),
        )

        assertEquals(1, tracks.size)
        assertEquals(42, tracks.single().id)
        assertEquals("mixtape-smoke.mp3", tracks.single().title)
        assertEquals("Unknown artist", tracks.single().artist)
        assertEquals("content://media/external/audio/media/42", tracks.single().uri)
    }

    @Test
    fun ignoresMissingIdsAndNonPositiveDurations() {
        val tracks = AudioTrackMapper.mapRows(
            sequenceOf(
                AudioRow(id = null, title = "No id", artist = "A", displayName = "bad.mp3", durationMs = 1),
                AudioRow(id = 2, title = "Zero", artist = "A", displayName = "zero.mp3", durationMs = 0),
                AudioRow(id = 3, title = "Negative", artist = "A", displayName = "negative.mp3", durationMs = -1),
            ),
        )

        assertTrue(tracks.isEmpty())
    }

    @Test
    fun sortsDeterministicallyByTitleThenId() {
        val tracks = AudioTrackMapper.mapRows(
            sequenceOf(
                AudioRow(id = 3, title = "beta", artist = "A", displayName = "b.mp3", durationMs = 1),
                AudioRow(id = 2, title = "Alpha", artist = "A", displayName = "a2.mp3", durationMs = 1),
                AudioRow(id = 1, title = "alpha", artist = "A", displayName = "a1.mp3", durationMs = 1),
            ),
        )

        assertEquals(listOf(1L, 2L, 3L), tracks.map { it.id })
    }

    @Test
    fun emptyRowsReturnEmptyList() {
        assertTrue(AudioTrackMapper.mapRows(emptySequence()).isEmpty())
    }
}
