package com.example.androidmixtape.ui
import java.io.File
import org.junit.Assert.*
import org.junit.Test
class TrackLongPressMenuContractTest {
    private fun source(name:String)=listOf(File("src/modern/java/com/example/androidmixtape/ui/$name.kt"),File("app/src/modern/java/com/example/androidmixtape/ui/$name.kt")).first{it.exists()}.readText()
    @Test fun nativeTrackRowsKeepLongPressActionMenuWithExactLabels() {
        val body=source("DemoPackaging").substringAfter("internal fun DemoTrackList(")
        assertTrue(body.contains("combinedClickable")&&body.contains("onLongClick={menuTrack=track.id}"))
        assertTrue(body.contains("DropdownMenu(")&&body.contains("DropdownMenuItem("))
        listOf("Delete from device","Remove from mixtape","Track info").forEach { label ->
            assertEquals(1,Regex("Text\\(\"${Regex.escape(label)}\"").findAll(body).count())
        }
        assertTrue(body.contains("Track actions for")&&body.contains("contentDescription"))
    }
    @Test fun callbacksStillReachTheActivityThroughTheSharedPortraitLandscapeList() {
        val app=source("MixtapeApp");val screen=source("DemoPlayerScreen")
        val list=source("DemoPackaging").substringAfter("internal fun DemoTrackList(")
        assertTrue(app.contains("onDelete = onDeleteTrackFromDevice")&&app.contains("onRemove = onRemoveTrackFromMixtape")&&app.contains("onInfo = onShowTrackInfo"))
        assertTrue(screen.contains("onSelectTrack,onDelete,onRemove,onInfo"))
        assertTrue(list.contains("onDelete(track)")&&list.contains("onRemove(track)")&&list.contains("onInfo(track)"))
    }
    @Test fun trackInfoRouteStillProvidesAllMetadataAndBackNavigation() {
        val app=source("MixtapeApp")
        assertTrue(app.contains("MixtapeScreen.TrackInfo -> TrackInfoScreen("))
        val info=app.substringAfter("private fun TrackInfoScreen(").substringBefore("private fun TrackInfoRow(")
        listOf("Title","Artist","Duration","MediaStore ID","URI","Display name","Album","MIME type","Size","Date added","Date modified","Track number","Mixtape").forEach { assertTrue(info.contains(it)) }
        assertTrue(info.contains("onBack")&&info.contains("Back to Now Playing"))
    }
}
