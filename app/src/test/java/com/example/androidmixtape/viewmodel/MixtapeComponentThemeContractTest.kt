package com.example.androidmixtape.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeComponentThemeContractTest {
    @Test
    fun defaultSettingsEnableEveryRandomizableComponentTheme() {
        val settings = MixtapeThemeSettings()

        assertEquals(CassetteTheme.entries.toSet(), settings.enabledCassetteThemes)
        assertEquals(ScrewTheme.entries.toSet(), settings.enabledScrewThemes)
        assertEquals(StickerTheme.entries.toSet(), settings.enabledStickerThemes)
        assertEquals(CaseTheme.entries.toSet(), settings.enabledCaseThemes)
        assertEquals(SleeveTheme.entries.toSet(), settings.enabledSleeveThemes)
    }

    @Test
    fun inMemoryThemeStorePersistsGlobalDeckAndAllowLists() {
        val store = InMemoryMixtapeThemeSettingsStore()
        val expected = MixtapeThemeSettings(
            deckTheme = DeckTheme.SafetyYellow,
            enabledCassetteThemes = setOf(CassetteTheme.GhostClear),
            enabledScrewThemes = setOf(ScrewTheme.Dark),
            enabledStickerThemes = setOf(StickerTheme.PrismC60),
            enabledCaseThemes = setOf(CaseTheme.VioletClear),
            enabledSleeveThemes = setOf(SleeveTheme.GraphPaper),
        )

        store.saveSettings(expected)

        assertEquals(expected, store.settings())
    }

    @Test
    fun visualPropertiesCarryStableIndependentComponentsAndDecorativeId() {
        val properties = MixtapeVisualProperties(
            decorativeId = "42",
            cassetteTheme = CassetteTheme.TranslucentCobalt,
            screwTheme = ScrewTheme.Dark,
            stickerTheme = StickerTheme.HissTachiLoNoise,
            caseTheme = CaseTheme.ElectricBlueClear,
            sleeveTheme = SleeveTheme.BlueprintGrid,
        )

        assertEquals("42", properties.decorativeId)
        assertEquals(CassetteTheme.TranslucentCobalt, properties.cassetteTheme)
        assertEquals(ScrewTheme.Dark, properties.screwTheme)
        assertEquals(StickerTheme.HissTachiLoNoise, properties.stickerTheme)
        assertEquals(CaseTheme.ElectricBlueClear, properties.caseTheme)
        assertEquals(SleeveTheme.BlueprintGrid, properties.sleeveTheme)
        assertTrue(DeckTheme.entries.size >= 4)
    }
}
