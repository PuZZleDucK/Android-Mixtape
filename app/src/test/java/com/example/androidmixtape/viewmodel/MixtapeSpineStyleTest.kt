package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MixtapeSpineStyleTest {
    @get:Rule val dispatcher=MainDispatcherRule()
    @Test fun automaticStylesAreStableAndCoverAllChoicesWithIndependentInks() {
        val styles=(0 until 5_000).map(::initialSpineStyle)
        assertEquals(SpineTextAlignment.entries.toSet(),styles.map { it.alignment }.toSet())
        assertEquals(SpineSymbolPlacement.entries.toSet(),styles.map { it.symbolPlacement }.toSet())
        assertEquals(MixtapeSymbolColor.entries.toSet(),styles.map { it.nameColor }.toSet())
        assertEquals(MixtapeSymbolColor.entries.toSet(),styles.map { it.symbolColor }.toSet())
        styles.forEachIndexed { seed,style ->
            assertNotEquals(style.nameColor,style.symbolColor)
            assertEquals(style,initialSpineStyle(seed))
            assertEquals(style,initialSpineStyle(seed+5_000))
        }
        assertEquals(initialSpineStyle(4_999),initialSpineStyle(-1))
    }
    @Test fun migrationChangesOnlyTheNewLayoutAndUnchosenDefaultInks() {
        val original=MixtapeVisualProperties(jitterStartIndex=971,handwritingFont=MixtapeHandwritingFont.Caveat,
            cassetteTheme=CassetteTheme.GhostClear,caseTheme=CaseTheme.CrystalClear,sleeveTheme=SleeveTheme.GraphPaper)
        val migrated=original.withInitialSpineStyle()
        assertNotEquals(migrated.nameColor,migrated.symbolColor)
        assertEquals(original,migrated.copy(nameColor=original.nameColor,symbolColor=original.symbolColor,
            spineTextAlignment=original.spineTextAlignment,spineSymbolPlacement=original.spineSymbolPlacement))
    }
    @Test fun migrationRespectsPreviouslyCustomizedInks() {
        for(name in MixtapeSymbolColor.entries)for(symbol in MixtapeSymbolColor.entries) {
            if(name==MixtapeSymbolColor.Navy&&symbol==MixtapeSymbolColor.Navy)continue
            val original=MixtapeVisualProperties(jitterStartIndex=237,nameColor=name,symbolColor=symbol)
            val migrated=original.withInitialSpineStyle()
            assertEquals(name,migrated.nameColor);assertEquals(symbol,migrated.symbolColor)
        }
    }
    @Test fun newTapesSaveTheirVarietyAndDoNotRerollOnReload() = runTest {
        val store=InMemoryMixtapeVisualPropertiesStore()
        val vm=create(store);vm.onPermissionResult(true)
        val groups=vm.uiState.value.mixTapeGroups
        for(group in groups) {
            val p=group.visualProperties;val style=initialSpineStyle(p.jitterStartIndex)
            assertEquals(style.alignment,p.spineTextAlignment)
            assertEquals(style.symbolPlacement,p.spineSymbolPlacement)
            assertNotEquals(p.nameColor,p.symbolColor)
            assertEquals(p,store.propertiesFor(group.stableKey))
        }
        val reloaded=create(store);reloaded.onPermissionResult(true)
        assertEquals(groups.map { it.visualProperties },reloaded.uiState.value.mixTapeGroups.map { it.visualProperties })
    }
    @Test fun chosenAlignmentPlacementAndMatchingManualInksSurviveEditingAndReload() = runTest {
        val store=InMemoryMixtapeVisualPropertiesStore();val vm=create(store)
        vm.onPermissionResult(true);vm.selectMixTapeGroup(0);vm.seekTo(12_000)
        val before=vm.uiState.value;val p=before.currentMixtapeVisualProperties
        vm.updateCurrentMixtapeCustomization(MixtapeCustomization(
            name="My lettering",decorativeId=p.decorativeId,handwritingFont=p.handwritingFont,
            embellishment=p.embellishment,symbolColor=MixtapeSymbolColor.Navy,nameColor=MixtapeSymbolColor.Navy,
            cassetteTheme=p.cassetteTheme,screwTheme=p.screwTheme,stickerTheme=p.stickerTheme,
            caseTheme=p.caseTheme,sleeveTheme=p.sleeveTheme,sleeveInk=p.sleeveInk,
            spineTextAlignment=SpineTextAlignment.Right,spineSymbolPlacement=SpineSymbolPlacement.LowerLeft))
        val after=vm.uiState.value
        assertEquals(before.queueTracks,after.queueTracks);assertEquals(before.positionMs,after.positionMs)
        assertEquals(before.isPlaying,after.isPlaying)
        assertEquals(SpineTextAlignment.Right,after.currentMixtapeVisualProperties.spineTextAlignment)
        assertEquals(SpineSymbolPlacement.LowerLeft,after.currentMixtapeVisualProperties.spineSymbolPlacement)
        val reloaded=create(store);reloaded.onPermissionResult(true)
        assertEquals(after.currentMixtapeVisualProperties,reloaded.uiState.value.mixTapeGroups.first().visualProperties)
    }
    @Test fun preferenceSchemaKeepsThemesAndSavesLayoutAsStableStrings() {
        val source=listOf(File("src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt"),File("app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt")).first { it.exists() }.readText()
        for(key in listOf("spine_text_alignment","spine_symbol_placement","spine_style_version"))assertTrue(source.contains(key))
        assertTrue(source.contains("if (!preferences.contains(prefKey(stableMixtapeKey, \"spine_style_version\")))"))
        assertTrue(source.contains("properties.withInitialSpineStyle().also { saveProperties(stableMixtapeKey,it) }"))
        assertTrue(source.contains("THEME_SCHEMA = 2"))
    }
    private fun create(store:MixtapeVisualPropertiesStore):MixtapeViewModel {
        val tracks=(1..120).map { Track(it.toLong(),"Song $it","Artist",180_000,"content://song/$it") }
        return MixtapeViewModel(repository=object:AudioRepository {override suspend fun loadTracks()=tracks},
            controller=MixtapeController(FakePlayerEngine()),visualPropertiesStore=store,mixtapeRandom=Random(23))
    }
}
