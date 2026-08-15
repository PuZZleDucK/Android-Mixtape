package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentThemePreviewUiContractTest {
    @Test
    fun everyComponentThemeSettingsPageProvidesAVisualPreview() {
        val source = mixtapeAppSource()

        listOf(
            "DeckThemeSettingsScreen(" to "DeckComponentPreview(",
            "CassetteThemeSettings" to "CassetteComponentPreview(cassetteTheme = theme",
            "ScrewThemeSettings" to "CassetteComponentPreview(screwTheme = theme",
            "StickerThemeSettings" to "CassetteComponentPreview(stickerTheme = theme",
            "CaseThemeSettings" to "CaseComponentPreview(caseTheme = theme",
            "SleeveThemeSettings" to "SleeveComponentPreview(sleeveTheme = theme",
        ).forEach { (screen, preview) ->
            assertTrue("$screen should provide a small component preview for every option.", source.contains(screen) && source.contains(preview))
        }
    }

    @Test
    fun genericThemeRowsRenderThePreviewBesideTheirControls() {
        val source = mixtapeAppSource()
        val body = source.substringAfter("private fun <T> ThemeToggleSettingsScreen(").substringBefore("@Composable\nprivate fun DeckComponentPreview(")

        assertTrue(body.contains("preview: @Composable (T, Modifier) -> Unit"))
        assertTrue(body.contains("preview(") && body.contains(".width(112.dp)") && body.contains(".height(64.dp)"))
        assertTrue(body.contains("Checkbox("))
    }

    private fun mixtapeAppSource(): String {
        val file = listOf(
            File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        ).firstOrNull { it.exists() }
        return requireNotNull(file) { "MixtapeApp.kt not found" }.readText()
    }
}
