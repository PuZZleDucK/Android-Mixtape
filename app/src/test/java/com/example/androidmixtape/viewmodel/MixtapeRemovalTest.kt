package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.DeleteTrackResult
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.name.InMemoryMixtapeNameStore
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MixtapeRemovalTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private val repository = RemovalRepository()
    private val names = InMemoryMixtapeNameStore()
    private val visuals = InMemoryMixtapeVisualPropertiesStore()
    private val membership = InMemoryMixtapeMembershipStore()
    private val settings = InMemoryMixtapeSettingsStore(MixtapeSettings(3, ArtistGrouping.NoGrouping))

    private fun openPlayer(): MixtapeViewModel = MixtapeViewModel(
        repository = repository,
        controller = MixtapeController(FakePlayerEngine()),
        mixtapeRandom = Random(7),
        nameStore = names,
        visualPropertiesStore = visuals,
        settingsStore = settings,
        membershipStore = membership,
    ).also { it.onPermissionResult(true) }

    private fun nameTapes(vm: MixtapeViewModel) {
        vm.uiState.value.mixtapeNameInfos.forEachIndexed { index, tape ->
            vm.editMixtapeName(tape.stableKey, "Saved tape $index")
        }
    }

    private fun assertOnlyTrackRemoved(before: MixtapeUiState, after: MixtapeUiState, tapeIndex: Int, id: Long) {
        assertEquals("Tape count must not change, even when a tape becomes empty", before.mixTapeGroups.size, after.mixTapeGroups.size)
        before.mixTapeGroups.forEachIndexed { index, tape ->
            val expected = if (index == tapeIndex) tape.tracks.filterNot { it.id == id } else tape.tracks
            assertEquals("Only the selected tape may lose the selected track, no swaps or refilling", expected, after.mixTapeGroups[index].tracks)
            assertEquals("Tape $index name changed", tape.name, after.mixTapeGroups[index].name)
            assertEquals("Tape $index appearance changed", tape.visualProperties, after.mixTapeGroups[index].visualProperties)
        }
        assertEquals("Tape identities must not depend on remaining track positions", before.mixtapeNameInfos, after.mixtapeNameInfos)
    }

    @Test fun removingTrackDoesNotSwapRefillRenameOrInterruptAnotherSong() = runTest {
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        vm.seekTo(1_234)
        val before = vm.uiState.value
        vm.removeTrackFromCurrentMixtape(2)
        val after = vm.uiState.value
        assertOnlyTrackRemoved(before, after, 0, 2)
        assertEquals(before.tracks, after.tracks)
        assertEquals(before.currentTrack, after.currentTrack)
        assertEquals(before.positionMs, after.positionMs)
        assertTrue(after.isPlaying)
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test fun deletionFromDeviceDoesNotRepackOrRenameLaterTapes() = runTest {
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        val before = vm.uiState.value
        vm.deleteTrackFromDevice(2)
        assertOnlyTrackRemoved(before, vm.uiState.value, 0, 2)
        assertEquals(listOf(2L), repository.deletedIds)
        assertFalse(vm.uiState.value.tracks.any { it.id == 2L })
    }

    @Test fun removalWorksWithOneTapeAndPreservesItsNameWhenEmpty() = runTest {
        repository.tracks = repository.tracks.take(1)
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        val before = vm.uiState.value
        vm.removeTrackFromCurrentMixtape(1)
        assertOnlyTrackRemoved(before, vm.uiState.value, 0, 1)
        assertTrue(vm.uiState.value.queueTracks.isEmpty())
        assertFalse(vm.uiState.value.isPlaying)
        assertEquals(MixtapeScreen.MixTapes, vm.uiState.value.screen)
    }

    @Test fun removalSurvivesRefreshReopeningAndLibraryReordering() = runTest {
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        val before = vm.uiState.value
        vm.removeTrackFromCurrentMixtape(2)
        repository.tracks = repository.tracks.reversed()
        vm.refresh()
        assertOnlyTrackRemoved(before, vm.uiState.value, 0, 2)
        assertOnlyTrackRemoved(before, openPlayer().uiState.value, 0, 2)
    }

    @Test fun deviceDeletionSurvivesReopeningWithoutRenamingAnyTape() = runTest {
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        val before = vm.uiState.value
        vm.deleteTrackFromDevice(2)
        assertOnlyTrackRemoved(before, openPlayer().uiState.value, 0, 2)
    }

    @Test fun emptyTapeKeepsItsIdentityAndDoesNotInheritTheNextTape() = runTest {
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        val original = vm.uiState.value
        for (id in 1L..3L) {
            val before = vm.uiState.value
            vm.removeTrackFromCurrentMixtape(id)
            assertOnlyTrackRemoved(before, vm.uiState.value, 0, id)
        }
        val reopened = openPlayer().uiState.value
        assertEquals(original.mixtapeNameInfos, reopened.mixtapeNameInfos)
        assertTrue(reopened.mixTapeGroups.first().tracks.isEmpty())
        assertEquals(original.mixTapeGroups.drop(1).map { it.tracks }, reopened.mixTapeGroups.drop(1).map { it.tracks })
    }

    @Test fun newLibraryTracksGetNewTapesRatherThanRefillingEditedTapes() = runTest {
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        vm.removeTrackFromCurrentMixtape(2)
        val edited = vm.uiState.value
        val newTrack = Track(10, "New song", "New artist", 180_000, "content://track/10")
        repository.tracks = repository.tracks + newTrack
        vm.refresh()
        assertEquals(edited.mixTapeGroups, vm.uiState.value.mixTapeGroups.take(3))
        assertEquals(listOf(newTrack), vm.uiState.value.mixTapeGroups.last().tracks)
        assertFalse(vm.uiState.value.mixTapeGroups.any { tape -> tape.tracks.any { it.id == 2L } })
    }

    @Test fun removingSharedTrackOnlyChangesTheSelectedTape() = runTest {
        val vm = openPlayer()
        nameTapes(vm)
        val stored = membership.load()!!
        membership.save(stored.copy(tapes = stored.tapes.mapIndexed { index, tape ->
            if (index == 1) tape.copy(trackIds = tape.trackIds + 2L) else tape
        }))
        vm.refresh()
        vm.selectMixTapeGroup(0)
        val before = vm.uiState.value
        vm.removeTrackFromCurrentMixtape(2)
        assertOnlyTrackRemoved(before, vm.uiState.value, 0, 2)
    }

    @Test fun removingCurrentSongAdvancesWithinSameTapeWithoutRenaming() = runTest {
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        val before = vm.uiState.value
        vm.removeTrackFromCurrentMixtape(1)
        assertOnlyTrackRemoved(before, vm.uiState.value, 0, 1)
        assertEquals(2L, vm.uiState.value.currentTrack?.id)
        assertTrue(vm.uiState.value.isPlaying)
    }

    @Test fun legacyNameKeysAreRetainedOnFirstMembershipSave() = runTest {
        names.saveName("0|1,2,3", "Existing user name")
        val vm = openPlayer()
        assertEquals("Existing user name", vm.uiState.value.mixTapeGroups.first().name)
        vm.selectMixTapeGroup(0)
        vm.removeTrackFromCurrentMixtape(2)
        assertEquals("Existing user name", openPlayer().uiState.value.mixTapeGroups.first().name)
    }

    @Test fun exclusionToggleDoesNotRestoreAnExplicitlyRemovedTrack() = runTest {
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        vm.removeTrackFromCurrentMixtape(2)
        val before = vm.uiState.value
        vm.addFilenameExclusionPattern("Track 1")
        vm.removeFilenameExclusionPattern("Track 1")
        assertEquals(before.mixTapeGroups, vm.uiState.value.mixTapeGroups)
    }

    @Test fun androidConfirmationKeepsEveryTapeIdentityEvenAfterSwitchingTapes() = runTest {
        val unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null)
        val sender = unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
            .invoke(unsafe, android.content.IntentSender::class.java) as android.content.IntentSender
        repository.deleteResult = DeleteTrackResult.RequiresUserAction(sender)
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        val before = vm.uiState.value
        val saved = membership.load()
        vm.deleteTrackFromDevice(2)
        assertEquals(before.mixTapeGroups, vm.uiState.value.mixTapeGroups)
        assertEquals(saved, membership.load())
        vm.selectMixTapeGroup(1)
        vm.seekTo(1_234)
        repository.tracks = repository.tracks.filterNot { it.id == 2L }
        vm.confirmTrackDeletedFromDevice()
        assertOnlyTrackRemoved(before, vm.uiState.value, 0, 2)
        assertEquals(before.mixTapeGroups[1].tracks, vm.uiState.value.queueTracks)
        assertEquals(1_234L, vm.uiState.value.positionMs)
        assertTrue(vm.uiState.value.isPlaying)
        assertOnlyTrackRemoved(before, openPlayer().uiState.value, 0, 2)
    }

    @Test fun failedDeletionDoesNotChangeSavedMembershipOrNames() = runTest {
        val vm = openPlayer()
        nameTapes(vm)
        vm.selectMixTapeGroup(0)
        val before = vm.uiState.value
        val saved = membership.load()
        repository.deleteResult = DeleteTrackResult.Failure("Permission denied")
        vm.deleteTrackFromDevice(2)
        assertEquals(saved, membership.load())
        assertEquals(before.mixTapeGroups, vm.uiState.value.mixTapeGroups)
        assertEquals(before.mixTapeGroups, openPlayer().uiState.value.mixTapeGroups)
    }

    @Test fun removalWithoutAnOpenTapeDoesNothing() = runTest {
        val vm = openPlayer()
        val before = vm.uiState.value
        vm.removeTrackFromCurrentMixtape(2)
        assertEquals(before, vm.uiState.value)
        vm.selectMixTapeGroup(0)
        val selected = vm.uiState.value
        vm.removeTrackFromCurrentMixtape(5)
        assertEquals(selected, vm.uiState.value)
    }
}

private class RemovalRepository : AudioRepository {
    var tracks = (1..9).map { Track(it.toLong(), "Track $it", "Artist $it", 180_000, "content://track/$it") }
    val deletedIds = mutableListOf<Long>()
    var deleteResult: DeleteTrackResult = DeleteTrackResult.Success
    override suspend fun loadTracks() = tracks
    override suspend fun deleteTrack(track: Track): DeleteTrackResult {
        deletedIds += track.id
        if (deleteResult == DeleteTrackResult.Success) tracks = tracks.filterNot { it.id == track.id }
        return deleteResult
    }
}
