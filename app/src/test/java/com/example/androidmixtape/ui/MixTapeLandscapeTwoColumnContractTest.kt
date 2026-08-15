package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MixTapeLandscapeTwoColumnContractTest {
    @Test
    fun mixTapesLibraryChoosesLayoutFromAvailableOrientationConstraints() {
        val librarySource = mixTapeLibrarySource().normalizedLineEndings()

        assertTrue(
            "MixTapeLibrary should inspect available constraints, for example with BoxWithConstraints, before choosing portrait vs landscape layout.",
            librarySource.contains("BoxWithConstraints("),
        )
        assertTrue(
            "MixTapeLibrary should make landscape explicit from width-vs-height constraints so rotated phones get the two-wide cassette case layout.",
            listOf("maxWidth > maxHeight", "maxHeight < maxWidth", "isLandscape").any { librarySource.contains(it) },
        )
    }

    @Test
    fun landscapeMixTapesLibraryUsesTwoEqualCassetteSpineColumns() {
        val librarySource = mixTapeLibrarySource().normalizedLineEndings()
        val usesFixedTwoColumnGrid = librarySource.contains("LazyVerticalGrid") &&
            Regex("""GridCells\.Fixed\s*\(\s*2\s*\)""").containsMatchIn(librarySource)
        val usesTwoWeightedLazyColumns = librarySource.contains("isLandscape") &&
            Regex("""Row\([\s\S]*?LazyColumn[\s\S]*?\.weight\(\s*1f\s*\)[\s\S]*?LazyColumn[\s\S]*?\.weight\(\s*1f\s*\)""").containsMatchIn(librarySource)

        assertTrue(
            "Landscape MixTapeLibrary should render cassette spines in exactly two equal columns inside one case frame, using LazyVerticalGrid(GridCells.Fixed(2)) or an explicit Row of two weighted LazyColumns.",
            usesFixedTwoColumnGrid || usesTwoWeightedLazyColumns,
        )
    }

    @Test
    fun cassetteSpineRowsKeepTheirCompactDimensionsInLandscape() {
        val spineSource = cassetteSpineSource()

        assertTrue(
            "Cassette spine rows should keep their existing 64.dp-ish minimum height/touch target when the landscape screen adds a second column.",
            Regex("""heightIn\s*\(\s*min\s*=\s*(6[0-9]|[7-9][0-9])\.dp""").containsMatchIn(spineSource) ||
                Regex("""\.height\s*\(\s*(6[0-9]|[7-9][0-9])\.dp""").containsMatchIn(spineSource),
        )
        assertFalse(
            "Cassette spine rows should remain spine-shaped rows, not full cassette shell art forced through the shell aspect-ratio contract in the landscape grid.",
            spineSource.contains("aspectRatio(CASSETTE_SHELL_ASPECT_RATIO") ||
                spineSource.contains("aspectRatio(CASSETTE_VISUAL_ASPECT_RATIO"),
        )
        assertFalse(
            "Cassette spine rows should not borrow the front-facing case/J-card ratio in the landscape grid; long spine rows need their own ratio contract or an explicit touch-target exception.",
            spineSource.contains("aspectRatio(CASSETTE_CASE_FRONT_ASPECT_RATIO"),
        )
        assertTrue(
            "The same cassette-spine ratio contract must apply in landscape: either use a spine-specific physical ratio or name CassetteSpineTouchTargetRatioException for full-width accessible rows.",
            spineSource.contains("CASSETTE_SPINE_LABEL_ASPECT_RATIO") ||
                spineSource.contains("CASSETTE_CASE_SPINE_ASPECT_RATIO") ||
                spineSource.contains("CassetteSpineTouchTargetRatioException"),
        )
    }

    private fun mixTapeLibrarySource(): String {
        val source = mixtapeAppSource()
        val startMarker = "private fun MixTapeLibrary("
        val endMarker = "@Composable\nprivate fun SettingsScreen("
        assertTrue("Expected to find MixTapeLibrary in MixtapeApp.kt", source.contains(startMarker))
        assertTrue("Expected to find SettingsScreen after MixTapeLibrary in MixtapeApp.kt", source.contains(endMarker))
        return source.substringAfter(startMarker).substringBefore(endMarker)
    }

    private fun cassetteSpineSource(): String {
        val source = mixtapeAppSource()
        val startMarker = "private fun CassetteSpine"
        val endMarker = "@Composable\nprivate fun NowPlaying("
        assertTrue("Expected to find CassetteSpine* in MixtapeApp.kt", source.contains(startMarker))
        assertTrue("Expected to find NowPlaying after CassetteSpine* in MixtapeApp.kt", source.contains(endMarker))
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
