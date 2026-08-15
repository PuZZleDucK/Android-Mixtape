package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingMixTapeToggleContractTest {
    @Test
    fun nowPlayingExposesTracksVsMixTapesToggleWithoutLeavingNowPlaying() {
        val source = mixtapeAppSource().normalizedLineEndings()
        val nowPlayingBody = composableBody(source, "private fun NowPlaying(")

        assertTrue(
            "NowPlaying should define a local body mode for the track-list vs mix-tape-list pane.",
            nowPlayingBody.contains("NowPlayingBodyMode") || nowPlayingBody.contains("NowPlayingPane"),
        )
        assertTrue(
            "NowPlaying should render an accessible Tracks toggle.",
            nowPlayingBody.contains("Tracks") && nowPlayingBody.contains("Show current track list"),
        )
        assertTrue(
            "NowPlaying should render an accessible Mix tapes toggle.",
            nowPlayingBody.contains("Mix tapes") && nowPlayingBody.contains("Show mix tape list"),
        )
        assertTrue(
            "Switching the NowPlaying body should not navigate away; it should conditionally keep the current-track pane available.",
            nowPlayingBody.contains("CassetteCoverTrackList(") && nowPlayingBody.contains("MixTapeBriefcaseList("),
        )
    }

    @Test
    fun nowPlayingUsesSameHoistedMixtapeScrollStateAsLibraryForEjectPreservation() {
        val source = mixtapeAppSource().normalizedLineEndings()
        val appBody = mixtapeAppBody(source)
        val nowPlayingBody = composableBody(source, "private fun NowPlaying(")

        assertTrue(
            "MixtapeApp should pass the app-owned portrait LazyListState to NowPlaying so Eject returns to the same browsed list location.",
            Regex("""NowPlaying\([\s\S]*mixTapeListState\s*=\s*mixTapeListState""").containsMatchIn(appBody),
        )
        assertTrue(
            "MixtapeApp should pass the app-owned landscape LazyGridState to NowPlaying so Eject returns to the same browsed grid location.",
            Regex("""NowPlaying\([\s\S]*mixTapeGridState\s*=\s*mixTapeGridState""").containsMatchIn(appBody),
        )
        assertTrue(
            "NowPlaying should accept the hoisted portrait LazyListState instead of remembering a fresh one.",
            Regex("""mixTapeListState\s*:\s*LazyListState""").containsMatchIn(nowPlayingBody),
        )
        assertTrue(
            "NowPlaying should accept the hoisted landscape LazyGridState instead of remembering a fresh one.",
            Regex("""mixTapeGridState\s*:\s*LazyGridState""").containsMatchIn(nowPlayingBody),
        )
        assertFalse(
            "NowPlaying must not create independent lazy scroll state; that would lose the browsed location when Eject shows Mix Tapes.",
            nowPlayingBody.contains("rememberLazyListState(") || nowPlayingBody.contains("rememberLazyGridState("),
        )
    }

    @Test
    fun reusableBriefcaseListSupportsCurrentMixtapeHighlightAndSharedSelection() {
        val source = mixtapeAppSource().normalizedLineEndings()
        val briefcaseSource = composableBody(source, "private fun MixTapeBriefcaseList(")
        val librarySource = composableBody(source, "private fun MixTapeLibrary(")
        val nowPlayingBody = composableBody(source, "private fun NowPlaying(")
        val spineSource = composableBody(source, "private fun CassetteSpineRow(")

        assertTrue(
            "MixTapeLibrary should delegate the cassette briefcase body to MixTapeBriefcaseList so NowPlaying and Mix Tapes share one list/grid implementation.",
            librarySource.contains("MixTapeBriefcaseList("),
        )
        assertTrue(
            "NowPlaying should reuse MixTapeBriefcaseList for the Mix tapes pane.",
            nowPlayingBody.contains("MixTapeBriefcaseList("),
        )
        assertTrue(
            "MixTapeBriefcaseList should thread onMixTapeGroupClick(index) so selecting from NowPlaying opens/plays that mixtape.",
            briefcaseSource.contains("onMixTapeGroupClick") && briefcaseSource.contains("CassetteSpineRow("),
        )
        assertTrue(
            "MixTapeBriefcaseList should accept the currentMixtapeIndex from state for centering/highlighting.",
            Regex("""currentMixtapeIndex\s*:\s*Int""").containsMatchIn(briefcaseSource),
        )
        assertTrue(
            "CassetteSpineRow should receive an isCurrentMixtape flag or equivalent current-mixtape marker.",
            Regex("""isCurrentMixtape\s*:\s*Boolean""").containsMatchIn(spineSource),
        )
        assertTrue(
            "The current cassette spine should expose Current mixtape semantics for accessibility and testability.",
            spineSource.contains("Current mixtape"),
        )
    }

    @Test
    fun nowPlayingMixTapePaneCentersCurrentMixtapeOnlyWhenPaneOpens() {
        val source = mixtapeAppSource().normalizedLineEndings()
        val nowPlayingBody = composableBody(source, "private fun NowPlaying(")

        assertTrue(
            "NowPlaying should use uiState.currentMixtapeIndex to identify the mixtape to center/highlight.",
            nowPlayingBody.contains("currentMixtapeIndex"),
        )
        assertTrue(
            "Opening the Mix tapes pane should scroll the shared portrait/grid state so the current mixtape starts near the center.",
            nowPlayingBody.contains("animateScrollToItem") || nowPlayingBody.contains("scrollToItem"),
        )
        assertTrue(
            "Centering must be guarded by remembered state/effect keying so recomposition does not keep yanking the list away from the user's browsed location.",
            nowPlayingBody.contains("LaunchedEffect") && (nowPlayingBody.contains("lastCentered") || nowPlayingBody.contains("centeredMixtape")),
        )
    }

    @Test
    fun nowPlayingPreviewSlotShowsOppositeOfMainBodyMode() {
        val source = mixtapeAppSource().normalizedLineEndings()
        val toggleAndPreviewSource = composableBody(source, "private fun NowPlayingModeToggleAndSpine(")

        assertTrue(
            "The toggle/preview row should branch on bodyMode so the preview slot shows the opposite of the main pane instead of always showing the cassette spine.",
            toggleAndPreviewSource.contains("bodyMode == NowPlayingBodyMode.Tracks") || toggleAndPreviewSource.contains("when (bodyMode)"),
        )
        assertTrue(
            "When the main Now Playing body is Tracks, the opposite preview should remain the current-mixtape cassette spine.",
            Regex("""bodyMode\s*==\s*NowPlayingBodyMode\.Tracks[\s\S]*CassetteSpineRow\(""").containsMatchIn(toggleAndPreviewSource) ||
                Regex("""NowPlayingBodyMode\.Tracks[\s\S]*CassetteSpineRow\(""").containsMatchIn(toggleAndPreviewSource),
        )
        assertTrue(
            "When the main Now Playing body is Mix tapes, the opposite preview should switch to a compact CurrentTrackPreview in the old spine slot.",
            Regex("""bodyMode\s*==\s*NowPlayingBodyMode\.MixTapes[\s\S]*CurrentTrackPreview\(""").containsMatchIn(toggleAndPreviewSource) ||
                Regex("""NowPlayingBodyMode\.MixTapes[\s\S]*CurrentTrackPreview\(""").containsMatchIn(toggleAndPreviewSource),
        )
        assertTrue(
            "The compact preview should receive state.currentTrack plus queue/currentIndex context so it can identify the exact row from the tracklist.",
            toggleAndPreviewSource.contains("currentTrack = state.currentTrack") &&
                toggleAndPreviewSource.contains("tracks = state.queueTracks") &&
                toggleAndPreviewSource.contains("currentIndex = state.currentIndex"),
        )
    }

    @Test
    fun currentTrackPreviewKeepsTrackListPaperStylingAndEmptyQueueFallback() {
        val source = mixtapeAppSource().normalizedLineEndings()
        val previewSource = composableBody(source, "private fun CurrentTrackPreview(")

        assertTrue(
            "CurrentTrackPreview should expose accessibility semantics for test automation and screen readers.",
            previewSource.contains("Current track preview"),
        )
        assertTrue(
            "CurrentTrackPreview should accept the current track, full queue, and current index for accurate tracklist context.",
            Regex("""currentTrack\s*:\s*Track\?""").containsMatchIn(previewSource) &&
                Regex("""tracks\s*:\s*List<Track>""").containsMatchIn(previewSource) &&
                Regex("""currentIndex\s*:\s*Int""").containsMatchIn(previewSource),
        )
        assertTrue(
            "CurrentTrackPreview should use the same sleeve-paper and case-plastic theme palettes as the main track list.",
            previewSource.contains("sleeveTheme.paperPalette()") &&
                previewSource.contains("caseTheme.plasticPalette()") &&
                previewSource.contains("CardDefaults.cardColors(containerColor = sleevePaper.base)"),
        )
        assertFalse(
            "The compact track-list preview should not waste space on a Current track header.",
            previewSource.contains("text = \"Current track\""),
        )
        assertTrue(
            "CurrentTrackPreview should center a three-row window containing the previous, current, and next queue entries.",
            previewSource.contains("previewIndex - 1") &&
                previewSource.contains("previewIndex to trackRowText(previewIndex)") &&
                previewSource.contains("previewIndex + 1"),
        )
        assertTrue(
            "Only the current row should use the sleeve theme highlight; neighboring rows should retain normal ink.",
            previewSource.contains("if (isCurrent) sleevePaper.accent else sleevePaper.ink"),
        )
        assertTrue(
            "The mini track window should render the regular track-row typography and spacing at canonical size, then uniformly scale the whole surface.",
            previewSource.contains("requiredWidth(sourceWidth)") &&
                previewSource.contains("scaleX = previewScale") &&
                previewSource.contains("scaleY = previewScale") &&
                previewSource.contains("fontSize = 40.sp") &&
                previewSource.contains("padding(horizontal = 18.dp)") &&
                !previewSource.contains("${'$'}{index + 1}."),
        )
        assertTrue(
            "CurrentTrackPreview should not crash or render a blank row when the queue/currentTrack is missing.",
            previewSource.contains("No current track") || previewSource.contains("No tracks queued"),
        )
    }

    private fun mixtapeAppBody(source: String): String {
        val startMarker = "fun MixtapeApp("
        val endMarker = "@Composable\nprivate fun PermissionRequired("
        assertTrue("Expected to find MixtapeApp in MixtapeApp.kt", source.contains(startMarker))
        assertTrue("Expected to find PermissionRequired after MixtapeApp in MixtapeApp.kt", source.contains(endMarker))
        return source.substringAfter(startMarker).substringBefore(endMarker)
    }

    private fun composableBody(source: String, startMarker: String): String {
        val start = source.indexOf(startMarker)
        assertTrue("Expected to find $startMarker in MixtapeApp.kt", start >= 0)
        val nextComposable = source.indexOf("\n@Composable", start + startMarker.length)
        if (nextComposable > start) return source.substring(start, nextComposable)
        return source.substring(start)
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
