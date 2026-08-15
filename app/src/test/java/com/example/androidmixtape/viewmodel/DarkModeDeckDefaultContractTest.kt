package com.example.androidmixtape.viewmodel

import android.content.res.Configuration
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DarkModeDeckDefaultContractTest {
    @Test
    fun darkModeDefaultsToBlackoutPortable() {
        assertEquals(
            DeckTheme.BlackoutPortable,
            initialDeckThemeForUiMode(Configuration.UI_MODE_NIGHT_YES),
        )
    }

    @Test
    fun lightAndUnspecifiedModesDefaultToSilverface() {
        assertEquals(
            DeckTheme.SilverfaceHiFi,
            initialDeckThemeForUiMode(Configuration.UI_MODE_NIGHT_NO),
        )
        assertEquals(
            DeckTheme.SilverfaceHiFi,
            initialDeckThemeForUiMode(Configuration.UI_MODE_NIGHT_UNDEFINED),
        )
    }

    @Test
    fun firstDetectedDefaultIsPersisted() {
        val source = File(
            findProjectRoot(File(requireNotNull(System.getProperty("user.dir")))),
            "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt",
        ).readText()
        assertTrue(source.contains("putString(KEY_DECK, initialTheme.name).commit()"))
    }

    private fun findProjectRoot(start: File): File {
        var current: File? = start.absoluteFile
        while (current != null) {
            if (File(current, "settings.gradle.kts").isFile) return current
            current = current.parentFile
        }
        error("Could not find project root from ${start.absolutePath}")
    }
}
