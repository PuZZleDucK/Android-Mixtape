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
        assertTrue("Cold entry handles its intent without treating rotation as a new launcher entry",
            activity.substringAfter("override fun onCreate(").substringBefore("override fun onNewIntent(")
                .contains("handleMediaNotificationIntent(intent, launcherEntry = savedInstanceState == null)"))
        assertTrue("Delivered notification/launcher intents are still inspected",
            activity.substringAfter("override fun onNewIntent(").substringBefore("private fun handleMediaNotificationIntent(")
                .contains("handleMediaNotificationIntent(intent)"))
        assertTrue("Ordinary launcher entries have their own non-transport route",
            activity.contains("launcherEntry && intent?.action == Intent.ACTION_MAIN") &&
                activity.contains("viewModel.openFromLauncher()"))
        assertTrue("The intent handler must navigate, not start playback", activity.contains("viewModel.openNowPlayingFromNotification()"))
    }
    @Test fun ordinaryTaskReturnDoesNotRequireANewIntentAndDoesNotResetRotation() {
        val activity=source("src/modern/java/com/example/androidmixtape/MainActivity.kt")
        assertTrue(activity.contains("override fun onUserLeaveHint()"))
        assertTrue(activity.contains("returningFromUserLeave = true"))
        assertTrue(activity.contains("if (returningFromUserLeave && !handledEntryIntent) viewModel.openFromLauncher()"))
        assertTrue(activity.contains("returningFromUserLeave = false"))
        assertTrue(activity.substringAfter("private fun handleMediaNotificationIntent(").substringBefore("override fun onUserLeaveHint()")
            .contains("handledEntryIntent = true"))
        assertTrue(activity.contains("launcherEntry = savedInstanceState == null"))
    }

}
