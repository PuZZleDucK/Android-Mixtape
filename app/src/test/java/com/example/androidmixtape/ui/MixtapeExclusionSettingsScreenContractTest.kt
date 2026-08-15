package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeExclusionSettingsScreenContractTest {
    private val projectDir = findAndroidMixtapeProjectDir(File(requireNotNull(System.getProperty("user.dir"))))
    private val appSource = File(projectDir, "app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt").readText()
    private val mainActivitySource = File(projectDir, "app/src/modern/java/com/example/androidmixtape/MainActivity.kt").readText()
    private val viewModelSource = File(projectDir, "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt").readText()

    @Test
    fun settingsScreenLinksToDedicatedExclusionSettingsPage() {
        val settingsBody = functionBody(appSource, "SettingsScreen")

        assertTrue(
            "Main Settings should expose an Exclusion settings button/card.",
            settingsBody.contains("Exclusion settings"),
        )
        assertTrue(
            "MixtapeApp should expose a callback for opening the exclusion settings page.",
            appSource.contains("onShowExclusionSettings"),
        )
        assertTrue(
            "MixtapeApp should route MixtapeScreen.ExclusionSettings to a dedicated ExclusionSettingsScreen composable.",
            appSource.contains("MixtapeScreen.ExclusionSettings") && appSource.contains("ExclusionSettingsScreen("),
        )
        assertTrue(
            "MixtapeScreen should define ExclusionSettings for navigation from Settings.",
            Regex("""enum\s+class\s+MixtapeScreen[\s\S]*ExclusionSettings""").containsMatchIn(viewModelSource),
        )
    }

    @Test
    fun dedicatedPageProvidesAddRemovePatternControlsAndExamples() {
        val exclusionSettingsBody = functionBody(appSource, "ExclusionSettingsScreen")
        assertTrue("Expected a dedicated ExclusionSettingsScreen composable.", exclusionSettingsBody.isNotBlank())
        assertTrue(
            "Exclusion settings should include an editable text field for one filename pattern.",
            exclusionSettingsBody.contains("OutlinedTextField") || exclusionSettingsBody.contains("TextField"),
        )
        assertTrue(
            "Exclusion settings should include an Add button/action for the typed pattern.",
            exclusionSettingsBody.contains("Add") && exclusionSettingsBody.contains("onAddFilenameExclusionPattern"),
        )
        assertTrue(
            "Exclusion settings should list current patterns from state.mixtapeExclusionSettings.filenamePatterns.",
            exclusionSettingsBody.contains("filenamePatterns") && exclusionSettingsBody.contains("mixtapeExclusionSettings"),
        )
        assertTrue(
            "Exclusion settings should include Remove controls for existing patterns.",
            exclusionSettingsBody.contains("Remove") && exclusionSettingsBody.contains("onRemoveFilenameExclusionPattern"),
        )
        assertTrue(
            "Help text should document examples for glob and contains-style matching.",
            exclusionSettingsBody.contains("*.tmp.mp3") &&
                exclusionSettingsBody.contains("*voice memo*") &&
                exclusionSettingsBody.contains("WhatsApp*.opus"),
        )
    }

    @Test
    fun mainActivityWiresPersistentStoreAndCallbacks() {
        assertTrue(
            "MainActivity should use SharedPreferencesMixtapeExclusionSettingsStore so exclusion patterns persist across app restarts.",
            mainActivitySource.contains("SharedPreferencesMixtapeExclusionSettingsStore"),
        )
        assertTrue(
            "MainActivity should pass the exclusion settings store into MixtapeViewModel.Factory.",
            mainActivitySource.contains("exclusionSettingsStore"),
        )
        assertTrue(
            "MainActivity should wire Settings -> Exclusion settings navigation into the ViewModel.",
            mainActivitySource.contains("onShowExclusionSettings = viewModel::showExclusionSettings"),
        )
        assertTrue(
            "MainActivity should wire add/remove exclusion pattern actions into the ViewModel.",
            mainActivitySource.contains("onAddFilenameExclusionPattern = viewModel::addFilenameExclusionPattern") &&
                mainActivitySource.contains("onRemoveFilenameExclusionPattern = viewModel::removeFilenameExclusionPattern"),
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
