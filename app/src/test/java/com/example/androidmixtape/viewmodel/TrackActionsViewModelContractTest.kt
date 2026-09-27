package com.example.androidmixtape.viewmodel

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackActionsViewModelContractTest {
    @Test
    fun stateAndViewModelExposeTrackActionEntryPoints() {
        val source = viewModelSource()
        val screenBlock = enumBlock(source, "enum class MixtapeScreen")
        val uiStateSignature = source.substringAfter("data class MixtapeUiState(").substringBefore(") {")

        assertTrue(
            "MixtapeScreen must add TrackInfo so track details are a first-class navigation destination.",
            screenBlock.contains("TrackInfo"),
        )
        assertTrue(
            "MixtapeUiState should expose the track selected for the info page as selectedTrackInfo: Track?.",
            uiStateSignature.contains("selectedTrackInfo: Track?"),
        )

        listOf(
            "fun showTrackInfo(trackId: Long)",
            "fun backFromTrackInfo()",
            "fun removeTrackFromCurrentMixtape(trackId: Long)",
            "fun deleteTrackFromDevice(trackId: Long)",
        ).forEach { functionContract ->
            assertTrue(
                "MixtapeViewModel should expose $functionContract for the long-press menu actions.",
                source.contains(functionContract),
            )
        }
    }

    @Test
    fun deleteFromDeviceUsesRepositoryResultAndLeavesStateOnFailure() {
        val source = viewModelSource()
        val body = functionBody(source, "fun deleteTrackFromDevice(trackId: Long)")
        val repositorySource = repositorySource()

        assertTrue(
            "AudioRepository should own platform deletion through a suspend deleteTrack(track: Track): DeleteTrackResult API.",
            repositorySource.contains("suspend fun deleteTrack(track: Track): DeleteTrackResult"),
        )
        assertTrue(
            "Repository deletion should return a typed DeleteTrackResult so scoped-storage failures are not treated as success.",
            repositorySource.contains("DeleteTrackResult") && repositorySource.contains("Success") && repositorySource.contains("Failure"),
        )
        assertTrue(
            "ViewModel delete should call repository.deleteTrack before mutating library/mixtape state.",
            body.contains("repository.deleteTrack"),
        )
        assertTrue(
            "A successful delete should remove the track from libraryTracks and mixtapeTracks.",
            body.contains("removeDeletedTrackFromAppState") &&
                functionBody(source, "private fun removeDeletedTrackFromAppState(").let {
                    it.contains("libraryTracks") && it.contains("mixtapeTracks") && it.contains("filterNot")
                },
        )
        assertTrue(
            "A failed delete should keep state unchanged and publish a failure message instead of silently hiding the track.",
            body.contains("Failure") && (body.contains("delete failed") || body.contains("Could not delete") || body.contains("Unable to delete")),
        )
    }

    private fun functionBody(source: String, startMarker: String): String {
        val start = source.indexOf(startMarker)
        assertTrue("Expected to find $startMarker in MixtapeViewModel.kt", start >= 0)
        val nextFun = source.indexOf("\n    fun ", start + startMarker.length)
        val nextOverride = source.indexOf("\n    override fun ", start + startMarker.length)
        val candidates = listOf(nextFun, nextOverride).filter { it > start }
        val end = candidates.minOrNull() ?: source.length
        return source.substring(start, end)
    }

    private fun enumBlock(source: String, marker: String): String {
        val start = source.indexOf(marker)
        assertTrue("Expected to find $marker", start >= 0)
        val end = source.indexOf("}", start)
        assertTrue("Expected enum block to close", end > start)
        return source.substring(start, end)
    }

    private fun viewModelSource(): String = readFirstExisting("app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt")

    private fun repositorySource(): String = readFirstExisting("app/src/main/java/com/example/androidmixtape/data/AudioRepository.kt")

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
