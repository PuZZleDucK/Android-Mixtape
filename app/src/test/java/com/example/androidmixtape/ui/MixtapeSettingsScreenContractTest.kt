package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeSettingsScreenContractTest {
    @Test
    fun settingsScreenOffersSongsPerTapeAndArtistGroupingControls() {
        val source = mixtapeAppSource()

        assertTrue(
            "MixtapeApp should expose a songs-per-mixtape change callback for MainActivity/ViewModel wiring.",
            source.contains("onSongsPerMixTapeChange"),
        )
        assertTrue(
            "MixtapeApp should expose an artist grouping change callback for MainActivity/ViewModel wiring.",
            source.contains("onArtistGroupingChange"),
        )
        assertTrue(
            "Settings should label the current songs-per-mixtape control.",
            source.contains("Songs per mix tape"),
        )
        assertTrue(
            "Settings should include a no-grouping artist option.",
            source.contains("No artist grouping"),
        )
        assertTrue(
            "Settings should include a keep-artists-together option.",
            source.contains("Keep artist together"),
        )
        assertTrue(
            "Settings should include a triplets-across-tapes artist option.",
            source.contains("Artist triplets on different tapes"),
        )
    }

    private fun mixtapeAppSource(): String {
        val path = listOf(
            File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("app/src/main/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("src/main/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        ).firstOrNull { it.exists() }
        assertTrue("Expected to find MixtapeApp.kt from ${System.getProperty("user.dir")}", path != null)
        return path!!.readText()
    }
}
