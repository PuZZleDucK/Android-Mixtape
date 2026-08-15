package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackListFullNameHorizontalScrollContractTest {
    @Test
    fun nowPlayingTrackListUsesSharedHorizontalScrollWithoutReplacingVerticalScroll() {
        val body = cassetteCoverTrackListBody()

        assertTrue(
            "The Now Playing track-list should own one remembered horizontal ScrollState shared by every row.",
            body.contains("val contentHorizontalScrollState = rememberScrollState()"),
        )
        assertTrue(
            "The track-list paper should pan horizontally when scrollContent is enabled.",
            Regex("horizontalScroll\\(\\s*contentHorizontalScrollState").containsMatchIn(body),
        )
        assertTrue(
            "Horizontal panning must retain the existing independent vertical track-list scroll state.",
            Regex("verticalScroll\\(\\s*contentScrollState").containsMatchIn(body),
        )
        assertTrue(
            "Opening a different track set should return the shared horizontal viewport to its left edge.",
            Regex("LaunchedEffect\\(tracks\\)[\\s\\S]*contentHorizontalScrollState\\.scrollTo\\(0\\)")
                .containsMatchIn(body),
        )
    }

    @Test
    fun fullRowTextDeterminesFinitePaperWidthAndRemainsAccessible() {
        val body = cassetteCoverTrackListBody()
        val rowRenderer = rowRenderer(body)

        assertTrue(
            "Canvas text needs a finite measured content width rather than the viewport width.",
            body.contains("BoxWithConstraints") && body.contains("rememberTextMeasurer()"),
        )
        assertTrue(
            "The widest complete rowText should participate in text measurement.",
            Regex("measure\\([\\s\\S]{0,400}(text\\s*=\\s*rowText|rowText)")
                .containsMatchIn(body),
        )
        assertTrue(
            "The measured paper width should never be narrower than the finite viewport.",
            body.contains("maxWidth") &&
                (body.contains("maxOf(") || body.contains(".coerceAtLeast(")),
        )
        assertFalse(
            "The Canvas row must not remain constrained to fillMaxWidth(), which drops tokens beyond the viewport.",
            rowRenderer.contains(".fillMaxWidth()"),
        )
        assertTrue(
            "Accessibility should expose the same complete row string, including title, artist, and duration.",
            rowRenderer.contains("contentDescription = rowText") ||
                rowRenderer.contains("contentDescription = \"Cassette cover track ${'$'}rowText\""),
        )
    }

    private fun rowRenderer(body: String): String {
        val rowTextStart = body.indexOf("val rowText =")
        assertTrue("Expected a complete rowText in CassetteCoverTrackList.", rowTextStart >= 0)
        val rendererStart = body.indexOf("JitteredHandwritingText(", rowTextStart)
        assertTrue("Expected a row JitteredHandwritingText renderer.", rendererStart >= 0)
        val menuStart = body.indexOf("DropdownMenu(", rendererStart)
        assertTrue("Expected the track action menu after the row renderer.", menuStart > rendererStart)
        return body.substring(rendererStart, menuStart)
    }

    private fun cassetteCoverTrackListBody(): String {
        val source = listOf(
            File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        ).firstOrNull { it.exists() }
        assertTrue("Expected to find the modern MixtapeApp.kt source.", source != null)

        val text = source!!.readText().replace("\r\n", "\n")
        val startMarker = "private fun CassetteCoverTrackList("
        val endMarker = "\n\n\nprivate fun tapeNameFor("
        assertTrue("Expected CassetteCoverTrackList in MixtapeApp.kt.", text.contains(startMarker))
        assertTrue("Expected tapeNameFor after CassetteCoverTrackList.", text.contains(endMarker))
        return text.substringAfter(startMarker).substringBefore(endMarker)
    }
}
