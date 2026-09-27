package com.example.androidmixtape.ui
import java.io.File
import org.junit.Assert.*
import org.junit.Test
class TracklistDoubleClickUiContractTest {
    private fun source(name:String)=listOf(File("src/modern/java/com/example/androidmixtape/$name.kt"),File("app/src/modern/java/com/example/androidmixtape/$name.kt")).first{it.exists()}.readText()
    @Test fun sharedTrackListStillUsesNativeCueAndPlaybackActions() {
        assertTrue(source("MainActivity").contains("onTrackDoubleClick = viewModel::jumpToTrackWithCue"))
        assertTrue(source("ui/MixtapeApp").contains("onSelectTrack = onTrackDoubleClick"))
        val screen=source("ui/DemoPlayerScreen")
        assertTrue(screen.contains("onSelectTrack,onDelete,onRemove,onInfo"))
        assertEquals(1,Regex("DemoTrackList\\(").findAll(screen).count())
    }
    @Test fun demoAddsSingleTapSelectionWhileKeepingDoubleTapAndTrackActions() {
        val list=source("ui/DemoPackaging").substringAfter("internal fun DemoTrackList(")
        assertTrue(list.contains("onClick={onSelect(index,track)}"))
        assertTrue(list.contains("onDoubleClick={onSelect(index,track)}"))
        assertTrue(list.contains("onLongClick={menuTrack=track.id}"))
    }
}
