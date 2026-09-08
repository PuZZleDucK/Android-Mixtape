package com.example.androidmixtape.playback

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import com.example.androidmixtape.MainActivity
import com.example.androidmixtape.viewmodel.MixtapeViewModel
import org.junit.Rule
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real service/controller boundary, not a synthetic PlayerEngine callback. */
@RunWith(AndroidJUnit4::class)
class CounterTransportServiceTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun externalTransportPublishesDiscontinuitiesAndStablePause() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val audio = File(context.cacheDir, "counter-transport.wav")
        val samples = 8000 * 30
        val bytes = samples * 2
        val wav = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        wav.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVEfmt ".toByteArray())
        wav.putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
        wav.put("data".toByteArray()).putInt(bytes)
        audio.writeBytes(wav.array())
        var engine: MediaControllerPlayerEngine? = null
        var controller: MediaController? = null
        var activity: ActivityScenario<MainActivity>? = null
        val snapshots = mutableListOf<ExternalPlaybackSnapshot>()
        fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
        fun await(label: String, condition: () -> Boolean) {
            val deadline = SystemClock.elapsedRealtime() + 10000
            while (SystemClock.elapsedRealtime() < deadline) {
                var done = false
                main { controller?.playerError?.let { throw AssertionError(it) }; done = condition() }
                if (done) { println("PASS $label"); return }
                SystemClock.sleep(25)
            }
            fail(label)
        }
        try {
            lateinit var future: ListenableFuture<MediaController>
            main {
                engine = MediaControllerPlayerEngine(context)
                engine!!.setOnExternalPlaybackSnapshotChanged { snapshots.add(it) }
                future = MediaController.Builder(context, SessionToken(context,
                    ComponentName(context, MixtapeMediaLibraryService::class.java))).buildAsync()
            }
            controller = future.get(10, TimeUnit.SECONDS)
            val permission = if (android.os.Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_AUDIO"
                else "android.permission.READ_EXTERNAL_STORAGE"
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} $permission")
            ).use { it.readBytes() }
            activity = ActivityScenario.launch(MainActivity::class.java)
            var viewModel: MixtapeViewModel? = null
            activity.onActivity { viewModel = ViewModelProvider(it)[MixtapeViewModel::class.java] }
            await("Activity initial scan finished") {
                viewModel!!.uiState.value.status != com.example.androidmixtape.viewmodel.LibraryStatus.Loading
            }
            main {
                controller!!.setMediaItems(listOf("first", "second").map {
                    MediaItem.Builder().setMediaId(it).setUri(Uri.fromFile(audio))
                        .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setExtras(
                            android.os.Bundle().apply {
                                putLong("com.example.androidmixtape.extra.TRACK_DURATION_MS", 30000L)
                            }
                        ).build()).build()
                })
                controller!!.prepare()
            }
            await("ready with two tracks") { controller!!.playbackState == Player.STATE_READY && snapshots.lastOrNull()?.tracks?.size == 2 }
            main { controller!!.play() }
            await("live playback snapshot") { snapshots.lastOrNull()?.isPlaying == true && controller!!.currentPosition > 100 }
            main { controller!!.pause() }
            await("pause snapshot") { snapshots.lastOrNull()?.isPlaying == false }
            var pausedPosition = 0L
            var pausedSamples = 0
            main { pausedPosition = controller!!.currentPosition; pausedSamples = snapshots.size }
            SystemClock.sleep(500)
            main {
                assertEquals("Paused position must not advance", pausedPosition, controller!!.currentPosition)
                assertEquals("Pause must not manufacture samples", pausedSamples, snapshots.size)
            }
            for (position in listOf(10000L, 2000L, 25000L, 0L)) {
                var start = 0
                main { start = snapshots.size; controller!!.seekTo(position) }
                await("external seek $position marks discontinuity") {
                    snapshots.drop(start).any { it.positionDiscontinuity && it.positionMs == position }
                }
                SystemClock.sleep(200)
                main { assertEquals(position, snapshots.last().positionMs) }
            }
            var start = 0
            main { start = snapshots.size; controller!!.seekTo(1, 0) }
            await("track change settles at second track") {
                snapshots.drop(start).any { it.currentIndex == 1 && it.positionMs == 0L && it.positionDiscontinuity }
            }
            main { controller!!.play() }
            await("resume after seeks") { snapshots.lastOrNull()?.isPlaying == true }
            await("Activity ViewModel receives live service queue") { viewModel!!.uiState.value.queueTracks.size == 2 }
            // Navigate through a production ViewModel action, without injecting UI state.
            main { viewModel!!.backFromTrackInfo() }
            var beforeRotation = 0L
            main { beforeRotation = controller!!.currentPosition }
            activity.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            activity.recreate()
            activity.onActivity { viewModel = ViewModelProvider(it)[MixtapeViewModel::class.java] }
            await("playback survives Activity rotation and recreation") {
                controller!!.isPlaying && controller!!.currentPosition > beforeRotation &&
                    viewModel!!.uiState.value.isPlaying
            }
            main { controller!!.pause(); controller!!.seekTo(0, 0L) }
            await("recreated Activity receives external reset") {
                viewModel!!.uiState.value.currentIndex == 0 && viewModel!!.uiState.value.positionMs == 0L &&
                    !viewModel!!.uiState.value.isPlaying
            }
            compose.onNodeWithContentDescription("Tape counter 000").assertExists()
            SystemClock.sleep(500)
            compose.onNodeWithContentDescription("Tape counter 000").assertExists()
            val evidence = File(context.getExternalFilesDir(null), "counter-wheels/activity").apply { mkdirs() }
            instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                File(evidence, "recreated-reset.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
            println("PASS recreated production deck settles on 000 with no stale digits")
            compose.onNodeWithContentDescription("Fast-forward").performClick()
            await("local fast-forward reaches second track") { viewModel!!.uiState.value.currentIndex == 1 }
            main { controller!!.pause(); controller!!.seekTo(0L) }
            await("second track paused at start") { !viewModel!!.uiState.value.isPlaying && viewModel!!.uiState.value.positionMs == 0L }
            compose.onNodeWithContentDescription("Tape counter 500").assertExists()
            compose.onNodeWithContentDescription("Rewind").performClick()
            await("local rewind reaches first track") { viewModel!!.uiState.value.currentIndex == 0 }
            compose.onNodeWithContentDescription("Stop").performClick()
            await("local stop resets counter") { !viewModel!!.uiState.value.isPlaying && viewModel!!.uiState.value.positionMs == 0L }
            compose.onNodeWithContentDescription("Tape counter 000").assertExists()
            main { controller!!.stop() }
            await("stop publishes not playing") { snapshots.lastOrNull()?.isPlaying == false && controller!!.playbackState == Player.STATE_IDLE }
            main { controller!!.clearMediaItems() }
            await("queue reset clears tracks") { snapshots.lastOrNull()?.tracks?.isEmpty() == true }
        } finally {
            activity?.close()
            main { engine?.release(); controller?.let { it.stop(); it.clearMediaItems(); it.release() } }
            context.stopService(Intent(context, MixtapeMediaLibraryService::class.java))
            audio.delete()
        }
    }
}
