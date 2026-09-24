package com.example.androidmixtape.playback

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source-level red specs for the system media card's non-transport tap. */
class MediaNotificationNavigationContractTest {
    private fun source(path: String): String =
        listOf(File("app/$path"), File(path)).first { it.isFile }.readText()

    @Test fun mediaSessionProvidesActivityPendingIntent() {
        val service = source("src/modern/java/com/example/androidmixtape/playback/MixtapeMediaLibraryService.kt")
        assertTrue("The Media3 session needs a content tap target", service.contains("setSessionActivity("))
        assertTrue("Use an explicit activity PendingIntent, not a playback command", service.contains("PendingIntent.getActivity("))
    }

    @Test fun activityHandlesColdAndWarmNotificationLaunches() {
        val activity = source("src/modern/java/com/example/androidmixtape/MainActivity.kt")
        assertTrue("A warm activity must receive a fresh notification tap", activity.contains("override fun onNewIntent("))
        assertTrue(
            "Both initial and delivered intents must be inspected",
            Regex("handleMediaNotificationIntent\\(intent\\)").findAll(activity).count() >= 2,
        )
        assertTrue("The intent handler must navigate, not start playback", activity.contains("viewModel.openNowPlayingFromNotification()"))
    }
}
