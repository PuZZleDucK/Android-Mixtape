package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeSymbolSettingsScreenContractTest {
    private val projectDir = findAndroidMixtapeProjectDir(File(requireNotNull(System.getProperty("user.dir"))))
    private val appSource = File(projectDir, "app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt").readText()
    private val mainActivitySource = File(projectDir, "app/src/modern/java/com/example/androidmixtape/MainActivity.kt").readText()
    private val viewModelSource = File(projectDir, "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt").readText()

    @Test
    fun settingsScreenLinksToDedicatedMixtapeSymbolSettingsPage() {
        val settingsBody = functionBody(appSource, "SettingsScreen")

        assertTrue(
            "Main Settings should expose a Mixtape symbol settings button or card.",
            settingsBody.contains("Mixtape symbols"),
        )
        assertTrue(
            "MixtapeApp should expose a callback for opening the symbol settings page.",
            appSource.contains("onShowMixtapeSymbolSettings"),
        )
        assertTrue(
            "MixtapeApp should route MixtapeScreen.MixtapeSymbolSettings to a dedicated MixtapeSymbolSettingsScreen composable.",
            appSource.contains("MixtapeScreen.MixtapeSymbolSettings") && appSource.contains("MixtapeSymbolSettingsScreen("),
        )
        assertTrue(
            "MixtapeScreen should define MixtapeSymbolSettings for navigation from Settings.",
            Regex("""enum\s+class\s+MixtapeScreen[\s\S]*MixtapeSymbolSettings""").containsMatchIn(viewModelSource),
        )
    }

    @Test
    fun dedicatedPageRendersEveryEmbellishmentWithPreviewAndToggle() {
        val symbolSettingsBody = functionBody(appSource, "MixtapeSymbolSettingsScreen")
        assertTrue("Expected a dedicated MixtapeSymbolSettingsScreen composable.", symbolSettingsBody.isNotBlank())
        assertTrue(
            "Symbol settings should iterate over every MixtapeEmbellishment so new symbols are automatically listed.",
            symbolSettingsBody.contains("MixtapeEmbellishment.entries"),
        )
        assertTrue(
            "Symbol settings should render each hand-drawn symbol preview with HandDrawnEmbellishment.",
            symbolSettingsBody.contains("HandDrawnEmbellishment("),
        )
        assertTrue(
            "Symbol settings should render a checkbox or switch for each symbol.",
            symbolSettingsBody.contains("SettingsPreviewCard") &&
                File(projectDir, "app/src/modern/java/com/example/androidmixtape/ui/SettingsComponents.kt").readText().contains("Checkbox"),
        )
        assertTrue(
            "The UI should prevent turning off the final enabled symbol by disabling the last checked toggle.",
            symbolSettingsBody.contains("enabled =") && symbolSettingsBody.contains("enabledEmbellishments.size > 1"),
        )
    }

    @Test
    fun mainActivityWiresPersistentStoreAndCallbacks() {
        assertTrue(
            "MainActivity should use the SharedPreferencesMixtapeSymbolSettingsStore so symbol choices persist across app restarts.",
            mainActivitySource.contains("SharedPreferencesMixtapeSymbolSettingsStore"),
        )
        assertTrue(
            "MainActivity should wire Settings -> Mixtape symbol settings navigation into the ViewModel.",
            mainActivitySource.contains("onShowMixtapeSymbolSettings = viewModel::showMixtapeSymbolSettings"),
        )
        assertTrue(
            "MainActivity should wire symbol toggle changes into MixtapeViewModel.updateMixtapeEmbellishmentEnabled.",
            mainActivitySource.contains("updateMixtapeEmbellishmentEnabled"),
        )
    }

    private fun functionBody(source: String, functionName: String): String {
        val funIndex = source.indexOf("fun $functionName(")
        if (funIndex < 0) return ""
        val braceStart = source.indexOf('{', funIndex)
        if (braceStart < 0) return ""

        var depth = 0
        for (index in braceStart until source.length) {
            when (source[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return source.substring(braceStart + 1, index)
                }
            }
        }
        return ""
    }

    private fun findAndroidMixtapeProjectDir(start: File): File {
        var current: File? = start.absoluteFile
        while (current != null) {
            if (File(current, "settings.gradle.kts").isFile) return current
            current = current.parentFile
        }
        error("Could not find project root from ${start.absolutePath}")
    }
}
