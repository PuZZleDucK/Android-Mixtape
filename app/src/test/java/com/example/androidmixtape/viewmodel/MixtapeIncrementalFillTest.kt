package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.Track
import org.junit.Assert.assertEquals
import org.junit.Test

/** New MediaStore tracks should use the final tape's free slots without moving older songs. */
class MixtapeIncrementalFillTest {
    private val settings = MixtapeSettings(songsPerMixTape = 3, artistGrouping = ArtistGrouping.NoGrouping)
    private fun track(id: Long) = Track(id, "Track $id", "Artist $id", 180_000, "content://track/$id")
    private fun ids(groups: List<MixTapeGroup>) = groups.map { group -> group.tracks.map(Track::id) }

    @Test fun newTracksFillPartialLastTapeWithoutChangingItsIdentity() {
        val store = InMemoryMixtapeMembershipStore()
        val first = resolveMixtapeGroups((1L..5L).map(::track), settings, store)
        val updated = resolveMixtapeGroups((1L..6L).map(::track), settings, store)

        assertEquals(listOf(listOf(1L, 2L, 3L), listOf(4L, 5L, 6L)), ids(updated))
        assertEquals(first.map(MixTapeGroup::stableKey), updated.map(MixTapeGroup::stableKey))
        assertEquals(listOf(0, 3), updated.map(MixTapeGroup::startIndex))
        assertEquals(updated.map(MixTapeGroup::stableKey), resolveMixtapeGroups((1L..6L).map(::track), settings, store).map(MixTapeGroup::stableKey))
    }

    @Test fun overflowFillsLastTapeBeforeCreatingExactlyTheNeededNewTapes() {
        val store = InMemoryMixtapeMembershipStore()
        resolveMixtapeGroups((1L..5L).map(::track), settings, store)
        val updated = resolveMixtapeGroups((1L..10L).map(::track), settings, store)

        assertEquals(listOf(listOf(1L, 2L, 3L), listOf(4L, 5L, 6L), listOf(7L, 8L, 9L), listOf(10L)), ids(updated))
        assertEquals(10, store.load()!!.knownTrackIds.size)
    }

    @Test fun fullLastTapeCreatesNewTapeAndDoesNotRefillEarlierEditedTape() {
        val store = InMemoryMixtapeMembershipStore()
        val original = resolveMixtapeGroups((1L..6L).map(::track), settings, store)
        store.removeTracks(setOf(2L), onlyMixtapeKey = original.first().stableKey)
        val updated = resolveMixtapeGroups(((1L..6L) + 7L).map(::track), settings, store)

        assertEquals(listOf(listOf(1L, 3L), listOf(4L, 5L, 6L), listOf(7L)), ids(updated))
        assertEquals(original.map(MixTapeGroup::stableKey), updated.take(2).map(MixTapeGroup::stableKey))
    }

    @Test fun deliberatelyRemovedTrackStaysOutButNewTrackFillsFinalTape() {
        val store = InMemoryMixtapeMembershipStore()
        val original = resolveMixtapeGroups((1L..5L).map(::track), settings, store)
        store.removeTracks(setOf(5L), onlyMixtapeKey = original.last().stableKey)
        val updated = resolveMixtapeGroups((1L..6L).map(::track), settings, store)

        assertEquals(listOf(listOf(1L, 2L, 3L), listOf(4L, 6L)), ids(updated))
        assertEquals(original.last().stableKey, updated.last().stableKey)
    }

    @Test fun missingKnownTrackDoesNotBecomeNewAndFinalTapeUsesActualAvailableCapacity() {
        val store = InMemoryMixtapeMembershipStore()
        val original = resolveMixtapeGroups((1L..5L).map(::track), settings, store)
        val updated = resolveMixtapeGroups(listOf(1L, 2L, 3L, 4L, 6L).map(::track), settings, store)

        assertEquals(listOf(listOf(1L, 2L, 3L), listOf(4L, 6L)), ids(updated))
        assertEquals(original.last().stableKey, updated.last().stableKey)
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L, 6L), store.load()!!.knownTrackIds)
    }
}
