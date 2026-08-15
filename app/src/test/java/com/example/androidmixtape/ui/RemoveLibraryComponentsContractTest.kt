package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoveLibraryComponentsContractTest {
    @Test
    fun modernUiDoesNotRenderHeaderTitlesAboveNavigation() {
        val source = mixtapeAppSource()

        assertFalse(
            "The modern app should not render the Mixtape TopAppBar title above the navigation row.",
            source.contains("TopAppBar"),
        )
        assertFalse(
            "The modern app should not configure a Scaffold topBar for the removed title area.",
            source.contains("topBar ="),
        )
        assertFalse(
            "The top-level state.message banner above screen navigation should be removed; keep status guidance inside specific states instead.",
            source.contains("text = state.message"),
        )
    }

    @Test
    fun modernUiDoesNotExposeLibraryScreenOrLibraryNavigationButton() {
        val source = mixtapeAppSource()

        assertFalse(
            "The modern app should no longer switch to a Library screen route.",
            source.contains("MixtapeScreen.Library"),
        )
        assertFalse(
            "The standalone TrackLibrary page should be removed from modern UI source.",
            source.contains("private fun TrackLibrary("),
        )
        assertFalse(
            "No visible Library navigation button should remain in mixtape, settings, or now-playing screens.",
            source.contains("Text(\"Library\")"),
        )
    }

    @Test
    fun viewModelContractNoLongerModelsLibraryScreen() {
        val source = viewModelSource()

        assertFalse(
            "MixtapeScreen should no longer include a Library enum entry.",
            source.contains("Library,"),
        )
        assertFalse(
            "The ViewModel should not expose backToLibrary once the library page is removed.",
            source.contains("fun backToLibrary("),
        )
        assertFalse(
            "No ViewModel transition should target MixtapeScreen.Library after the library page is removed.",
            source.contains("screen = MixtapeScreen.Library"),
        )
    }

    private fun mixtapeAppSource(): String = sourceFromCandidates(
        "MixtapeApp.kt",
        File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        File("app/src/main/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        File("src/main/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
    )

    private fun viewModelSource(): String = sourceFromCandidates(
        "MixtapeViewModel.kt",
        File("app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt"),
        File("src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt"),
    )

    private fun sourceFromCandidates(label: String, vararg candidates: File): String {
        val path = candidates.firstOrNull { it.exists() }
        assertTrue("Expected to find $label from ${System.getProperty("user.dir")}", path != null)
        return path!!.readText()
    }
}
