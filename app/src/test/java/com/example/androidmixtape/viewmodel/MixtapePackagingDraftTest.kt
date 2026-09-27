package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.name.MixtapeNameSource
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MixtapePackagingDraftTest {
    @get:Rule val dispatcher=MainDispatcherRule()
    private fun draft(group:MixTapeGroup):MixtapeCustomization {
        val p=group.visualProperties
        return MixtapeCustomization(group.name,p.decorativeId,p.handwritingFont,p.embellishment,p.symbolColor,p.nameColor,
            p.cassetteTheme,p.screwTheme,p.stickerTheme,p.caseTheme,p.sleeveTheme,p.sleeveInk,
            p.spineTextAlignment,p.spineSymbolPlacement,p.jitterStartIndex)
    }
    @Test fun diceChangesNameAndEveryPackagingFieldButLeavesCassetteAlone() = runTest {
        val vm=fixture();vm.onPermissionResult(true)
        val group=vm.uiState.value.mixTapeGroups.first();val old=draft(group)
        repeat(30) {
            val roll=vm.randomizeMixtapePackaging(group.stableKey,old)
            assertNotEquals(old.name,roll.name);assertNotEquals(old.decorativeId,roll.decorativeId)
            assertNotEquals(old.handwritingFont,roll.handwritingFont);assertNotEquals(old.jitterStartIndex,roll.jitterStartIndex)
            assertNotEquals(old.embellishment,roll.embellishment);assertNotEquals(old.nameColor,roll.nameColor)
            assertNotEquals(old.symbolColor,roll.symbolColor);assertNotEquals(roll.nameColor,roll.symbolColor)
            assertNotEquals(old.spineTextAlignment,roll.spineTextAlignment);assertNotEquals(old.spineSymbolPlacement,roll.spineSymbolPlacement)
            assertNotEquals(old.caseTheme,roll.caseTheme);assertNotEquals(old.sleeveTheme,roll.sleeveTheme)
            assertNotEquals(old.sleeveInk,roll.sleeveInk)
            assertEquals(old.cassetteTheme,roll.cassetteTheme);assertEquals(old.screwTheme,roll.screwTheme);assertEquals(old.stickerTheme,roll.stickerTheme)
            assertTrue(roll.decorativeId.matches(Regex("[A-Z0-9]{1,2}")))
        }
    }
    @Test fun rollingAndCancellingDoesNotMutateSavedTapesOrPlayback() = runTest {
        val vm=fixture();vm.onPermissionResult(true);vm.selectMixTapeGroup(0);vm.seekTo(12_000)
        val before=vm.uiState.value
        val group=before.mixTapeGroups.last()
        repeat(10) { vm.randomizeMixtapePackaging(group.stableKey,draft(group)) }
        assertEquals(before,vm.uiState.value)
        vm.refresh()
        assertEquals(before.mixTapeGroups,vm.uiState.value.mixTapeGroups)
    }
    @Test fun savingAnUnloadedSpineChangesOnlyThatTapeAndSurvivesReload() = runTest {
        val store=InMemoryMixtapeVisualPropertiesStore()
        val vm=fixture(store);vm.onPermissionResult(true);vm.selectMixTapeGroup(0);vm.seekTo(21_000)
        val before=vm.uiState.value;val target=before.mixTapeGroups.last()
        val rolled=vm.randomizeMixtapePackaging(target.stableKey,draft(target))
        vm.updateMixtapeCustomization(target.stableKey,rolled)
        val after=vm.uiState.value
        assertEquals(before.currentMixtapeStableKey,after.currentMixtapeStableKey)
        assertEquals(before.currentMixtapeVisualProperties,after.currentMixtapeVisualProperties)
        assertEquals(before.queueTracks,after.queueTracks);assertEquals(before.positionMs,after.positionMs)
        assertEquals(before.currentIndex,after.currentIndex);assertEquals(before.isPlaying,after.isPlaying)
        assertEquals(before.mixTapeGroups.dropLast(1),after.mixTapeGroups.dropLast(1))
        assertEquals(rolled,draft(after.mixTapeGroups.last()))
        vm.refresh();assertEquals(rolled,draft(vm.uiState.value.mixTapeGroups.last()))
        assertEquals(after.mixTapeGroups.last().visualProperties,store.propertiesFor(target.stableKey))
    }
    @Test fun diceHonoursEnabledSetsEvenWhenOnlyOneOptionRemains() {
        val p=MixtapeVisualProperties();val old=draft(MixTapeGroup("A",emptyList(),0,p))
        val new=old.randomizedPackaging(Random(7),"B",listOf(p.handwritingFont),listOf(p.embellishment),listOf(p.caseTheme),listOf(p.sleeveTheme))
        assertEquals(p.handwritingFont,new.handwritingFont);assertEquals(p.embellishment,new.embellishment)
        assertEquals(p.caseTheme,new.caseTheme);assertEquals(p.sleeveTheme,new.sleeveTheme)
        assertEquals("B",new.name)
    }
    @Test fun generatedNamesAvoidOtherTapesAndUnknownKeysAreNoOps() = runTest {
        val vm=fixture();vm.onPermissionResult(true)
        val groups=vm.uiState.value.mixTapeGroups;val old=draft(groups.first())
        val otherNames=groups.drop(1).map { it.name }.toSet()
        repeat(100) { assertFalse(vm.randomizeMixtapePackaging(groups.first().stableKey,old).name in otherNames) }
        val before=vm.uiState.value
        assertEquals(old,vm.randomizeMixtapePackaging("missing",old))
        vm.updateMixtapeCustomization("missing",old.copy(name="Wrong tape"))
        assertEquals(before,vm.uiState.value)
    }
    private fun fixture(store:MixtapeVisualPropertiesStore=InMemoryMixtapeVisualPropertiesStore()):MixtapeViewModel {
        val tracks=(1..180).map { Track(it.toLong(),"Song $it","Artist",180_000,"content://song/$it") }
        return MixtapeViewModel(repository=object:AudioRepository {override suspend fun loadTracks()=tracks},
            controller=MixtapeController(FakePlayerEngine()),mixtapeRandom=Random(45),visualPropertiesStore=store,
            nameSource=object:MixtapeNameSource {override fun names()=(1..200).map { "Album $it" }})
    }
}
