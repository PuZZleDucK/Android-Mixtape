package com.example.androidmixtape.viewmodel

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeSymbolSettingsContractTest {
    private val projectDir = findProjectDir(File(requireNotNull(System.getProperty("user.dir"))))
    private val viewModelSource = File(projectDir, "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt").readText()
    private val visualPropertiesSource = File(projectDir, "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt").readText()

    @Test
    fun symbolSettingsModelAndStoresAreDefined() {
        assertTrue(
            "Expected a shared MixtapeSymbolSettings model carrying enabled MixtapeEmbellishment values.",
            visualPropertiesSource.contains("MixtapeSymbolSettings") && visualPropertiesSource.contains("enabledEmbellishments"),
        )
        assertTrue(
            "Expected a MixtapeSymbolSettingsStore abstraction for global symbol/image preferences.",
            visualPropertiesSource.contains("interface MixtapeSymbolSettingsStore"),
        )
        assertTrue(
            "Expected an in-memory symbol settings store for unit tests.",
            visualPropertiesSource.contains("InMemoryMixtapeSymbolSettingsStore"),
        )
        assertTrue(
            "Expected a SharedPreferences-backed symbol settings store for app persistence.",
            visualPropertiesSource.contains("SharedPreferencesMixtapeSymbolSettingsStore"),
        )
    }

    @Test
    fun viewModelExposesSymbolSettingsStateAndNavigation() {
        assertTrue(
            "MixtapeScreen should include a dedicated MixtapeSymbolSettings entry separate from general Settings.",
            Regex("""enum\s+class\s+MixtapeScreen[\s\S]*MixtapeSymbolSettings""").containsMatchIn(viewModelSource),
        )
        assertTrue(
            "MixtapeUiState should expose enabled symbol settings for the dedicated settings page.",
            viewModelSource.contains("mixtapeSymbolSettings") || viewModelSource.contains("enabledMixtapeEmbellishments"),
        )
        assertTrue(
            "MixtapeViewModel should have a showMixtapeSymbolSettings navigation method.",
            viewModelSource.contains("showMixtapeSymbolSettings"),
        )
        assertTrue(
            "MixtapeViewModel should expose updateMixtapeEmbellishmentEnabled(embellishment, enabled) and enforce at least one enabled symbol.",
            viewModelSource.contains("updateMixtapeEmbellishmentEnabled") && viewModelSource.contains("enabledEmbellishments"),
        )
    }

    @Test
    fun visualAssignmentUsesOnlyEnabledSymbolsAndMigratesDisabledPersistedAssignments() {
        assertFalse(
            "visualPropertiesFor must not randomly choose from the full MixtapeEmbellishment enum after symbol settings exist; it must choose from the enabled set.",
            viewModelSource.contains("MixtapeEmbellishment.entries.random(mixtapeRandom)"),
        )
        assertTrue(
            "When a saved per-mixtape embellishment is disabled, visualPropertiesFor should copy it to an enabled replacement and save the migrated properties.",
            viewModelSource.contains("copy(embellishment") && viewModelSource.contains("visualPropertiesStore.saveProperties"),
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
