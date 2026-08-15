package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackListHorizontalSpaceContractTest {
    @Test
    fun trackListsGiveNamesTheFullAvailableWidthWithoutNumbersOrLeftIndent() {
        val source = mixtapeAppSource()
        val mainTrackList = functionBody(source, "private fun CassetteCoverTrackList(")
        val currentPreview = functionBody(source, "private fun CurrentTrackPreview(")
        val fontPreview = functionBody(source, "private fun HandwritingFontContextPreview(")

        listOf(mainTrackList, currentPreview, fontPreview).forEach { trackList ->
            assertFalse(trackList.contains("${'$'}{index + 1}."))
            assertFalse(trackList.contains("padding(start = 36.dp)"))
            assertFalse(trackList.contains("padding(start = 54.dp"))
            assertFalse(trackList.contains("sleevePaper.margin"))
        }
        assertTrue(mainTrackList.contains("${'$'}{track.title} — ${'$'}{track.artist}"))
        assertTrue(currentPreview.contains("padding(horizontal = 18.dp)"))
    }

    private fun functionBody(source: String, marker: String): String {
        val start = source.indexOf(marker)
        require(start >= 0) { "Missing $marker" }
        var depth = 0
        var opened = false
        for (index in start until source.length) {
            when (source[index]) {
                '{' -> { depth++; opened = true }
                '}' -> if (opened && --depth == 0) return source.substring(start, index + 1)
            }
        }
        error("Unclosed $marker")
    }

    private fun mixtapeAppSource(): String = listOf(
        File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
    ).first { it.exists() }.readText()
}
