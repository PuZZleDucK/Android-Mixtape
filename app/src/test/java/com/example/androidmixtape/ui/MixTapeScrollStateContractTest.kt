package com.example.androidmixtape.ui
import java.io.File
import org.junit.Assert.*
import org.junit.Test
class MixTapeScrollStateContractTest {
    private fun source(name:String)=listOf(File("src/modern/java/com/example/androidmixtape/ui/$name.kt"),File("app/src/modern/java/com/example/androidmixtape/ui/$name.kt")).first{it.exists()}.readText()
    @Test fun appOwnsTheListStateAcrossEjectAndRotation() {
        val app=source("MixtapeApp")
        assertTrue(app.contains("val mixTapeListState = rememberLazyListState()"))
        assertTrue(app.contains("listState = mixTapeListState"))
    }
    @Test fun fullPageLandscapeLibraryUsesTwoColumnsAndHoistedGridState() {
        val app=source("MixtapeApp")
        assertTrue(app.contains("val mixTapeGridState = rememberLazyGridState()"))
        val idle=app.substringAfter("MixtapeScreen.MixTapes -> DemoTapeLibrary(").substringBefore("MixtapeScreen.NowPlaying -> NowPlaying(")
        assertTrue(idle.contains("listState = mixTapeListState"))
        assertTrue(idle.contains("gridState = mixTapeGridState"))
        val library=source("DemoPackaging").substringAfter("internal fun DemoTapeLibrary(").substringBefore("internal fun DemoTapeShelf(")
        assertTrue(library.contains("if(maxWidth>maxHeight)"))
        assertTrue(library.contains("columns=GridCells.Fixed(2),state=gridState"))
        assertTrue(library.contains("gridItemsIndexed(groups,key={_,group->group.stableKey})"))
        assertTrue(library.contains("DemoSpine("))
        assertTrue(library.contains("DemoTapeShelf(groups,currentIndex,listState"))
        assertTrue(library.contains("centerCurrent=false"))
        assertFalse(library.contains("rememberLazy"))
        assertFalse(source("DemoPlayerScreen").contains("DemoTapeLibrary("))
    }
    @Test fun shelfReceivesHoistedStateInsteadOfCreatingANewOne() {
        val shelf=source("DemoPackaging").substringAfter("internal fun DemoTapeShelf(").substringBefore("internal fun DemoTrackList(")
        assertTrue(shelf.contains("state:LazyListState"))
        assertTrue(shelf.contains("state=state"))
        assertFalse(shelf.contains("rememberLazyListState"))
    }
    @Test fun portraitAndLandscapeReuseExactlyTheSameListComposition() {
        val ui=source("DemoPlayerScreen")
        assertTrue(ui.contains("val list:@Composable (Modifier)->Unit"))
        assertEquals(1,Regex("DemoTapeShelf\\(").findAll(ui).count())
        assertTrue(ui.contains("list(Modifier.weight(1f).fillMaxHeight())"))
        assertTrue(ui.contains("list(Modifier.weight(1f).fillMaxWidth())"))
    }
}
