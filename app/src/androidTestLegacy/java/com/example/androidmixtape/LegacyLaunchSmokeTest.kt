package com.example.androidmixtape

import android.app.Activity
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyLaunchSmokeTest {
    @Test
    fun legacyVariantExposesLauncherActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        assertTrue("Expected legacy application id", context.packageName.endsWith(".legacy"))
        assertNotNull("Legacy variant should expose a launcher activity", intent)

        @Suppress("DEPRECATION")
        val activity = instrumentation.startActivitySync(
            intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ) as Activity

        try {
            instrumentation.waitForIdleSync()
            assertTrue("Legacy launcher should stay alive long enough for a smoke check", !activity.isFinishing)
        } finally {
            activity.finish()
        }
    }
}
