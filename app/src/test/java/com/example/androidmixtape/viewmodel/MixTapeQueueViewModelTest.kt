package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MixTapeQueueViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refreshOpensMixTapeListAndKeepsFullLibraryAvailable() = runTest {
        val tracks = numberedTracks(25)
        val viewModel = viewModelWith(MixTapeFakeRepository(tracks))

        viewModel.onPermissionResult(true)

        val state = viewModel.uiState.value
        assertEquals(LibraryStatus.Ready, state.status)
        assertEquals(MixtapeScreen.MixTapes, state.screen)
        assertEquals("Main library must continue to expose all audio tracks", tracks, state.tracks)
        assertEquals(listOf("Mix Tape 1"), state.mixTapeGroups.map { it.name })
        assertEquals(listOf(25), state.mixTapeGroups.map { it.tracks.size })
    }

    @Test
    fun selectingSecondMixTapeQueuesOnlyItsRandomizedGroup() = runTest {
        val tracks = numberedTracks(58)
        val viewModel = viewModelWith(MixTapeFakeRepository(tracks))
        viewModel.onPermissionResult(true)

        viewModel.showMixTapes()
        assertEquals(MixtapeScreen.MixTapes, viewModel.uiState.value.screen)
        val expectedQueue = viewModel.uiState.value.mixTapeGroups[1].tracks

        viewModel.selectMixTapeGroup(1)

        var state = viewModel.uiState.value
        assertEquals(MixtapeScreen.NowPlaying, state.screen)
        assertEquals("Selecting a mix tape must not replace the full library list", tracks, state.tracks)
        assertEquals(expectedQueue, state.queueTracks)
        assertEquals(expectedQueue.first(), state.currentTrack)
        assertEquals(0, state.currentIndex)
        assertFalse(state.canGoPrevious)
        assertTrue(state.canGoNext)

        repeat(expectedQueue.size - 1) { viewModel.next() }
        state = viewModel.uiState.value
        assertEquals(expectedQueue.last(), state.currentTrack)
        assertFalse(state.canGoNext)

        viewModel.next()
        assertEquals("Next must stay inside the selected mix tape queue", expectedQueue.last(), viewModel.uiState.value.currentTrack)
    }

    @Test
    fun selectingLastMixTapeQueuesOnlyThatTapeAndKeepsFullLibraryAvailable() = runTest {
        val tracks = numberedTracks(57)
        val viewModel = viewModelWith(MixTapeFakeRepository(tracks))
        viewModel.onPermissionResult(true)
        val expectedQueue = viewModel.uiState.value.mixTapeGroups.last().tracks

        viewModel.selectMixTapeGroup(viewModel.uiState.value.mixTapeGroups.lastIndex)

        val state = viewModel.uiState.value
        assertEquals(MixtapeScreen.NowPlaying, state.screen)
        assertEquals(tracks, state.tracks)
        assertEquals(expectedQueue, state.queueTracks)
        assertEquals(expectedQueue.first(), state.currentTrack)
        assertEquals(0, state.currentIndex)
        assertFalse(state.canGoPrevious)
        assertFalse(state.canGoNext)
    }

    @Test
    fun resetAllMixTapesReshufflesOnlyTheMixtapeOrderAndPreservesLibraryTracks() = runTest {
        val tracks = numberedTracks(57)
        val viewModel = viewModelWith(MixTapeFakeRepository(tracks), mixtapeRandom = Random(7))
        viewModel.onPermissionResult(true)
        val initialMixTapeOrder = viewModel.uiState.value.mixTapeGroups.flatMap { it.tracks }

        viewModel.resetAllMixTapes()

        val state = viewModel.uiState.value
        val resetMixTapeOrder = state.mixTapeGroups.flatMap { it.tracks }
        assertEquals(MixtapeScreen.MixTapes, state.screen)
        assertEquals("2 mix tapes reset", state.message)
        assertEquals("Main library order must remain unchanged after mixtape reset", tracks, state.tracks)
        assertSameTrackSetWithoutDuplicates(tracks, resetMixTapeOrder)
        assertEquals(listOf(56, 1), state.mixTapeGroups.map { it.tracks.size })
        assertNotEquals("Reset should recreate mixtapes with a new randomized order", initialMixTapeOrder, resetMixTapeOrder)
    }

    @Test
    fun settingsResetStopsPlaybackClearsStaleQueueAndReturnsToMixTapes() = runTest {
        val tracks = numberedTracks(13)
        val fakePlayer = FakePlayerEngine()
        val viewModel = viewModelWith(
            repository = MixTapeFakeRepository(tracks),
            controller = MixtapeController(fakePlayer),
            mixtapeRandom = Random(11),
        )
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)
        assertTrue(viewModel.uiState.value.isPlaying)

        viewModel.showSettings()
        assertEquals(MixtapeScreen.Settings, viewModel.uiState.value.screen)

        viewModel.resetAllMixTapes()

        val state = viewModel.uiState.value
        assertEquals(MixtapeScreen.MixTapes, state.screen)
        assertFalse(state.isPlaying)
        assertEquals(emptyList<Track>(), state.queueTracks)
        assertNull(state.currentTrack)
        assertEquals(1, fakePlayer.pauseCount)
        assertEquals(0L, fakePlayer.lastSeek)
    }

    @Test
    fun uiStateExposesDefaultMixtapeSettings() = runTest {
        val viewModel = viewModelWith(MixTapeFakeRepository(numberedTracks(25)))

        viewModel.onPermissionResult(true)

        val state = viewModel.uiState.value
        assertEquals(MixtapeScreen.MixTapes, state.screen)
        assertEquals(MixtapeSettings(), state.mixtapeSettings)
        assertEquals(DEFAULT_SONGS_PER_MIXTAPE, state.mixtapeSettings.songsPerMixTape)
        assertEquals(ArtistGrouping.ArtistTripletsAcrossTapes, state.mixtapeSettings.artistGrouping)
        assertEquals(listOf(25), state.mixTapeGroups.map { it.tracks.size })
    }

    @Test
    fun changingSongsPerMixtapeRebuildsGroupsAndKeepsSelectionBoundedToSelectedTape() = runTest {
        val tracks = numberedTracks(25)
        val viewModel = viewModelWith(MixTapeFakeRepository(tracks))
        viewModel.onPermissionResult(true)

        viewModel.updateSongsPerMixTape(NORMAL_SONGS_PER_MIXTAPE)

        var state = viewModel.uiState.value
        assertEquals(NORMAL_SONGS_PER_MIXTAPE, state.mixtapeSettings.songsPerMixTape)
        assertEquals(listOf(24, 1), state.mixTapeGroups.map { it.tracks.size })
        assertEquals(tracks, state.mixTapeGroups.flatMap { it.tracks })

        val expectedQueue = state.mixTapeGroups[1].tracks
        viewModel.selectMixTapeGroup(1)

        state = viewModel.uiState.value
        assertEquals(expectedQueue, state.queueTracks)
        assertEquals(expectedQueue.first(), state.currentTrack)
        repeat(expectedQueue.size - 1) { viewModel.next() }
        assertEquals(expectedQueue.last(), viewModel.uiState.value.currentTrack)
        viewModel.next()
        assertEquals("Next must stay inside the selected normal-size mix tape", expectedQueue.last(), viewModel.uiState.value.currentTrack)
    }

    @Test
    fun nonPositiveSongsPerMixtapeSettingIsRejectedAndKeepsPreviousGroups() = runTest {
        val viewModel = viewModelWith(MixTapeFakeRepository(numberedTracks(13)))
        viewModel.onPermissionResult(true)

        viewModel.updateSongsPerMixTape(0)

        val state = viewModel.uiState.value
        assertEquals(DEFAULT_SONGS_PER_MIXTAPE, state.mixtapeSettings.songsPerMixTape)
        assertEquals(listOf(13), state.mixTapeGroups.map { it.tracks.size })
    }

    @Test
    fun changingArtistGroupingRebuildsGroupsWithoutDroppingOrDuplicatingTracks() = runTest {
        val tracks = artistTracks()
        val viewModel = viewModelWith(MixTapeFakeRepository(tracks))
        viewModel.onPermissionResult(true)
        viewModel.updateSongsPerMixTape(NORMAL_SONGS_PER_MIXTAPE)

        viewModel.updateArtistGrouping(ArtistGrouping.ArtistTripletsAcrossTapes)

        val state = viewModel.uiState.value
        assertEquals(ArtistGrouping.ArtistTripletsAcrossTapes, state.mixtapeSettings.artistGrouping)
        assertEquals(listOf(12), state.mixTapeGroups.map { it.tracks.size })
        assertSameTrackSetWithoutDuplicates(tracks, state.mixTapeGroups.flatMap { it.tracks })
    }

    @Test
    fun changingArtistGroupingFillsTapesAndSelectedQueuesStayBounded() = runTest {
        val tracks = singleArtistTracks(25)
        val viewModel = viewModelWith(MixTapeFakeRepository(tracks))
        viewModel.onPermissionResult(true)
        viewModel.updateSongsPerMixTape(NORMAL_SONGS_PER_MIXTAPE)

        viewModel.updateArtistGrouping(ArtistGrouping.ArtistTripletsAcrossTapes)

        var state = viewModel.uiState.value
        assertEquals(ArtistGrouping.ArtistTripletsAcrossTapes, state.mixtapeSettings.artistGrouping)
        assertEquals(listOf(24, 1), state.mixTapeGroups.map { it.tracks.size })
        state.mixTapeGroups.dropLast(1).forEach { group ->
            assertEquals("Every non-final tape should match the selected songs-per-mixtape count", NORMAL_SONGS_PER_MIXTAPE, group.tracks.size)
        }
        assertSameTrackSetWithoutDuplicates(tracks, state.mixTapeGroups.flatMap { it.tracks })

        val expectedQueue = state.mixTapeGroups.last().tracks
        viewModel.selectMixTapeGroup(state.mixTapeGroups.lastIndex)

        state = viewModel.uiState.value
        assertEquals(expectedQueue, state.queueTracks)
        assertEquals(expectedQueue.first(), state.currentTrack)
        viewModel.next()
        assertEquals("Next must stay inside the selected triplet-spread tape", expectedQueue.last(), viewModel.uiState.value.currentTrack)
    }

    private fun viewModelWith(
        repository: AudioRepository,
        controller: MixtapeController = MixtapeController(FakePlayerEngine()),
        mixtapeRandom: Random = Random(0),
    ): MixtapeViewModel = MixtapeViewModel(
        repository = repository,
        controller = controller,
        mixtapeRandom = mixtapeRandom,
    )

    private fun assertSameTrackSetWithoutDuplicates(expected: List<Track>, actual: List<Track>) {
        assertEquals(expected.map { it.id }.sorted(), actual.map { it.id }.sorted())
        assertEquals("Mixtape reset must not duplicate or drop tracks", expected.size, actual.distinctBy { it.id }.size)
    }
}

private class MixTapeFakeRepository(
    private val tracks: List<Track> = emptyList(),
) : AudioRepository {
    override suspend fun loadTracks(): List<Track> = tracks
}

private fun numberedTracks(count: Int): List<Track> = (1..count).map { number ->
    Track(
        id = number.toLong(),
        title = "Track $number",
        artist = "Artist $number",
        durationMs = number * 1_000L,
        uri = "content://track/$number",
    )
}

private fun artistTracks(): List<Track> = (1..6).map { number ->
    Track(
        id = number.toLong(),
        title = "Alpha Track $number",
        artist = "Alpha",
        durationMs = number * 1_000L,
        uri = "content://track/$number",
    )
} + (7..12).map { number ->
    Track(
        id = number.toLong(),
        title = "Beta Track $number",
        artist = "Beta",
        durationMs = number * 1_000L,
        uri = "content://track/$number",
    )
}

private fun singleArtistTracks(count: Int): List<Track> = (1..count).map { number ->
    Track(
        id = number.toLong(),
        title = "Alpha Track $number",
        artist = "Alpha",
        durationMs = number * 1_000L,
        uri = "content://track/$number",
    )
}
