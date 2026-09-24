package com.example.androidmixtape.viewmodel

import android.content.IntentSender
import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.DeleteTrackResult
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.name.InMemoryMixtapeNameStore
import com.example.androidmixtape.name.MixtapeNameSource
import com.example.androidmixtape.name.MixtapeNameStore
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MixtapeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateRequiresPermission() {
        val viewModel = viewModelWith(FakeRepository(emptyList()))

        assertEquals(LibraryStatus.PermissionRequired, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.message.contains("Allow audio access"))
    }

    @Test
    fun deniedPermissionShowsRecoverablePermissionState() {
        val viewModel = viewModelWith(FakeRepository(emptyList()))

        viewModel.onPermissionResult(false)

        assertEquals(LibraryStatus.PermissionRequired, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.message.contains("permission"))
    }

    @Test
    fun grantedPermissionWithEmptyRepositoryShowsEmptyLibrary() = runTest {
        val viewModel = viewModelWith(FakeRepository(emptyList()))

        viewModel.onPermissionResult(true)

        assertEquals(LibraryStatus.Empty, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.message.contains("No audio files"))
    }

    @Test
    fun repositoryFailureShowsErrorAndKeepsRefreshAvailable() = runTest {
        val viewModel = viewModelWith(FakeRepository(error = IllegalStateException("boom")))

        viewModel.onPermissionResult(true)

        assertEquals(LibraryStatus.Error, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.message.contains("boom"))
    }

    @Test
    fun grantedPermissionWithNonEmptyRepositoryOpensMixTapeListByDefault() = runTest {
        val tracks = numberedTracks(13)
        val viewModel = viewModelWith(FakeRepository(tracks))

        viewModel.onPermissionResult(true)

        val state = viewModel.uiState.value
        assertEquals(LibraryStatus.Ready, state.status)
        assertEquals(MixtapeScreen.MixTapes, state.screen)
        assertEquals(tracks, state.tracks)
        assertEquals(1, state.mixTapeGroups.size)
        assertEquals("Mix Tape 1", state.mixTapeGroups[0].name)
        assertEquals(tracks, state.mixTapeGroups[0].tracks)
        assertTrue(state.message.contains("mix tapes ready"))
    }

    @Test
    fun savedMixtapeNameIsShown() = runTest {
        val tracks = numberedTracks(13)
        val nameStore = InMemoryMixtapeNameStore(
            mapOf("0|1,2,3,4,5,6,7,8,9,10,11,12,13" to "Saved Road Songs"),
        )
        val viewModel = viewModelWith(FakeRepository(tracks), nameStore = nameStore)

        viewModel.onPermissionResult(true)

        assertEquals("Saved Road Songs", viewModel.uiState.value.mixTapeGroups[0].name)
    }

    @Test
    fun randomNamesComeFromTheBundledNameSourceWithoutRepeatingWhenEnoughNamesExist() = runTest {
        val names = listOf("brave radio sunrise", "XO golden echo offline", "cosmic postcards")
        val viewModel = viewModelWith(
            repository = FakeRepository(numberedTracks(113)),
            mixtapeRandom = Random(15),
            nameSource = fixedNameSource(names),
        )

        viewModel.onPermissionResult(true)

        val assignedNames = viewModel.uiState.value.mixTapeGroups.map { it.name }
        assertEquals(3, assignedNames.size)
        assertTrue(assignedNames.all { it in names })
        assertEquals(assignedNames.size, assignedNames.distinct().size)
    }

    @Test
    fun randomizeMixtapeNameAlwaysChoosesAnotherNameWhenAvailable() = runTest {
        val names = listOf("brave radio sunrise", "XO golden echo offline")
        val viewModel = viewModelWith(
            repository = FakeRepository(numberedTracks(13)),
            mixtapeRandom = Random(7),
            nameSource = fixedNameSource(names),
        )
        viewModel.onPermissionResult(true)
        val before = viewModel.uiState.value.mixTapeGroups.single()
        val stableKey = viewModel.uiState.value.mixtapeNameInfos.single().stableKey

        viewModel.regenerateMixtapeName(stableKey)

        val after = viewModel.uiState.value.mixTapeGroups.single()
        assertTrue(before.name in names)
        assertTrue(after.name in names)
        assertTrue(after.name != before.name)
    }

    @Test
    fun mixtapeSelectionUpdatesCurrentTrackAndProgressState() = runTest {
        val tracks = listOf(Track(7, "Seven", "Artist", 70_000, "content://seven"))
        val viewModel = viewModelWith(FakeRepository(tracks))
        viewModel.onPermissionResult(true)

        viewModel.selectMixTapeGroup(0)

        assertEquals(LibraryStatus.Ready, viewModel.uiState.value.status)
        assertEquals("Seven", viewModel.uiState.value.currentTrack?.title)
        assertEquals(70_000, viewModel.uiState.value.durationMs)
        assertTrue(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun automaticPlayerTransitionRefreshesNowPlayingUiStateWithoutManualNext() = runTest {
        val tracks = numberedTracks(3)
        val fakePlayer = FakePlayerEngine()
        val viewModel = viewModelWith(
            repository = FakeRepository(tracks),
            controller = MixtapeController(fakePlayer),
        )
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)

        fakePlayer.simulateAutomaticTransitionTo(1)

        val state = viewModel.uiState.value
        assertEquals(
            "Automatic playback completion must update the ViewModel currentIndex without calling next(), because Now Playing and the cassette cover read from uiState.",
            1,
            state.currentIndex,
        )
        assertEquals("Track 2", state.currentTrack?.title)
        assertTrue("Playback should remain active after the player reports the next track.", state.isPlaying)
        assertEquals("The ViewModel duration should follow the newly playing track.", 2_000L, state.durationMs)
        assertEquals(MixtapeScreen.NowPlaying, state.screen)
        assertEquals("Track 2", state.queueTracks[state.currentIndex].title)
        assertTrue("The existing Now Playing message should be preserved across automatic transitions.", state.message.contains("Playing Mix Tape 1"))
    }

    @Test
    fun carInitiatedPlaybackUpdatesPhoneNowPlayingQueueAndCurrentTrack() = runTest {
        val tracks = numberedTracks(60)
        val phoneMixtape = tracks.take(LP_SONGS_PER_MIXTAPE)
        val carMixtape = tracks.drop(LP_SONGS_PER_MIXTAPE)
        val fakePlayer = FakePlayerEngine()
        val viewModel = viewModelWith(
            repository = FakeRepository(tracks),
            controller = MixtapeController(fakePlayer),
        )
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)
        assertEquals(phoneMixtape, viewModel.uiState.value.queueTracks)

        fakePlayer.simulateExternalPlaybackSnapshot(
            tracks = carMixtape,
            currentIndex = 1,
            isPlaying = true,
            positionMs = 1_234L,
            durationMs = carMixtape[1].durationMs,
        )

        val state = viewModel.uiState.value
        assertEquals(
            "When the car starts a different mixtape, the phone queue must switch to that car-selected side instead of keeping the stale phone mixtape.",
            carMixtape,
            state.queueTracks,
        )
        assertEquals(1, state.currentIndex)
        assertEquals(carMixtape[1], state.currentTrack)
        assertTrue("Phone transport state must mirror the car-started playback state.", state.isPlaying)
        assertEquals(1_234L, state.positionMs)
        assertEquals(carMixtape[1].durationMs, state.durationMs)
        assertEquals(MixtapeScreen.NowPlaying, state.screen)
    }

    @Test
    fun coldLauncherReturnToActiveSessionOpensCurrentTapeWithoutReplacingQueue() = runTest {
        val tracks = numberedTracks(60)
        val activeTape = tracks.drop(LP_SONGS_PER_MIXTAPE)
        val player = FakePlayerEngine()
        val viewModel = viewModelWith(FakeRepository(tracks), MixtapeController(player))
        // A fresh Activity/ViewModel connects to a media session that kept playing while the UI was gone.
        player.simulateExternalPlaybackSnapshot(activeTape, 2, true, 1_234L, activeTape[2].durationMs)

        viewModel.onPermissionResult(true)

        val state = viewModel.uiState.value
        assertEquals(LibraryStatus.Ready, state.status)
        assertEquals(MixtapeScreen.NowPlaying, state.screen)
        assertEquals(activeTape, state.queueTracks)
        assertEquals(activeTape[2], state.currentTrack)
        assertEquals(1, state.currentMixtapeIndex)
        assertEquals(1_234L, state.positionMs)
        assertTrue(state.isPlaying)
        assertTrue("Startup must not send a replacement playlist to the already-playing session", player.loaded.isEmpty())
        assertEquals(null, player.playedIndex)
    }

    @Test
    fun coldLauncherReturnWithNoSessionStillShowsLibraryWithoutAutoplay() = runTest {
        val player = FakePlayerEngine()
        val viewModel = viewModelWith(FakeRepository(numberedTracks(3)), MixtapeController(player))

        viewModel.onPermissionResult(true)

        assertEquals(MixtapeScreen.MixTapes, viewModel.uiState.value.screen)
        assertFalse(viewModel.uiState.value.isPlaying)
        assertEquals(null, player.playedIndex)
    }

    @Test
    fun repeatedGrantedPermissionDoesNotReloadOrInterruptActivePlayback() = runTest {
        val tracks = numberedTracks(3)
        val repository = FakeRepository(tracks)
        val viewModel = viewModelWith(repository)
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)
        viewModel.next()

        viewModel.onPermissionResult(true)

        val state = viewModel.uiState.value
        assertEquals(
            "Activity recreation should not rescan MediaStore when permission is already granted and the library is ready.",
            1,
            repository.loadCount,
        )
        assertEquals("Orientation-style recreation should keep the playing track selected.", 1, state.currentIndex)
        assertEquals("Track 2", state.currentTrack?.title)
        assertTrue("Playback should remain active after a repeated startup permission check.", state.isPlaying)
    }

    @Test
    fun successfulDeleteFromDeviceRemovesTrackFromLibraryGroupsAndOpenQueue() = runTest {
        val tracks = numberedTracks(13)
        val repository = FakeRepository(tracks, deleteResult = DeleteTrackResult.Success)
        val viewModel = viewModelWith(repository)
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)

        viewModel.deleteTrackFromDevice(2L)

        val state = viewModel.uiState.value
        assertEquals(listOf(2L), repository.deletedTrackIds)
        assertFalse("Deleted track should leave the app-level library list immediately after confirmed device deletion.", state.tracks.any { it.id == 2L })
        assertFalse("Deleted track should be removed from every mixtape group.", state.mixTapeGroups.any { group -> group.tracks.any { it.id == 2L } })
        assertFalse("Deleted track should disappear from the currently open now-playing queue.", state.queueTracks.any { it.id == 2L })
        assertTrue("Controller currentIndex should remain valid after rebuilding the queue.", state.currentIndex in state.queueTracks.indices)
        assertTrue(state.message.contains("Deleted Track 2"))
    }

    @Test
    fun successfulDeleteFromDevicePreservesPlayingNonDeletedTrackPositionAndVisualIdentity() = runTest {
        val tracks = numberedTracks(13)
        val repository = FakeRepository(tracks, deleteResult = DeleteTrackResult.Success)
        val viewModel = viewModelWith(
            repository = repository,
            mixtapeRandom = Random(2373),
        )
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)
        viewModel.next()
        viewModel.seekTo(750L)
        val beforeDeleteVisuals = viewModel.uiState.value.currentMixtapeVisualProperties

        viewModel.deleteTrackFromDevice(4L)

        val state = viewModel.uiState.value
        assertEquals(listOf(4L), repository.deletedTrackIds)
        assertFalse("Deleted non-current track should disappear from the open queue.", state.queueTracks.any { it.id == 4L })
        assertEquals(
            "Deleting a different device track must keep the same song selected instead of resetting to the first track.",
            2L,
            state.currentTrack?.id,
        )
        assertTrue("Playback should continue after deleting a different queued track.", state.isPlaying)
        assertEquals("Playback position should not be rewound by the queue rebuild.", 750L, state.positionMs)
        assertEquals(
            "The open mixtape's visual identity, including handwriting font, must survive stable-key churn caused by deletion.",
            beforeDeleteVisuals,
            state.currentMixtapeVisualProperties,
        )
    }

    @Test
    fun confirmedScopedStorageDeletePreservesPlayingNonDeletedTrackAndPosition() = runTest {
        val tracks = numberedTracks(13)
        val repository = FakeRepository(
            tracks = tracks,
            deleteResult = DeleteTrackResult.RequiresUserAction(fakeIntentSender()),
        )
        val viewModel = viewModelWith(repository)
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)
        viewModel.next()
        viewModel.seekTo(900L)

        viewModel.deleteTrackFromDevice(4L)
        runCurrent()
        assertEquals("The delete should be pending until Android confirmation succeeds.", 13, viewModel.uiState.value.queueTracks.size)

        viewModel.confirmTrackDeletedFromDevice()

        val state = viewModel.uiState.value
        assertFalse("Confirmed deleted track should disappear from the open queue.", state.queueTracks.any { it.id == 4L })
        assertEquals(
            "After Android delete confirmation, the current song should still be the pre-delete song.",
            2L,
            state.currentTrack?.id,
        )
        assertTrue("Confirmed delete should not pause a different currently playing track.", state.isPlaying)
        assertEquals("Confirmed delete should not rewind a different currently playing track.", 900L, state.positionMs)
    }

    @Test
    fun deletingCurrentlyPlayingTrackStopsWithValidRemainingQueueState() = runTest {
        val tracks = numberedTracks(4)
        val repository = FakeRepository(tracks, deleteResult = DeleteTrackResult.Success)
        val viewModel = viewModelWith(repository)
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)
        viewModel.next()

        viewModel.deleteTrackFromDevice(2L)

        val state = viewModel.uiState.value
        assertFalse("The deleted current track must leave the queue.", state.queueTracks.any { it.id == 2L })
        assertFalse("The player cannot continue playing a file that was deleted from the device.", state.isPlaying)
        assertEquals("Deleting the playing file should leave progress at the start of a valid remaining item.", 0L, state.positionMs)
        assertTrue("Current index should still point at a valid remaining track when the queue is non-empty.", state.currentIndex in state.queueTracks.indices)
    }

    @Test
    fun failedDeleteFromDeviceLeavesLibraryGroupsAndOpenQueueUnchanged() = runTest {
        val tracks = numberedTracks(13)
        val repository = FakeRepository(tracks, deleteResult = DeleteTrackResult.Failure("permission denied"))
        val viewModel = viewModelWith(repository)
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)
        val before = viewModel.uiState.value

        viewModel.deleteTrackFromDevice(2L)

        val state = viewModel.uiState.value
        assertEquals(listOf(2L), repository.deletedTrackIds)
        assertEquals("Failed delete should not hide the track from the library.", before.tracks, state.tracks)
        assertEquals("Failed delete should not rebuild mixtape membership.", before.mixTapeGroups.map { it.tracks }, state.mixTapeGroups.map { it.tracks })
        assertEquals("Failed delete should leave the current queue intact.", before.queueTracks, state.queueTracks)
        assertTrue(state.message.contains("Could not delete Track 2"))
        assertTrue(state.message.contains("permission denied"))
    }

    @Test
    fun addingFilenameExclusionPatternFiltersLibraryGroupsAndOpenQueue() = runTest {
        val tracks = exclusionTestTracks()
        val viewModel = viewModelWith(FakeRepository(tracks))
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)

        invokeStringAction(viewModel, "addFilenameExclusionPattern", "voice memo")

        val state = viewModel.uiState.value
        assertFalse("Excluded files should leave the app-level library list immediately.", state.tracks.any { it.id == 2L })
        assertFalse("Excluded files should be removed from every mix tape group.", state.mixTapeGroups.any { group -> group.tracks.any { it.id == 2L } })
        assertFalse("Excluded files should disappear from the currently open now-playing queue.", state.queueTracks.any { it.id == 2L })
        assertTrue("Non-matching tracks should stay available.", state.tracks.map { it.id }.containsAll(listOf(1L, 3L)))
    }

    @Test
    fun removingFilenameExclusionPatternMakesRawLibraryTracksEligibleAgain() = runTest {
        val tracks = exclusionTestTracks()
        val viewModel = viewModelWith(FakeRepository(tracks))
        viewModel.onPermissionResult(true)

        invokeStringAction(viewModel, "addFilenameExclusionPattern", "voice memo")
        assertFalse(viewModel.uiState.value.tracks.any { it.id == 2L })

        invokeStringAction(viewModel, "removeFilenameExclusionPattern", "voice memo")

        val state = viewModel.uiState.value
        assertEquals("Removing a pattern should rebuild from the raw, unfiltered library.", tracks.map { it.id }, state.tracks.map { it.id })
        assertTrue("Restored tracks should be eligible for mix tape groups again.", state.mixTapeGroups.any { group -> group.tracks.any { it.id == 2L } })
    }

    @Test
    fun blankAndDuplicateFilenameExclusionPatternsAreIgnored() = runTest {
        val viewModel = viewModelWith(FakeRepository(exclusionTestTracks()))
        viewModel.onPermissionResult(true)

        invokeStringAction(viewModel, "addFilenameExclusionPattern", "   ")
        invokeStringAction(viewModel, "addFilenameExclusionPattern", "voice memo")
        invokeStringAction(viewModel, "addFilenameExclusionPattern", " voice memo ")

        assertEquals(
            "Blank patterns and trim-equivalent duplicates should not be saved.",
            listOf("voice memo"),
            filenameExclusionPatterns(viewModel),
        )
    }

    @Test
    fun selectedMixtapeIndexIsExposedForNowPlayingCenteringAndSurvivesEject() = runTest {
        val viewModel = viewModelWith(FakeRepository(numberedTracks(113)))
        viewModel.onPermissionResult(true)

        viewModel.selectMixTapeGroup(2)

        assertEquals(
            "Now Playing needs the selected mixtape's library index so the Mix tapes pane can center and highlight it without recomputing private stable keys in UI code.",
            2,
            currentMixtapeIndex(viewModel.uiState.value),
        )

        invokeEject(viewModel)

        assertEquals(MixtapeScreen.MixTapes, viewModel.uiState.value.screen)
        assertEquals(
            "Eject should preserve the selected mixtape index so the shared Mix Tapes list can keep the user at the browsed/current cassette instead of losing context.",
            2,
            currentMixtapeIndex(viewModel.uiState.value),
        )
    }

    @Test
    fun ejectStopsCurrentTrackAndReturnsToMixTapes() = runTest {
        val tracks = numberedTracks(3)
        val viewModel = viewModelWith(FakeRepository(tracks))
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)
        viewModel.seekTo(500L)

        invokeEject(viewModel)

        val state = viewModel.uiState.value
        assertEquals(LibraryStatus.Ready, state.status)
        assertEquals(MixtapeScreen.MixTapes, state.screen)
        assertFalse("Eject should stop playback before leaving the now-playing screen.", state.isPlaying)
        assertEquals("Eject should rewind the current track to the beginning.", 0L, state.positionMs)
        assertTrue(state.message.contains("mix tapes ready"))
    }

    private fun invokeStringAction(viewModel: MixtapeViewModel, methodName: String, value: String) {
        val method = viewModel.javaClass.methods.singleOrNull { method ->
            method.name == methodName && method.parameterTypes.toList() == listOf(String::class.java)
        }
        assertNotNull("MixtapeViewModel must expose $methodName(pattern: String).", method)
        method!!.invoke(viewModel, value)
    }

    private fun filenameExclusionPatterns(viewModel: MixtapeViewModel): List<String> {
        val settings = viewModel.uiState.value.javaClass.methods.singleOrNull { method ->
            method.name == "getMixtapeExclusionSettings" && method.parameterCount == 0
        }?.invoke(viewModel.uiState.value)
        assertNotNull("MixtapeUiState must expose mixtapeExclusionSettings.", settings)
        @Suppress("UNCHECKED_CAST")
        return settings!!.javaClass.methods.single { method ->
            method.name == "getFilenamePatterns" && method.parameterCount == 0
        }.invoke(settings) as List<String>
    }

    private fun currentMixtapeIndex(uiState: MixtapeUiState): Int {
        val getter = uiState.javaClass.methods.singleOrNull { method ->
            method.name == "getCurrentMixtapeIndex" && method.parameterCount == 0
        }
        assertNotNull("MixtapeUiState must expose currentMixtapeIndex: Int for Now Playing list centering/highlighting.", getter)
        return getter!!.invoke(uiState) as Int
    }

    private fun invokeEject(viewModel: MixtapeViewModel) {
        val eject = viewModel.javaClass.methods.singleOrNull { method ->
            method.name == "eject" && method.parameterCount == 0
        }
        assertNotNull("MixtapeViewModel must expose a no-argument eject() action.", eject)
        eject!!.invoke(viewModel)
    }

    private fun viewModelWith(
        repository: AudioRepository,
        controller: MixtapeController = MixtapeController(FakePlayerEngine()),
        mixtapeRandom: Random = Random.Default,
        nameSource: MixtapeNameSource = MixtapeNameSource.Empty,
        nameStore: MixtapeNameStore = InMemoryMixtapeNameStore(),
        visualPropertiesStore: MixtapeVisualPropertiesStore = InMemoryMixtapeVisualPropertiesStore(),
    ): MixtapeViewModel = MixtapeViewModel(
        repository = repository,
        controller = controller,
        mixtapeRandom = mixtapeRandom,
        nameSource = nameSource,
        nameStore = nameStore,
        visualPropertiesStore = visualPropertiesStore,
    )

    private fun fixedNameSource(names: List<String>): MixtapeNameSource = object : MixtapeNameSource {
        override fun names(): List<String> = names
    }

    private fun fakeIntentSender(): IntentSender {
        val unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null)
        val allocateInstance = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
        return allocateInstance.invoke(unsafe, IntentSender::class.java) as IntentSender
    }
}

private class FakeRepository(
    private val tracks: List<Track> = emptyList(),
    private val error: Throwable? = null,
    private val deleteResult: DeleteTrackResult = DeleteTrackResult.Failure("Delete from device is not available for this audio source"),
) : AudioRepository {
    var loadCount: Int = 0
        private set
    val deletedTrackIds: MutableList<Long> = mutableListOf()

    override suspend fun loadTracks(): List<Track> {
        loadCount += 1
        error?.let { throw it }
        return tracks
    }

    override suspend fun deleteTrack(track: Track): DeleteTrackResult {
        deletedTrackIds += track.id
        return deleteResult
    }
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

private fun exclusionTestTracks(): List<Track> = listOf(
    Track(
        id = 1L,
        title = "Keep One",
        artist = "Artist 1",
        durationMs = 1_000L,
        uri = "content://track/1",
        displayName = "keep-one.mp3",
    ),
    Track(
        id = 2L,
        title = "Field Recording",
        artist = "Artist 2",
        durationMs = 2_000L,
        uri = "content://track/2",
        displayName = "2026 voice memo.m4a",
    ),
    Track(
        id = 3L,
        title = "Keep Two",
        artist = "Artist 3",
        durationMs = 3_000L,
        uri = "content://track/3",
        displayName = "keep-two.mp3",
    ),
)
