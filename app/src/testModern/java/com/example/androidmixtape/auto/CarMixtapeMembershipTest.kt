package com.example.androidmixtape.auto

import com.example.androidmixtape.car.CarMixtapeCatalog
import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.name.InMemoryMixtapeNameStore
import com.example.androidmixtape.name.MixtapeNameSource
import com.example.androidmixtape.viewmodel.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CarMixtapeMembershipTest {
    @Test fun carUsesPhoneMembershipAndNamesRatherThanRegroupingTheLibrary() {
        val tracks = (1..9).map { Track(it.toLong(), "Track $it", "Artist", 180_000, "content://track/$it") }
        val settings = InMemoryMixtapeSettingsStore(MixtapeSettings(3, ArtistGrouping.NoGrouping))
        val membership = InMemoryMixtapeMembershipStore()
        val names = InMemoryMixtapeNameStore()
        val original = resolveMixtapeGroups(tracks, settings.settings(), membership)
        original.forEachIndexed { index, group -> names.saveName(group.stableKey, "Saved tape $index") }
        membership.removeTracks(setOf(2), onlyMixtapeKey = original.first().stableKey)
        val catalog = CarMixtapeCatalog(
            repository = object : AudioRepository { override suspend fun loadTracks() = tracks },
            settingsStore = settings,
            nameSource = MixtapeNameSource.Empty,
            nameStore = names,
            visualPropertiesStore = InMemoryMixtapeVisualPropertiesStore(),
            membershipStore = membership,
        )
        val carTapes = catalog.load().mixtapes
        assertEquals(original.map { it.stableKey }, carTapes.map { it.stableKey })
        assertEquals(listOf("Saved tape 0", "Saved tape 1", "Saved tape 2"), carTapes.map { it.displayName })
        assertEquals(listOf(1L, 3L), carTapes.first().tracks.map { it.source.id })
        assertEquals(original.drop(1).map { it.tracks }, carTapes.drop(1).map { tape -> tape.tracks.map { it.source } })
        assertFalse(carTapes.any { tape -> tape.tracks.any { it.source.id == 2L } })
    }
}
