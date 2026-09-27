package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Contracts revised for the approved native demo composition, not the retired footer preview. */
class NowPlayingMixTapeToggleContractTest {
    private fun source(name:String)=listOf(File("src/modern/java/com/example/androidmixtape/ui/$name.kt"),File("app/src/modern/java/com/example/androidmixtape/ui/$name.kt")).first{it.exists()}.readText()
    @Test fun integratedKeySwitchesListsAndRetainsSettingsLongPress() {
        val deck=source("DemoDeck")
        assertTrue(deck.contains("Show track list")&&deck.contains("Show tape list"))
        assertTrue(deck.contains("onToggleList")&&deck.contains("onLongClick=if(index==5)onSettings"))
        val app=source("MixtapeApp").substringAfter("private fun NowPlaying(").substringBefore("private fun MixtapeCustomizationDialog(")
        assertTrue(app.contains("onBodyModeChange(if (showTapes) NowPlayingBodyMode.Tracks else NowPlayingBodyMode.MixTapes)"))
    }
    @Test fun selectingAnyTapeReturnsToItsTracklist() {
        val app=source("MixtapeApp").substringAfter("fun selectTape(index: Int)").substringBefore("BackHandler")
        assertTrue(app.indexOf("onBodyModeChange(NowPlayingBodyMode.Tracks)") < app.indexOf("onMixTapeGroupClick(index)"))
        assertTrue(source("DemoPackaging").contains("onClick={onSelect(index)}"))
    }
    @Test fun idleLibraryHasFullPageShelfAndOnlyLoadedRouteComposesPlayer() {
        val app=source("MixtapeApp")
        val idle=app.substringAfter("MixtapeScreen.MixTapes -> DemoTapeLibrary(").substringBefore("MixtapeScreen.NowPlaying -> NowPlaying(")
        assertTrue(app.contains("MixtapeScreen.MixTapes -> DemoTapeLibrary("))
        assertTrue(app.contains("MixtapeScreen.NowPlaying -> NowPlaying("))
        assertTrue(source("DemoPackaging").contains("centerCurrent=false"))
        assertTrue(idle.contains("Modifier.weight(1f).fillMaxWidth()"))
        assertTrue(idle.contains("bodyMode = NowPlayingBodyMode.Tracks"))
        assertFalse(idle.contains("DemoDeck(")||idle.contains("DemoPlayerScreen("))
        assertTrue(source("DemoPlayerScreen").contains("if(showTapes) DemoTapeShelf"))
        assertTrue(source("DemoPlayerScreen").contains("DemoTrackList("))
    }
    @Test fun shelfCentersCurrentOnEntryButNotOnPlaybackTicks() {
        val shelf=source("DemoPackaging").substringAfter("internal fun DemoTapeShelf(").substringBefore("internal fun DemoTrackList(")
        assertTrue(shelf.contains("LaunchedEffect(currentIndex,maxHeight,maxWidth,groups.size,centerCurrent,density)"))
        assertTrue(shelf.contains("state.scrollToItem(target.index,target.offset)"))
        assertTrue(shelf.contains("demoShelfScrollTarget("))
        assertTrue(shelf.contains("contentPadding=PaddingValues(4.dp)"))
        assertFalse(shelf.contains("(maxHeight-rowHeight)/2"))
        assertFalse(shelf.contains("positionMs"))
    }
    @Test fun onlyPlaylistHasSeparateCurrentSpineAndNeitherHasHeaders() {
        val ui=source("DemoPlayerScreen")
        assertTrue(ui.indexOf("if(showTapes) DemoTapeShelf")<ui.indexOf("else Column"))
        assertTrue(ui.indexOf("else Column")<ui.indexOf("DemoSpine("))
        assertFalse(ui.contains("CurrentTrackPreview("))
        assertFalse(ui.contains("Text("))
        val deck=source("DemoDeck")
        assertFalse(deck.contains("demoText(style.name"))
        assertFalse(deck.contains("Flip")||deck.contains("SIDE A")||deck.contains("SIDE B"))
    }
    @Test fun transportOrderFilledGlyphsAndCompactSpacingFollowPhoneReview() {
        val deck=source("DemoDeck")
        assertTrue(deck.contains("val callbacks=listOf(onEject,onPrevious,onStop,onPlayPause,onNext,onToggleList)"))
        assertTrue(deck.contains("pressed=(index==3&&playing)"))
        assertTrue(deck.contains("val button=if(index==3)accent"))
        val symbols=deck.substringAfter("when(index) {").substringBefore("5 -> translate")
        assertFalse("Transport glyphs are filled, not outlined",symbols.contains("style=Stroke"))
        assertTrue(source("DemoPlayerScreen").contains("Arrangement.spacedBy(6.dp)"))
        val tracks=source("DemoPackaging").substringAfter("internal fun DemoTrackList(")
        assertTrue(tracks.contains("heightIn(min=26.dp)"))
        assertTrue(tracks.contains("padding(horizontal=4.dp,vertical=1.dp)"))
        assertTrue(tracks.contains("maxLines=1,overflow=TextOverflow.Ellipsis"))
        assertTrue(tracks.contains("verticalInkBounds=inkBounds,fixedRowHeight=rowHeight"))
        assertTrue(tracks.contains("fontSize=32.sp"))
    }
    @Test fun trackRowsAreTitlesOnlyAndShareTheSpineMaterial() {
        val paper=source("DemoPackaging")
        val tracks=paper.substringAfter("internal fun DemoTrackList(")
        assertTrue(tracks.contains("JitteredHandwritingText(track.title"))
        assertFalse(tracks.contains("track.artist")||tracks.contains("durationMs"))
        assertEquals(2,Regex("themes.sleeve\\(properties.sleeveTheme\\)").findAll(paper).count())
        assertTrue(tracks.contains("DemoCaseSurface(themes.case(properties.caseTheme)"))
    }
}
