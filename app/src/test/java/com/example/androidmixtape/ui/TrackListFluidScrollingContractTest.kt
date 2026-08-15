package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackListFluidScrollingContractTest {
    @Test
    fun scrollableTrackListUsesOneTwoDimensionalDragOwner() {
        val body = cassetteCoverTrackListBody()

        assertTrue(
            "The scrollable track-list viewport should own a draggable2D gesture so one drag can update x and y.",
            body.contains("rememberDraggable2DState") && body.contains(".draggable2D("),
        )
        assertTrue(
            "The 2D drag callback should dispatch horizontal and vertical components to their independent ScrollStates.",
            Regex(
                "rememberDraggable2DState[\\s\\S]{0,1200}" +
                    "contentHorizontalScrollState\\.dispatchRawDelta\\([\\s\\S]{0,500}" +
                    "contentScrollState\\.dispatchRawDelta\\(",
            ).containsMatchIn(body),
        )
    }

    @Test
    fun oneAxisScrollModifiersOnlyLayOutAndClampContent() {
        val body = cassetteCoverTrackListBody()

        assertTrue(
            "HorizontalScroll must remain for finite paper layout/clamping but its competing touch recognizer must be disabled.",
            Regex("horizontalScroll\\(\\s*contentHorizontalScrollState\\s*,[\\s\\S]{0,160}enabled\\s*=\\s*false")
                .containsMatchIn(body),
        )
        assertTrue(
            "VerticalScroll must remain for layout, clamping, and automatic centering but its competing touch recognizer must be disabled.",
            Regex("verticalScroll\\(\\s*contentScrollState\\s*,[\\s\\S]{0,160}enabled\\s*=\\s*false")
                .containsMatchIn(body),
        )
        assertFalse(
            "Do not restore two independently user-enabled one-axis drag recognizers.",
            body.contains("horizontalScroll(contentHorizontalScrollState)") ||
                body.contains("verticalScroll(contentScrollState)"),
        )
    }

    @Test
    fun expensiveTextGeometryIsPreparedOutsideCanvasDrawsAndRememberedForTheTrackSet() {
        val renderer = jitteredRendererSource()
        val canvasBody = renderer.substringAfter("Canvas(").substringBefore("\n    }\n}\n", missingDelimiterValue = renderer)
        val trackListBody = cassetteCoverTrackListBody()

        assertFalse(
            "Canvas redraws during scrolling must not call TextMeasurer.measure for every handwriting token.",
            canvasBody.contains("textMeasurer.measure("),
        )
        assertTrue(
            "Measured handwriting token layouts should be cached with remember before entering Canvas.",
            Regex("val measuredTokens\\s*=\\s*remember\\(").containsMatchIn(renderer.substringBefore("Canvas(")),
        )
        assertTrue(
            "The widest-row geometry should be remembered so current-track recomposition does not remeasure the full tape.",
            Regex("val widestRowWidthPx\\s*=\\s*remember\\(").containsMatchIn(trackListBody),
        )
    }

    private fun cassetteCoverTrackListBody(): String {
        val source = modernSource("MixtapeApp.kt")
        val startMarker = "private fun CassetteCoverTrackList("
        val endMarker = "\n\n\nprivate fun tapeNameFor("
        assertTrue("Expected CassetteCoverTrackList in MixtapeApp.kt.", source.contains(startMarker))
        assertTrue("Expected tapeNameFor after CassetteCoverTrackList.", source.contains(endMarker))
        return source.substringAfter(startMarker).substringBefore(endMarker)
    }

    private fun jitteredRendererSource(): String = modernSource("JitteredHandwritingTextRenderer.kt")

    private fun modernSource(fileName: String): String {
        val path = listOf(
            File("app/src/modern/java/com/example/androidmixtape/ui/$fileName"),
            File("src/modern/java/com/example/androidmixtape/ui/$fileName"),
        ).firstOrNull { it.exists() }
        assertTrue("Expected to find modern source $fileName.", path != null)
        return path!!.readText().replace("\r\n", "\n")
    }
}
