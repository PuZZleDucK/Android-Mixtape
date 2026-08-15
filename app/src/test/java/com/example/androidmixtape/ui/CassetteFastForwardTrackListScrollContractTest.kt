package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CassetteFastForwardTrackListScrollContractTest {
    @Test
    fun currentTrackAutoScrollDoesNotDependOnPreviouslyHighlightedRowBeingVisible() {
        val trackListBody = cassetteCoverTrackListSource().normalizedLineEndings()
        val autoScrollEffect = launchedEffectBody(trackListBody)

        assertTrue(
            "CassetteCoverTrackList should key its auto-scroll effect to currentTrackId changes so rapid Next jumps can trigger a fresh scroll target.",
            trackListBody.contains("val currentTrackId = tracks.getOrNull(currentIndex)?.id") &&
                trackListBody.contains("LaunchedEffect(currentTrackId, scrollContent)"),
        )
        assertTrue(
            "Now Playing track-list auto-scroll should attempt to center the measured current row when scrollContent is enabled. " +
                "It also should use row visibility clues to decide when correction is needed.",
            autoScrollEffect.contains("scrollContent") &&
                autoScrollEffect.contains("trackCenterOffsetsInViewport[currentTrackId]") &&
                autoScrollEffect.contains("trackTopOffsetsInViewport") &&
                autoScrollEffect.contains("trackBottomOffsetsInViewport") &&
                autoScrollEffect.contains("contentScrollState.animateScrollTo"),
        )
        assertTrue(
            "Current-row centering should remain bounded and only run when a measurable row is in view or off-center.",
            autoScrollEffect.contains("isCurrentRowInViewBand") &&
                autoScrollEffect.contains("isCurrentRowCentered"),
        )
        assertFalse(
            "Rapid fast-forward can leave the previously highlighted row off screen; auto-scroll must not be gated by a lastCurrentTrackWasVisible flag. " +
                "Check only for current-row visibility/capture logic.",
            trackListBody.contains("lastCurrentTrackWasVisible"),
        )
        assertFalse(
            "The current-track scroll effect must decide from the new/current row measurement, not from whether the previous current row was visible.",
            Regex("previous[\\s\\S]{0,120}visible|visible[\\s\\S]{0,120}previous", RegexOption.IGNORE_CASE)
                .containsMatchIn(autoScrollEffect),
        )
    }

    @Test
    fun currentTrackAutoScrollKeepsCenteredClampedScrollMath() {
        val autoScrollEffect = launchedEffectBody(cassetteCoverTrackListSource().normalizedLineEndings())

        assertTrue(
            "The current-row scroll target should center the measured row in the viewport.",
            autoScrollEffect.contains("contentScrollState.value + centerOffset - (viewportHeight / 2f)") ||
                autoScrollEffect.contains("contentScrollState.value + centerOffset - viewportHeight / 2f"),
        )
        assertTrue(
            "The current-row scroll target should be clamped so first/last tracks cannot overscroll.",
            autoScrollEffect.contains(".coerceIn(0, contentScrollState.maxValue)"),
        )
        assertTrue(
            "The current-row scroll effect should wait for at least one layout frame before reading row measurements.",
            autoScrollEffect.contains("withFrameNanos"),
        )
    }

    private fun launchedEffectBody(trackListBody: String): String {
        val startMarker = "LaunchedEffect(currentTrackId, scrollContent)"
        val start = trackListBody.indexOf(startMarker)
        assertTrue("Expected current-track LaunchedEffect in CassetteCoverTrackList.", start >= 0)
        val end = trackListBody.indexOf("\n    Card(", start)
        assertTrue("Expected Card body after current-track LaunchedEffect.", end > start)
        return trackListBody.substring(start, end)
    }

    private fun cassetteCoverTrackListSource(): String {
        val source = mixtapeAppSource()
        val startMarker = "private fun CassetteCoverTrackList("
        val endMarker = "\n\n\nprivate fun tapeNameFor("
        assertTrue("Expected to find CassetteCoverTrackList in MixtapeApp.kt", source.contains(startMarker))
        assertTrue("Expected to find tapeNameFor after CassetteCoverTrackList in MixtapeApp.kt", source.contains(endMarker))
        return source.substringAfter(startMarker).substringBefore(endMarker)
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

    private fun String.normalizedLineEndings(): String = replace("\r\n", "\n")
}
