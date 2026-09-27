package com.example.androidmixtape.viewmodel

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeExclusionSettingsContractTest {
    private val projectDir = findProjectDir(File(requireNotNull(System.getProperty("user.dir"))))
    private val viewModelSource = File(projectDir, "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt").readText()
    private val exclusionSource = listOf(
        File(projectDir, "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeExclusionSettings.kt"),
        File(projectDir, "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt"),
    ).filter { it.isFile }.joinToString("\n") { it.readText() }

    @Test
    fun exclusionSettingsModelMatcherAndStoresAreDefined() {
        assertTrue(
            "Expected a shared MixtapeExclusionSettings model with ordered filenamePatterns.",
            exclusionSource.contains("data class MixtapeExclusionSettings") && exclusionSource.contains("filenamePatterns"),
        )
        assertTrue(
            "Expected a FilenameExclusionMatcher object for glob-style filename matching.",
            exclusionSource.contains("FilenameExclusionMatcher") && exclusionSource.contains("fun matches"),
        )
        assertTrue(
            "Expected a MixtapeExclusionSettingsStore abstraction for pattern persistence.",
            exclusionSource.contains("interface MixtapeExclusionSettingsStore"),
        )
        assertTrue(
            "Expected an in-memory exclusion settings store for unit tests/default wiring.",
            exclusionSource.contains("InMemoryMixtapeExclusionSettingsStore"),
        )
        assertTrue(
            "Expected a SharedPreferences-backed exclusion settings store for modern app persistence.",
            exclusionSource.contains("SharedPreferencesMixtapeExclusionSettingsStore"),
        )
    }

    @Test
    fun viewModelExposesExclusionSettingsStateNavigationAndMutations() {
        assertTrue(
            "MixtapeScreen should include a dedicated ExclusionSettings route from Settings.",
            Regex("""enum\s+class\s+MixtapeScreen[\s\S]*ExclusionSettings""").containsMatchIn(viewModelSource),
        )
        assertTrue(
            "MixtapeUiState should expose mixtapeExclusionSettings for the settings page.",
            viewModelSource.contains("mixtapeExclusionSettings"),
        )
        assertTrue(
            "MixtapeViewModel should expose showExclusionSettings navigation.",
            viewModelSource.contains("showExclusionSettings"),
        )
        assertTrue(
            "MixtapeViewModel should expose add/remove filename exclusion pattern mutations.",
            viewModelSource.contains("addFilenameExclusionPattern") && viewModelSource.contains("removeFilenameExclusionPattern"),
        )
        assertTrue(
            "MixtapeViewModel.Factory should accept and pass an exclusionSettingsStore so persisted exclusions are loaded before grouping.",
            viewModelSource.contains("exclusionSettingsStore") && viewModelSource.contains("MixtapeExclusionSettingsStore"),
        )
    }

    @Test
    fun viewModelKeepsRawLibraryAndBuildsMixtapesFromFilteredTracks() {
        assertTrue(
            "ViewModel should keep an unfiltered/raw library list so removing a pattern can make tracks eligible again.",
            viewModelSource.contains("allLibraryTracks") || viewModelSource.contains("rawLibraryTracks") || viewModelSource.contains("unfilteredLibraryTracks"),
        )
        assertTrue(
            "Filtering should be applied with FilenameExclusionMatcher before controller queues and mix tape groups are rebuilt.",
            viewModelSource.contains("FilenameExclusionMatcher") && viewModelSource.contains("resolveMixtapeGroups(mixtapeTracks"),
        )
    }
}

private fun findProjectDir(start: File): File {
    var current: File? = start.absoluteFile
    while (current != null) {
        if (File(current, "settings.gradle.kts").isFile) return current
        current = current.parentFile
    }
    error("Could not find project root from ${start.absolutePath}")
}
