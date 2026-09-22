package com.example.androidmixtape

import android.graphics.Bitmap
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.viewmodel.MixtapeViewModel
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Requires shell-seeded synthetic media named Card4900Delete on an owned emulator. */
class ModernizationDeleteDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)
    ).use { it.readBytes().toString(Charsets.UTF_8) }

    private fun await(check: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (!check() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        assertTrue("Condition did not become true", check())
    }

    private fun button(text: String): AccessibilityNodeInfo? =
        instrumentation.uiAutomation.rootInActiveWindow
            ?.findAccessibilityNodeInfosByViewId("android:id/$text")?.firstOrNull { it.isClickable }

    private fun capture(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    private fun recreateBehindConsent(scenario: ActivityScenario<MainActivity>) {
        val created = java.util.concurrent.atomic.AtomicReference<MainActivity>()
        val monitor = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
        val callback = androidx.test.runner.lifecycle.ActivityLifecycleCallback { activity, stage ->
            if (activity is MainActivity && stage == androidx.test.runner.lifecycle.Stage.CREATED) created.set(activity)
        }
        instrumentation.runOnMainSync { monitor.addLifecycleCallback(callback) }
        try {
            // ActivityScenario.recreate forces RESUMED, impossible behind a system consent Activity.
            scenario.onActivity { it.recreate() }
            await { created.get() != null }
            scenario.onActivity { assertSame(created.get(), it) }
        } finally {
            instrumentation.runOnMainSync { monitor.removeLifecycleCallback(callback) }
        }
    }

    @Test fun pendingConsentSurvivesRecreationCancelThenApproveDeletesProviderRow() {
        instrumentation.uiAutomation.serviceInfo = instrumentation.uiAutomation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        val permission = if (Build.VERSION.SDK_INT >= 33) "READ_MEDIA_AUDIO" else "READ_EXTERNAL_STORAGE"
        shell("pm grant ${context.packageName} android.permission.$permission")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: MixtapeViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[MixtapeViewModel::class.java] }
            await { vm.uiState.value.tracks.any { it.title.startsWith("Card4900Delete") } }
            val track = vm.uiState.value.tracks.first { it.title.startsWith("Card4900Delete") }
            fun exists() = context.contentResolver.query(android.net.Uri.parse(track.uri), arrayOf("_id"), null, null, null)
                ?.use { it.moveToFirst() } == true
            assertTrue(exists())
            scenario.onActivity { vm.deleteTrackFromDevice(track.id) }
            await { button("button2") != null }
            capture("delete-before-recreation.png")
            recreateBehindConsent(scenario)
            // Dialog remains owned by Android while the app's callback is rebound.
            await { button("button2") != null }
            capture("delete-after-recreation.png")
            assertTrue(button("button2")!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            await { vm.uiState.value.message?.startsWith("Did not delete") == true }
            assertTrue(exists())
            scenario.onActivity {
                assertSame(vm, ViewModelProvider(it)[MixtapeViewModel::class.java])
                assertTrue(vm.uiState.value.tracks.any { t -> t.id == track.id })
                vm.deleteTrackFromDevice(track.id)
            }
            val approve = "button1"
            await { button(approve) != null }
            recreateBehindConsent(scenario)
            await { button(approve) != null }
            assertTrue(button(approve)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            await { vm.uiState.value.message?.startsWith("Deleted ") == true }
            assertFalse("MediaStore row must actually be gone", exists())
            assertFalse(vm.uiState.value.tracks.any { it.id == track.id })
            capture("delete-completed.png")
        }
    }
}
