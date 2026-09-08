package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Planning guardrails for card 4834. Runtime gesture/back tests remain required. */
class NowPlayingSettingsShortcutContractTest {
    private val source: String
        get() = listOf(
            File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        ).first { it.exists() }.readText()

    private fun body(name: String): String = source.substringAfter("private fun $name(")
        .substringBefore("\n@Composable")

    @Test fun toggleUsesOneCombinedGestureTargetWithLabeledLongPress() {
        val toggle = body("DeckViewToggle")
        assertTrue("Use combinedClickable on the existing toggle, not a second gear hit target",
            toggle.contains(".combinedClickable("))
        assertTrue("Expose a Settings long-click label for accessibility",
            toggle.contains("onLongClickLabel") && toggle.contains("Settings"))
        assertTrue("Keep short-click and long-click callbacks separate",
            toggle.contains("onLongClick =") && toggle.contains("onClick ="))
    }

    @Test fun nowPlayingThreadsSettingsActionToSharedPortraitLandscapeFooter() {
        for (name in listOf("NowPlaying", "NowPlayingModeToggleAndSpine", "DeckViewToggle")) {
            assertTrue("$name must receive the Settings callback", body(name).contains("onShowSettings"))
        }
    }

    @Test fun settingsBackMustNotBeHardwiredToLibrary() {
        assertTrue("Root Settings system Back must return to its origin, not always MixTapes",
            !source.contains("if (state.screen == MixtapeScreen.Settings) onBackToMixTapes()"))
        val settingsBranch = source.substringAfter("MixtapeScreen.Settings -> SettingsScreen(")
            .substringBefore("MixtapeScreen.Help")
        assertTrue("Settings toolbar Back must also use origin-aware navigation",
            !settingsBranch.contains("onBackToMixTapes = onBackToMixTapes"))
    }
}
