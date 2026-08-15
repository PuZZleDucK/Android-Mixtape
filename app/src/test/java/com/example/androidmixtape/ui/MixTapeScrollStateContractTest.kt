package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MixTapeScrollStateContractTest {
    @Test
    fun mixtapeAppOwnsStableScrollStatesAcrossNowPlayingSwap() {
        val source = mixtapeAppSource().normalizedLineEndings()
        val appBody = mixtapeAppBody(source)

        assertTrue(
            "MixtapeApp should remember the portrait Mix Tape list LazyListState so returning from NowPlaying/Eject restores the previous list position.",
            Regex("""val\s+mixTapeListState\s*=\s*rememberLazyListState\s*\(""").containsMatchIn(appBody),
        )
        assertTrue(
            "MixtapeApp should remember the landscape Mix Tape grid LazyGridState so rotated/two-column users keep their position after Eject.",
            Regex("""val\s+mixTapeGridState\s*=\s*rememberLazyGridState\s*\(""").containsMatchIn(appBody),
        )
    }

    @Test
    fun mixTapeLibraryReceivesStatesInsteadOfCreatingFreshOnes() {
        val librarySource = mixTapeLibrarySource().normalizedLineEndings()

        assertTrue(
            "MixTapeLibrary should take a hoisted LazyListState parameter for the portrait LazyColumn.",
            Regex("""mixTapeListState\s*:\s*LazyListState""").containsMatchIn(librarySource),
        )
        assertTrue(
            "MixTapeLibrary should take a hoisted LazyGridState parameter for the landscape LazyVerticalGrid.",
            Regex("""mixTapeGridState\s*:\s*LazyGridState""").containsMatchIn(librarySource),
        )
        assertFalse(
            "MixTapeLibrary must not remember fresh lazy scroll state internally, because it is removed from composition while NowPlaying is shown.",
            librarySource.contains("rememberLazyListState(") || librarySource.contains("rememberLazyGridState("),
        )
    }

    @Test
    fun portraitAndLandscapeContainersUseTheHoistedStates() {
        val source = mixtapeAppSource().normalizedLineEndings()
        val appBody = mixtapeAppBody(source)
        val librarySource = mixTapeLibrarySource(source).normalizedLineEndings()

        assertTrue(
            "The MixTapes screen should pass the app-owned list state into MixTapeLibrary.",
            Regex("""MixTapeLibrary\([\s\S]*mixTapeListState\s*=\s*mixTapeListState""").containsMatchIn(appBody),
        )
        assertTrue(
            "The MixTapes screen should pass the app-owned grid state into MixTapeLibrary.",
            Regex("""MixTapeLibrary\([\s\S]*mixTapeGridState\s*=\s*mixTapeGridState""").containsMatchIn(appBody),
        )
        assertTrue(
            "The portrait LazyColumn should use the hoisted list state.",
            Regex("""LazyColumn\([\s\S]*state\s*=\s*mixTapeListState""").containsMatchIn(librarySource),
        )
        assertTrue(
            "The landscape LazyVerticalGrid should use the hoisted grid state.",
            Regex("""LazyVerticalGrid\([\s\S]*state\s*=\s*mixTapeGridState""").containsMatchIn(librarySource),
        )
    }

    private fun mixtapeAppBody(source: String): String {
        val startMarker = "fun MixtapeApp("
        val endMarker = "@Composable\nprivate fun PermissionRequired("
        assertTrue("Expected to find MixtapeApp in MixtapeApp.kt", source.contains(startMarker))
        assertTrue("Expected to find PermissionRequired after MixtapeApp in MixtapeApp.kt", source.contains(endMarker))
        return source.substringAfter(startMarker).substringBefore(endMarker)
    }

    private fun mixTapeLibrarySource(source: String = mixtapeAppSource()): String {
        val startMarker = "private fun MixTapeLibrary("
        val endMarker = "@Composable\nprivate fun SettingsScreen("
        assertTrue("Expected to find MixTapeLibrary in MixtapeApp.kt", source.contains(startMarker))
        assertTrue("Expected to find SettingsScreen after MixTapeLibrary in MixtapeApp.kt", source.contains(endMarker))
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
