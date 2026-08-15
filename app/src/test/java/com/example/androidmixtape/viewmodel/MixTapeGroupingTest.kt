package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixTapeGroupingTest {
    @Test
    fun emptyLibraryProducesNoMixTapeGroups() {
        assertTrue(buildMixTapeGroups(emptyList()).isEmpty())
    }

    @Test
    fun groupsTracksIntoStableDefaultLpMixTapesWithHumanNames() {
        val tracks = numberedTracks(57)

        val groups = buildMixTapeGroups(tracks)

        assertEquals(listOf("Mix Tape 1", "Mix Tape 2"), groups.map { it.name })
        assertEquals(listOf(56, 1), groups.map { it.tracks.size })
        assertEquals(tracks.subList(0, 56), groups[0].tracks)
        assertEquals(tracks.subList(56, 57), groups[1].tracks)
    }

    @Test
    fun customGroupSizeKeepsNamesAndOriginalTrackOrderForEdgeCases() {
        val tracks = numberedTracks(7)

        val groups = buildMixTapeGroups(tracks, groupSize = 3)

        assertEquals(listOf("Mix Tape 1", "Mix Tape 2", "Mix Tape 3"), groups.map { it.name })
        assertEquals(listOf(0, 3, 6), groups.map { it.startIndex })
        assertEquals(tracks.subList(0, 3), groups[0].tracks)
        assertEquals(tracks.subList(3, 6), groups[1].tracks)
        assertEquals(tracks.subList(6, 7), groups[2].tracks)
    }

    @Test
    fun defaultSettingsUseLpSongsAndArtistTripletsAcrossTapes() {
        val tracks = numberedTracks(57)

        val groups = buildMixTapeGroups(tracks, MixtapeSettings())

        assertEquals(56, DEFAULT_SONGS_PER_MIXTAPE)
        assertEquals(LP_SONGS_PER_MIXTAPE, DEFAULT_SONGS_PER_MIXTAPE)
        assertEquals(DEFAULT_SONGS_PER_MIXTAPE, MixtapeSettings().songsPerMixTape)
        assertEquals(ArtistGrouping.ArtistTripletsAcrossTapes, MixtapeSettings().artistGrouping)
        assertEquals(listOf(56, 1), groups.map { it.tracks.size })
        assertEquals(tracks, groups.flatMap { it.tracks })
    }

    @Test
    fun settingsCanChangeSongsPerMixtapeWithoutDroppingTracks() {
        val tracks = numberedTracks(25)

        val groups = buildMixTapeGroups(tracks, MixtapeSettings(songsPerMixTape = 8))

        assertEquals(listOf(8, 8, 8, 1), groups.map { it.tracks.size })
        assertEquals(tracks, groups.flatMap { it.tracks })
        assertEquals(listOf(0, 8, 16, 24), groups.map { it.startIndex })
    }

    @Test
    fun noGroupingPreservesTheCurrentTrackOrderExactly() {
        val tracks = listOf(
            track(1, "Alpha"),
            track(2, "Beta"),
            track(3, "Alpha"),
            track(4, "Beta"),
            track(5, "Alpha"),
            track(6, "Beta"),
        )

        val groups = buildMixTapeGroups(
            tracks,
            MixtapeSettings(songsPerMixTape = 3, artistGrouping = ArtistGrouping.NoGrouping),
        )

        assertEquals(listOf(listOf(1L, 2L, 3L), listOf(4L, 5L, 6L)), groups.map { group -> group.tracks.map { it.id } })
        assertEquals(tracks, groups.flatMap { it.tracks })
    }

    @Test
    fun keepArtistTogetherPacksSameArtistRunsOntoTheSameTapeWhenTheyFit() {
        val tracks = listOf(
            track(1, "Alpha"),
            track(2, "Beta"),
            track(3, "Alpha"),
            track(4, "Beta"),
            track(5, "Alpha"),
            track(6, "Beta"),
            track(7, "Alpha"),
            track(8, "Beta"),
        )

        val groups = buildMixTapeGroups(
            tracks,
            MixtapeSettings(songsPerMixTape = 4, artistGrouping = ArtistGrouping.KeepArtistTogether),
        )

        assertEquals(listOf(listOf("Alpha", "Alpha", "Alpha", "Alpha"), listOf("Beta", "Beta", "Beta", "Beta")), groups.map { group -> group.tracks.map { it.artist } })
        assertEquals(tracks.map { it.id }.sorted(), groups.flatMap { it.tracks }.map { it.id }.sorted())
    }

    @Test
    fun keepArtistTogetherMovesWholeArtistToSeparateTapeInsteadOfSplittingAcrossLeftoverSpace() {
        val tracks = listOf(
            track(1, "Alpha"),
            track(2, "Alpha"),
            track(3, "Alpha"),
            track(4, "Beta"),
            track(5, "Beta"),
            track(6, "Beta"),
            track(7, "Beta"),
        )

        val groups = buildMixTapeGroups(
            tracks,
            MixtapeSettings(songsPerMixTape = 5, artistGrouping = ArtistGrouping.KeepArtistTogether),
        )

        assertEquals(
            "Beta has four songs and fits on one tape, so it must not be split just to fill Alpha's leftover space.",
            listOf(listOf("Alpha", "Alpha", "Alpha"), listOf("Beta", "Beta", "Beta", "Beta")),
            groups.map { group -> group.tracks.map { it.artist } },
        )
        assertEquals(tracks.map { it.id }.sorted(), groups.flatMap { it.tracks }.map { it.id }.sorted())
    }

    @Test
    fun artistTripletsAcrossTapesSplitsArtistsIntoThreeSongChunksOnDifferentTapes() {
        val tracks = (1..6).map { track(it.toLong(), "Alpha") } +
            (7..12).map { track(it.toLong(), "Beta") }

        val groups = buildMixTapeGroups(
            tracks,
            MixtapeSettings(songsPerMixTape = 6, artistGrouping = ArtistGrouping.ArtistTripletsAcrossTapes),
        )

        assertEquals(listOf(6, 6), groups.map { it.tracks.size })
        assertEquals(listOf(listOf(1L, 2L, 3L, 7L, 8L, 9L), listOf(4L, 5L, 6L, 10L, 11L, 12L)), groups.map { group -> group.tracks.map { it.id } })
        groups.forEach { group ->
            assertTrue("Each tape should contain no more than three tracks from one artist", group.tracks.groupBy { it.artist }.values.all { it.size <= 3 })
        }
        assertEquals(tracks.map { it.id }.sorted(), groups.flatMap { it.tracks }.map { it.id }.sorted())
    }

    @Test
    fun artistTripletsAcrossTapesStillFillsTapesForSingleArtist() {
        val tracks = (1..10).map { track(it.toLong(), "Alpha") }

        val groups = buildMixTapeGroups(
            tracks,
            MixtapeSettings(songsPerMixTape = 9, artistGrouping = ArtistGrouping.ArtistTripletsAcrossTapes),
        )

        assertEquals(
            "Artist triplet spreading must not create a long tail of underfilled tapes; every tape except the last should use the selected size.",
            listOf(9, 1),
            groups.map { it.tracks.size },
        )
        groups.dropLast(1).forEach { group ->
            assertEquals("Every non-final tape should match the configured songs-per-mixtape limit", 9, group.tracks.size)
        }
        groups.forEach { group ->
            assertTrue("No tape may exceed the configured songs-per-mixtape limit", group.tracks.size <= 9)
        }
        assertEquals(tracks.map { it.id }.sorted(), groups.flatMap { it.tracks }.map { it.id }.sorted())
    }

    @Test
    fun artistTripletsAcrossTapesKeepsMixedArtistsFairAndMetadataCoherentWhenAddingTapes() {
        val tracks = (1..10).map { track(it.toLong(), "Alpha") } +
            (11..16).map { track(it.toLong(), "Beta") }

        val groups = buildMixTapeGroups(
            tracks,
            MixtapeSettings(songsPerMixTape = 9, artistGrouping = ArtistGrouping.ArtistTripletsAcrossTapes),
        )

        groups.forEach { group ->
            assertTrue("No tape may exceed the configured songs-per-mixtape limit", group.tracks.size <= 9)
        }
        groups.dropLast(1).forEach { group ->
            assertEquals("Every non-final tape should match the configured songs-per-mixtape limit", 9, group.tracks.size)
        }
        assertEquals(tracks.map { it.id }.sorted(), groups.flatMap { it.tracks }.map { it.id }.sorted())
        assertEquals(listOf("Mix Tape 1", "Mix Tape 2"), groups.map { it.name })
        assertEquals(groups.runningFold(0) { start, group -> start + group.tracks.size }.dropLast(1), groups.map { it.startIndex })
    }
}

private fun numberedTracks(count: Int): List<Track> = (1..count).map { number ->
    track(id = number.toLong(), artist = "Artist $number")
}

private fun track(id: Long, artist: String): Track = Track(
    id = id,
    title = "Track $id",
    artist = artist,
    durationMs = id * 1_000L,
    uri = "content://track/$id",
)
