package com.example.androidmixtape.viewmodel

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeVisualOptionSettingsContractTest {
    private val projectDir = findAndroidMixtapeRoot(File(requireNotNull(System.getProperty("user.dir"))))
    private val viewModelSource = File(projectDir, "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt").readText()
    private val visualPropertiesSource = File(projectDir, "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt").readText()
    private val combinedViewModelSource = viewModelSource + "\n" + visualPropertiesSource

    @Test
    fun tapeSkinAndHandwritingFontSettingsModelsAndStoresAreDefined() {
        assertTrue(
            "Expected MixtapeTapeSkinSettings to carry the globally enabled MixtapeTapeSkin values.",
            combinedViewModelSource.contains("MixtapeTapeSkinSettings") && combinedViewModelSource.contains("enabledTapeSkins"),
        )
        assertTrue(
            "Expected a MixtapeTapeSkinSettingsStore abstraction with in-memory and SharedPreferences-backed implementations.",
            combinedViewModelSource.contains("interface MixtapeTapeSkinSettingsStore") &&
                combinedViewModelSource.contains("InMemoryMixtapeTapeSkinSettingsStore") &&
                combinedViewModelSource.contains("SharedPreferencesMixtapeTapeSkinSettingsStore"),
        )
        assertTrue(
            "Expected MixtapeHandwritingFontSettings to carry the globally enabled MixtapeHandwritingFont values.",
            combinedViewModelSource.contains("MixtapeHandwritingFontSettings") && combinedViewModelSource.contains("enabledHandwritingFonts"),
        )
        assertTrue(
            "Expected a MixtapeHandwritingFontSettingsStore abstraction with in-memory and SharedPreferences-backed implementations.",
            combinedViewModelSource.contains("interface MixtapeHandwritingFontSettingsStore") &&
                combinedViewModelSource.contains("InMemoryMixtapeHandwritingFontSettingsStore") &&
                combinedViewModelSource.contains("SharedPreferencesMixtapeHandwritingFontSettingsStore"),
        )
        assertTrue(
            "Settings models should normalize empty/unknown saved names back to a non-empty default set so the UI cannot enter a blank cassette/font state.",
            combinedViewModelSource.contains("normalized") &&
                combinedViewModelSource.contains("fromNames") &&
                combinedViewModelSource.contains("takeUnless { it.isEmpty() }") &&
                combinedViewModelSource.contains("MixtapeTapeSkin.entries") &&
                combinedViewModelSource.contains("MixtapeHandwritingFont.entries"),
        )
    }

    @Test
    fun viewModelExposesNavigationStateAndLastEnabledGuardsForBothSettingsPages() {
        assertTrue(
            "MixtapeScreen should include dedicated TapeSkinSettings and HandwritingFontSettings entries separate from general Settings.",
            Regex("""enum\s+class\s+MixtapeScreen[\s\S]*TapeSkinSettings[\s\S]*HandwritingFontSettings""").containsMatchIn(viewModelSource),
        )
        assertTrue(
            "MixtapeUiState should expose current tape skin/font allow-list settings for the dedicated settings pages.",
            viewModelSource.contains("mixtapeTapeSkinSettings") &&
                viewModelSource.contains("mixtapeHandwritingFontSettings"),
        )
        assertTrue(
            "MixtapeViewModel should accept injectable tape skin and handwriting font settings stores for unit tests and persistence.",
            viewModelSource.contains("tapeSkinSettingsStore") &&
                viewModelSource.contains("handwritingFontSettingsStore"),
        )
        assertTrue(
            "MixtapeViewModel should expose navigation actions for both visual option settings pages.",
            viewModelSource.contains("showTapeSkinSettings") && viewModelSource.contains("showHandwritingFontSettings"),
        )
        assertTrue(
            "MixtapeViewModel should expose tape-skin and handwriting-font toggle actions and guard against disabling the final enabled option.",
            viewModelSource.contains("updateMixtapeTapeSkinEnabled") &&
                viewModelSource.contains("updateMixtapeHandwritingFontEnabled") &&
                viewModelSource.contains("enabledTapeSkins.size > 1") &&
                viewModelSource.contains("enabledHandwritingFonts.size > 1"),
        )
    }

    @Test
    fun visualAssignmentUsesOnlyEnabledSkinsAndFontsAndMigratesDisabledPersistedAssignments() {
        assertFalse(
            "visualPropertiesFor must not randomly choose tape skins from the full enum after tape-skin settings exist; it must choose from the enabled set.",
            viewModelSource.contains("MixtapeTapeSkin.entries.random(mixtapeRandom)"),
        )
        assertFalse(
            "visualPropertiesFor must not randomly choose handwriting fonts from the full enum after font settings exist; it must choose from the enabled set.",
            viewModelSource.contains("MixtapeHandwritingFont.entries.random(mixtapeRandom)"),
        )
        assertTrue(
            "visualPropertiesFor should build non-empty enabled tape-skin and handwriting-font lists from the settings, with enum fallbacks only for defensive empty states.",
            viewModelSource.contains("enabledTapeSkins") &&
                viewModelSource.contains("enabledHandwritingFonts") &&
                viewModelSource.contains("MixtapeTapeSkin.entries.filter") &&
                viewModelSource.contains("MixtapeHandwritingFont.entries.filter") &&
                viewModelSource.contains("ifEmpty"),
        )
        assertTrue(
            "When saved per-mixtape tape skin or font is disabled, visualPropertiesFor should copy only those fields to enabled replacements and save the migrated properties while preserving jitter, symbol, and color.",
            viewModelSource.contains("properties.copy") &&
                viewModelSource.contains("tapeSkin =") &&
                viewModelSource.contains("handwritingFont =") &&
                viewModelSource.contains("visualPropertiesStore.saveProperties"),
        )
    }
}

private fun findAndroidMixtapeRoot(start: File): File {
    var current: File? = start.absoluteFile
    while (current != null) {
        if (File(current, "settings.gradle.kts").isFile) return current
        current = current.parentFile
    }
    error("Could not find project root from ${start.absolutePath}")
}
