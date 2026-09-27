package com.example.androidmixtape.ui

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.viewmodel.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    @get:Rule val composeRule = createComposeRule()
    private val tracks = (1..9).map { Track(it.toLong(), "Song $it", "Artist", 180_000, "content://test/$it") }
    private val groups = buildMixTapeGroups(tracks, 3)
    private var state by mutableStateOf(MixtapeUiState(
        status = LibraryStatus.Ready, screen = MixtapeScreen.Settings, tracks = tracks, mixTapeGroups = groups,
        mixtapeNameInfos = listOf("Road songs", "Night bus", "Sunday mix").mapIndexed { index, name -> MixtapeNameInfo(groups[index].stableKey, name) },
    ))
    private var viewport by mutableStateOf(360.dp to 620.dp)
    private var fontScale by mutableStateOf(1f)
    private var darkTheme by mutableStateOf(false)
    private val events = mutableListOf<String>()

    private fun open(screen: MixtapeScreen) { state = state.copy(screen = screen) }
    private fun showSettings() {
        composeRule.setContent {
            val density = LocalDensity.current
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale), LocalConfiguration provides configuration) {
                Box(Modifier.requiredSize(viewport.first, viewport.second)) {
                    AndroidMixtapeTheme {
                        MixtapeApp(
                            state = state,
                            onRequestPermission = {}, onRefresh = {}, onTogglePlayPause = {}, onPrevious = {}, onNext = {}, onSeekTo = {},
                            onBackToMixTapes = { events += "back"; open(MixtapeScreen.MixTapes) },
                            onShowSettings = { open(MixtapeScreen.Settings) },
                            onShowHelp = { open(MixtapeScreen.Help) },
                            onShowMixtapeNames = { open(MixtapeScreen.MixtapeNames) },
                            onShowMixtapeSymbolSettings = { open(MixtapeScreen.MixtapeSymbolSettings) },
                            onShowHandwritingFontSettings = { open(MixtapeScreen.HandwritingFontSettings) },
                            onShowDeckThemeSettings = { open(MixtapeScreen.DeckThemeSettings) },
                            onShowCassetteThemeSettings = { open(MixtapeScreen.CassetteThemeSettings) },
                            onShowScrewThemeSettings = { open(MixtapeScreen.ScrewThemeSettings) },
                            onShowStickerThemeSettings = { open(MixtapeScreen.StickerThemeSettings) },
                            onShowCaseThemeSettings = { open(MixtapeScreen.CaseThemeSettings) },
                            onShowSleeveThemeSettings = { open(MixtapeScreen.SleeveThemeSettings) },
                            onShowExclusionSettings = { open(MixtapeScreen.ExclusionSettings) },
                            onResetAllMixTapes = { events += "reset" },
                            onSongsPerMixTapeChange = { state = state.copy(mixtapeSettings = state.mixtapeSettings.copy(songsPerMixTape = it)) },
                            onArtistGroupingChange = { state = state.copy(mixtapeSettings = state.mixtapeSettings.copy(artistGrouping = it)) },
                            onHandwritingMessinessChange = { state = state.copy(mixtapeSettings = state.mixtapeSettings.copy(handwritingMessiness = it)) },
                            onDeckThemeChange = { events += "deck:$it"; state = state.copy(mixtapeThemeSettings = state.mixtapeThemeSettings.copy(deckTheme = it)) },
                            onCassetteThemeEnabledChange = { item, enabled -> events += "cassette:$item:$enabled" },
                            onScrewThemeEnabledChange = { item, enabled -> events += "screw:$item:$enabled" },
                            onStickerThemeEnabledChange = { item, enabled -> events += "sticker:$item:$enabled" },
                            onCaseThemeEnabledChange = { item, enabled -> events += "case:$item:$enabled" },
                            onSleeveThemeEnabledChange = { item, enabled -> events += "sleeve:$item:$enabled" },
                            onMixtapeEmbellishmentEnabledChange = { item, enabled -> events += "symbol:$item:$enabled" },
                            onMixtapeHandwritingFontEnabledChange = { item, enabled -> events += "font:$item:$enabled" },
                            onEditMixtapeName = { key, name ->
                                events += "edit:$key:$name"
                                state = state.copy(mixtapeNameInfos = state.mixtapeNameInfos.map { if (it.stableKey == key) it.copy(name = name) else it })
                            },
                            onRegenerateMixtapeName = { events += "randomize:$it" },
                            onAddFilenameExclusionPattern = { pattern ->
                                events += "add:$pattern"
                                state = state.copy(mixtapeExclusionSettings = MixtapeExclusionSettings(state.mixtapeExclusionSettings.filenamePatterns + pattern))
                            },
                            onRemoveFilenameExclusionPattern = { pattern ->
                                events += "remove:$pattern"
                                state = state.copy(mixtapeExclusionSettings = MixtapeExclusionSettings(state.mixtapeExclusionSettings.filenamePatterns - pattern))
                            },
                        )
                    }
                }
            }
        }
    }

    private fun link(label: String) = composeRule.onNodeWithText(label).performScrollTo().performClick()
    private fun option(label: String): SemanticsNodeInteraction {
        composeRule.onNodeWithContentDescription("Settings options").performScrollToNode(hasText(label))
        return composeRule.onNodeWithText(label)
    }
    private fun readable(value: Any) = value.toString().replace(Regex("(?<=.)(?=\\p{Upper})"), " ")

    @Test fun everyExistingSettingsDestinationRemainsReachableAndCategorySurvivesBack() {
        showSettings()
        val routes = listOf(
            Triple("Mixtapes", "Mixtape names", MixtapeScreen.MixtapeNames),
            Triple("Appearance", "Deck theme", MixtapeScreen.DeckThemeSettings),
            Triple("Appearance", "Cassette themes", MixtapeScreen.CassetteThemeSettings),
            Triple("Appearance", "Screw themes", MixtapeScreen.ScrewThemeSettings),
            Triple("Appearance", "Sticker themes", MixtapeScreen.StickerThemeSettings),
            Triple("Appearance", "Case themes", MixtapeScreen.CaseThemeSettings),
            Triple("Appearance", "Sleeve themes", MixtapeScreen.SleeveThemeSettings),
            Triple("Appearance", "Handwriting fonts", MixtapeScreen.HandwritingFontSettings),
            Triple("Appearance", "Mixtape symbols", MixtapeScreen.MixtapeSymbolSettings),
            Triple("Library", "Filename exclusions", MixtapeScreen.ExclusionSettings),
            Triple("Library", "Android Auto help", MixtapeScreen.Help),
        )
        routes.forEach { (category, label, destination) ->
            composeRule.onNodeWithText(category).performClick()
            link(label)
            composeRule.runOnIdle { assertEquals(destination, state.screen) }
            composeRule.onNodeWithContentDescription("Back to Settings").assertIsDisplayed().performClick()
            composeRule.onNodeWithText(category).assertIsSelected()
        }
    }

    @Test fun androidBackReturnsThroughSettingsWithoutExitingTheApp() {
        showSettings()
        composeRule.onNodeWithText("Appearance").performClick()
        link("Deck theme")
        androidx.test.espresso.Espresso.pressBack()
        composeRule.onNodeWithText("Appearance").assertIsSelected()
        androidx.test.espresso.Espresso.pressBack()
        composeRule.runOnIdle {
            assertEquals(MixtapeScreen.MixTapes, state.screen)
            assertEquals(listOf("back"), events)
        }
    }

    @Test fun tapeLengthArtistGroupingAndMessinessKeepAllChoices() {
        showSettings()
        listOf("Normal (24 tracks)" to 24, "LP (56 tracks)" to 56, "XLP (112 tracks)" to 112, "Mega (256 tracks)" to 256).forEach { (label, count) ->
            link(label)
            composeRule.onNodeWithText(label).assertIsSelected()
            composeRule.runOnIdle { assertEquals(count, state.mixtapeSettings.songsPerMixTape) }
        }
        listOf("No artist grouping" to ArtistGrouping.NoGrouping, "Keep artist together" to ArtistGrouping.KeepArtistTogether,
            "Artist triplets on different tapes" to ArtistGrouping.ArtistTripletsAcrossTapes).forEach { (label, grouping) ->
            link(label)
            composeRule.onNodeWithText(label).assertIsSelected()
            composeRule.runOnIdle { assertEquals(grouping, state.mixtapeSettings.artistGrouping) }
        }
        composeRule.onNodeWithText("Appearance").performClick()
        HandwritingMessiness.entries.forEach { level ->
            link(level.name)
            composeRule.onNodeWithText(level.name).assertIsSelected()
            composeRule.runOnIdle { assertEquals(level, state.mixtapeSettings.handwritingMessiness) }
        }
    }

    @Test fun componentCardsKeepPreviewsAndEachToggleFiresOnce() {
        showSettings()
        val cases = listOf(
            Triple(MixtapeScreen.CassetteThemeSettings, CassetteTheme.entries.first(), "cassette"),
            Triple(MixtapeScreen.ScrewThemeSettings, ScrewTheme.entries.first(), "screw"),
            Triple(MixtapeScreen.StickerThemeSettings, StickerTheme.entries.first(), "sticker"),
            Triple(MixtapeScreen.CaseThemeSettings, CaseTheme.entries.first(), "case"),
            Triple(MixtapeScreen.SleeveThemeSettings, SleeveTheme.entries.first(), "sleeve"),
            Triple(MixtapeScreen.MixtapeSymbolSettings, MixtapeEmbellishment.entries.first(), "symbol"),
            Triple(MixtapeScreen.HandwritingFontSettings, MixtapeHandwritingFont.entries.first(), "font"),
        )
        cases.forEach { (screen, item, prefix) ->
            composeRule.runOnIdle { open(screen); events.clear() }
            option(readable(item)).assertIsOn().performClick()
            composeRule.runOnIdle { assertEquals(listOf("$prefix:$item:false"), events) }
        }
        composeRule.runOnIdle { open(MixtapeScreen.DeckThemeSettings); events.clear() }
        val deck = DeckTheme.entries.last()
        option(readable(deck)).performClick()
        option(readable(deck)).assertIsSelected()
        composeRule.runOnIdle { assertEquals(listOf("deck:$deck"), events) }
    }

    @Test fun lastEnabledComponentSymbolAndFontCannotBeTurnedOff() {
        state = state.copy(
            mixtapeThemeSettings = state.mixtapeThemeSettings.copy(enabledCassetteThemes = setOf(CassetteTheme.entries.first())),
            mixtapeSymbolSettings = MixtapeSymbolSettings(setOf(MixtapeEmbellishment.entries.first())),
            mixtapeHandwritingFontSettings = MixtapeHandwritingFontSettings(setOf(MixtapeHandwritingFont.entries.first())),
        )
        showSettings()
        listOf(
            MixtapeScreen.CassetteThemeSettings to CassetteTheme.entries.first(),
            MixtapeScreen.MixtapeSymbolSettings to MixtapeEmbellishment.entries.first(),
            MixtapeScreen.HandwritingFontSettings to MixtapeHandwritingFont.entries.first(),
        ).forEach { (screen, item) ->
            composeRule.runOnIdle { open(screen) }
            option(readable(item)).assertIsOn().assertIsNotEnabled()
        }
        composeRule.runOnIdle { assertTrue(events.isEmpty()) }
    }

    @Test fun namesCanBeSearchedEditedAndRandomizedWithoutLosingTheirKeys() {
        open(MixtapeScreen.MixtapeNames)
        showSettings()
        composeRule.onNodeWithText("Find a mixtape").performTextInput("Night")
        composeRule.onNodeWithText("Road songs").assertDoesNotExist()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        composeRule.onNodeWithContentDescription("Edit name for Night bus").performScrollTo().performClick()
        composeRule.onNodeWithText("Mixtape name").performTextReplacement("")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        composeRule.onNodeWithText("Mixtape name").performTextReplacement("Night train")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithContentDescription("Randomize name for Night train").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("edit:${groups[1].stableKey}:Night train", "randomize:${groups[1].stableKey}"), events)
        }
    }

    @Test fun filenamePatternsStillSupportAddRemoveAndBlankInputGuard() {
        open(MixtapeScreen.ExclusionSettings)
        showSettings()
        composeRule.onNodeWithText("Add").assertIsNotEnabled()
        composeRule.onNodeWithText("Filename pattern").performTextInput("*voice memo*")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        link("Add")
        composeRule.onNodeWithContentDescription("Remove pattern *voice memo*").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("add:*voice memo*", "remove:*voice memo*"), events)
            assertTrue(state.mixtapeExclusionSettings.filenamePatterns.isEmpty())
        }
    }

    @Test fun resetRequiresConfirmationCanBeCancelledAndEmptyLibraryCanGoBack() {
        showSettings()
        composeRule.onNodeWithText("Library").performClick()
        link("Reset all mixtapes")
        composeRule.runOnIdle { assertTrue(events.isEmpty()) }
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle { assertTrue(events.isEmpty()) }
        link("Reset all mixtapes")
        composeRule.onNodeWithText("Reset mixtapes").performClick()
        composeRule.runOnIdle { assertEquals(listOf("reset"), events); state = state.copy(mixTapeGroups = emptyList()) }
        composeRule.onNodeWithText("Reset all mixtapes").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.runOnIdle { assertEquals("back", events.last()) }
    }

    @Test fun portraitLandscapeAndLargeTextKeepNavigationAndControlsReachable() {
        showSettings()
        saveScreenshot("settings-mixtapes-portrait")
        composeRule.onNodeWithText("Appearance").performClick()
        saveScreenshot("settings-appearance-portrait")
        composeRule.runOnIdle { viewport = 900.dp to 360.dp }
        saveScreenshot("settings-appearance-landscape")
        link("Deck theme")
        saveScreenshot("settings-decks-landscape")
        option(readable(DeckTheme.entries.last())).assertIsDisplayed()
        composeRule.runOnIdle { viewport = 320.dp to 560.dp; fontScale = 1.4f }
        option(readable(DeckTheme.entries.last())).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back to Settings").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Library").assertIsDisplayed().performClick()
        link("Reset all mixtapes")
        composeRule.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        composeRule.runOnIdle { open(MixtapeScreen.HandwritingFontSettings); viewport = 900.dp to 440.dp; fontScale = 1f }
        saveScreenshot("settings-fonts-landscape")
        option(readable(MixtapeHandwritingFont.entries.last())).assertIsDisplayed()
        composeRule.runOnIdle { darkTheme = true; open(MixtapeScreen.Settings) }
        composeRule.onNodeWithText("Appearance").performClick()
        saveScreenshot("settings-appearance-dark")
        link("Handwriting fonts")
        option(readable(MixtapeHandwritingFont.entries.first())).assertIsOn()
    }

    private fun saveScreenshot(name: String) {
        composeRule.waitForIdle()
        android.os.SystemClock.sleep(700)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
