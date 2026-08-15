package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackLongPressMenuContractTest {
    @Test
    fun cassetteTrackRowsUseLongPressOnlyActionMenuWithExactLabels() {
        val source = mixtapeAppSource()
        val trackListBody = functionBody(source, "private fun CassetteCoverTrackList(")

        assertTrue(
            "CassetteCoverTrackList should import/use Modifier.combinedClickable so normal taps and long presses can remain distinct.",
            source.contains("combinedClickable") && trackListBody.contains("combinedClickable"),
        )
        assertTrue(
            "Track rows must open actions from onLongClick, not from a normal row click.",
            trackListBody.contains("onLongClick"),
        )
        assertTrue(
            "Long-pressed track rows should show a compact Compose DropdownMenu anchored to that row.",
            trackListBody.contains("DropdownMenu(") && trackListBody.contains("DropdownMenuItem("),
        )

        val labels = listOf("Delete from device", "Remove from mixtape", "Track info")
        labels.forEach { label ->
            assertEquals(
                "The track action menu should render exactly one visible '$label' item.",
                1,
                Regex("Text\\(\\s*\\\"${Regex.escape(label)}\\\"").findAll(trackListBody).count(),
            )
        }
        assertTrue(
            "Each action menu anchor should expose a stable accessibility label such as 'Track actions for <title>'.",
            trackListBody.contains("Track actions for") && trackListBody.contains("contentDescription"),
        )
    }

    @Test
    fun trackActionCallbacksAreThreadedFromAppToNowPlayingAndTrackList() {
        val source = mixtapeAppSource()
        val appSignature = source.substringAfter("fun MixtapeApp(").substringBefore(") {")
        val nowPlayingBody = functionBody(source, "private fun NowPlaying(")
        val trackListSignature = source.substringAfter("private fun CassetteCoverTrackList(").substringBefore(") {")

        listOf(
            "onDeleteTrackFromDevice: (Track) -> Unit",
            "onRemoveTrackFromMixtape: (Track) -> Unit",
            "onShowTrackInfo: (Track) -> Unit",
        ).forEach { callbackContract ->
            assertTrue(
                "MixtapeApp should expose $callbackContract so MainActivity/ViewModel owns side effects.",
                appSignature.contains(callbackContract),
            )
        }
        listOf("onDeleteTrackFromDevice", "onRemoveTrackFromMixtape", "onShowTrackInfo").forEach { callbackName ->
            assertTrue(
                "NowPlaying should pass $callbackName down to CassetteCoverTrackList in both portrait and landscape branches.",
                Regex("CassetteCoverTrackList\\([\\s\\S]*?$callbackName\\s*=").findAll(nowPlayingBody).count() >= 2,
            )
            assertTrue(
                "CassetteCoverTrackList should accept $callbackName as a Track callback.",
                trackListSignature.contains("$callbackName: (Track) -> Unit"),
            )
        }
    }

    @Test
    fun trackInfoScreenIsRoutedAndShowsRequiredMetadataLabels() {
        val source = mixtapeAppSource()
        val appBody = functionBody(source, "fun MixtapeApp(")

        assertTrue(
            "MixtapeScreen should include a TrackInfo destination for the new detail page.",
            viewModelSource().contains("TrackInfo"),
        )
        assertTrue(
            "MixtapeApp should route MixtapeScreen.TrackInfo to a dedicated TrackInfoScreen.",
            appBody.contains("MixtapeScreen.TrackInfo") && appBody.contains("TrackInfoScreen("),
        )

        val infoBody = functionBody(source, "private fun TrackInfoScreen(")
        listOf("Title", "Artist", "Duration", "MediaStore ID", "URI", "Display name", "Album", "MIME type", "Size", "Date added", "Date modified", "Track number", "Mixtape").forEach { label ->
            assertTrue(
                "TrackInfoScreen should display a '$label' metadata row/label.",
                infoBody.contains(label),
            )
        }
        assertTrue(
            "TrackInfoScreen should provide a back affordance that returns to the prior now-playing tape.",
            infoBody.contains("onBack") && (infoBody.contains("Back") || infoBody.contains("Now Playing")),
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

    private fun mixtapeAppSource(): String = readFirstExisting(
        "app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt",
        "app/src/main/java/com/example/androidmixtape/ui/MixtapeApp.kt",
    )

    private fun viewModelSource(): String = readFirstExisting(
        "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt",
    )

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
