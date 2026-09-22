package com.example.androidmixtape

import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.viewmodel.LibraryStatus
import com.example.androidmixtape.viewmodel.MixtapeViewModel
import org.junit.Assert.*
import org.junit.Test

/** Uses only the owned emulator's synthetic library. No physical-device grants. */
class ModernizationLifecycleTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val permission = if (Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_AUDIO" else "android.permission.READ_EXTERNAL_STORAGE"
    private fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)
    ).use { it.readBytes() }

    @Test fun settingsGrantRefreshesSameActivityAndRecreationRebindsCallback() {
        // Clearing permission kills the process, so fresh denial is tested externally.
        shell("pm grant ${context.packageName} $permission")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var vm: MixtapeViewModel? = null
            var oldActivity: MainActivity? = null
            var oldCallback: Any? = null
            scenario.onActivity {
                oldActivity = it
                vm = ViewModelProvider(it)[MixtapeViewModel::class.java]
                // Model the denied state before returning from system Settings.
                vm!!.onPermissionResult(false)
                assertEquals(LibraryStatus.PermissionRequired, vm!!.uiState.value.status)
                oldCallback = vm!!.onDeleteTrackUserActionRequired
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            val deadline = SystemClock.elapsedRealtime() + 15000
            do {
                var loading = true
                scenario.onActivity { loading = vm!!.uiState.value.status == LibraryStatus.Loading }
                if (!loading) break
                SystemClock.sleep(100)
            } while (SystemClock.elapsedRealtime() < deadline)
            scenario.onActivity {
                assertSame(oldActivity, it)
                assertEquals(LibraryStatus.Ready, vm!!.uiState.value.status)
                assertTrue(vm!!.uiState.value.tracks.isNotEmpty())
            }
            scenario.recreate()
            scenario.onActivity {
                assertNotSame(oldActivity, it)
                assertSame(vm, ViewModelProvider(it)[MixtapeViewModel::class.java])
                assertNotNull(vm!!.onDeleteTrackUserActionRequired)
                assertNotSame(oldCallback, vm!!.onDeleteTrackUserActionRequired)
            }
            repeat(10) {
                scenario.moveToState(Lifecycle.State.CREATED)
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.onActivity { assertEquals(LibraryStatus.Ready, vm!!.uiState.value.status) }
            }
            scenario.close()
            instrumentation.runOnMainSync { assertNull(vm!!.onDeleteTrackUserActionRequired) }
        }
    }
}
