package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TracklistDoubleClickUiContractTest {
    @Test
    fun doubleClickCallbackIsThreadedFromActivityToBothTrackLists() {
        val appSource = readFirstExisting("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt")
        val activitySource = readFirstExisting("app/src/modern/java/com/example/androidmixtape/MainActivity.kt")
        val appSignature = appSource.substringAfter("fun MixtapeApp(").substringBefore(") {")
        val nowPlayingSignature = appSource.substringAfter("private fun NowPlaying(").substringBefore(") {")
        val nowPlayingBody = functionBody(appSource, "private fun NowPlaying(")
        val trackListSignature = appSource.substringAfter("private fun CassetteCoverTrackList(").substringBefore(") {")

        assertTrue(
            "MixtapeApp should expose onTrackDoubleClick(index, track) so MainActivity/ViewModel own the cue/playback side effect.",
            appSignature.contains("onTrackDoubleClick: (Int, Track) -> Unit"),
        )
        assertTrue(
            "MainActivity should wire track-list double-clicks to MixtapeViewModel.jumpToTrackWithCue.",
            activitySource.contains("onTrackDoubleClick = viewModel::jumpToTrackWithCue"),
        )
        assertTrue(
            "NowPlaying should accept onTrackDoubleClick(index, track) and pass it to the portrait and landscape track lists.",
            nowPlayingSignature.contains("onTrackDoubleClick: (Int, Track) -> Unit"),
        )
        assertTrue(
            "CassetteCoverTrackList should accept onTrackDoubleClick(index, track).",
            trackListSignature.contains("onTrackDoubleClick: (Int, Track) -> Unit"),
        )
        assertEquals(
            "Both portrait and landscape CassetteCoverTrackList calls should receive onTrackDoubleClick.",
            2,
            Regex("CassetteCoverTrackList\\([\\s\\S]*?onTrackDoubleClick\\s*=\\s*onTrackDoubleClick").findAll(nowPlayingBody).count(),
        )
    }

    @Test
    fun trackRowsUseDoubleClickForJumpWhileSingleClickStaysInertAndLongPressMenuRemains() {
        val source = readFirstExisting("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt")
        val trackListBody = functionBody(source, "private fun CassetteCoverTrackList(")

        assertTrue(
            "Track rows should use Modifier.combinedClickable so click, double-click, and long-press gestures can coexist.",
            trackListBody.contains("combinedClickable"),
        )
        assertTrue(
            "Normal single-click should remain inert; double-click is the explicit jump gesture.",
            Regex("onClick\\s*=\\s*\\{\\s*\\}").containsMatchIn(trackListBody),
        )
        assertTrue(
            "Track rows should call onTrackDoubleClick(index, track) from combinedClickable(onDoubleClick = ...).",
            Regex("onDoubleClick\\s*=\\s*\\{\\s*onTrackDoubleClick\\(\\s*index\\s*,\\s*track\\s*\\)\\s*\\}").containsMatchIn(trackListBody),
        )
        assertTrue(
            "The existing long-press track actions menu must remain wired from onLongClick.",
            trackListBody.contains("onLongClick") && trackListBody.contains("expandedTrackId = track.id"),
        )
    }

    private fun functionBody(source: String, startMarker: String): String {
        val start = source.indexOf(startMarker)
        assertTrue("Expected to find $startMarker in source", start >= 0)
        val nextComposable = source.indexOf("\n@Composable", start + startMarker.length)
        val nextPrivateFun = source.indexOf("\nprivate fun", start + startMarker.length)
        val candidates = listOf(nextComposable, nextPrivateFun).filter { it > start }
        val end = candidates.minOrNull() ?: source.length
        return source.substring(start, end)
    }

    private fun readFirstExisting(vararg paths: String): String {
        val candidates = paths.flatMap { path ->
            val withoutAppPrefix = path.removePrefix("app/")
            listOf(File(path), File(withoutAppPrefix), File("../$path"), File("../$withoutAppPrefix"))
        }
        val file = candidates.firstOrNull { it.exists() }
        assertTrue("Expected one of ${candidates.map { it.path }} from ${System.getProperty("user.dir")}", file != null)
        return file!!.readText()
    }
}
