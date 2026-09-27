package com.example.androidmixtape.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.common.C
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
import kotlin.math.PI
import kotlin.math.sin
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the actual shared service used by both phone and Android Auto controllers. */
@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class MediaAudioFocusTest {
    @get:Rule val composeRule = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val competingFocus = AudioManager.OnAudioFocusChangeListener { }
    private var controller: MediaController? = null
    private val toneFile = File(context.cacheDir, "mixtape-focus-test.wav")

    @Before fun connectToRealPlaybackService() {
        composeRule.setContent { Text("Mixtape audio-focus verification") }
        writeQuietTone()
        lateinit var future: ListenableFuture<MediaController>
        instrumentation.runOnMainSync {
            future = MediaController.Builder(context,
                SessionToken(context, ComponentName(context, MixtapeMediaLibraryService::class.java))).buildAsync()
        }
        controller = future.get(10, TimeUnit.SECONDS)
        instrumentation.runOnMainSync {
            controller!!.setMediaItem(MediaItem.fromUri(Uri.fromFile(toneFile)))
            controller!!.repeatMode = Player.REPEAT_MODE_ONE
            controller!!.prepare()
        }
        awaitPlayer("Prepared local test audio") { it.playbackState == Player.STATE_READY }
    }

    @After fun stopPlaybackAndReleaseFocus() {
        instrumentation.runOnMainSync {
            audioManager.abandonAudioFocus(competingFocus)
            controller?.let { it.stop(); it.clearMediaItems(); it.release() }
            controller = null
        }
        context.stopService(Intent(context, MixtapeMediaLibraryService::class.java))
        toneFile.delete()
    }

    @Test fun coldPlaybackOwnsMediaFocusWithoutAnotherAudioSource() {
        playAndVerifyFocus("cold-start")
        instrumentation.runOnMainSync {
            assertEquals(C.USAGE_MEDIA, controller!!.audioAttributes.usage)
            assertEquals(C.AUDIO_CONTENT_TYPE_MUSIC, controller!!.audioAttributes.contentType)
        }
    }

    @Test fun transientInterruptionResumesButPermanentFocusLossStaysPaused() {
        playAndVerifyFocus("before-interruption")
        assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
            audioManager.requestAudioFocus(competingFocus, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT))
        awaitPlayer("Temporary focus loss pauses output") { !it.isPlaying }
        audioManager.abandonAudioFocus(competingFocus)
        awaitPlayer("Output resumes after temporary focus returns") { it.isPlaying }

        assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
            audioManager.requestAudioFocus(competingFocus, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN))
        awaitPlayer("Another music source permanently takes over") { !it.isPlaying && !it.playWhenReady }
        audioManager.abandonAudioFocus(competingFocus)
        SystemClock.sleep(300)
        instrumentation.runOnMainSync { assertFalse(controller!!.playWhenReady) }
    }

    @Test fun controllerCanStartPlaybackWhilePhoneActivityIsInBackground() {
        // Models a car/controller play command without relying on a visible phone UI.
        shell("input keyevent KEYCODE_HOME")
        SystemClock.sleep(700)
        playAndVerifyFocus("background-controller")
    }

    private fun playAndVerifyFocus(label: String) {
        instrumentation.runOnMainSync { controller!!.play() }
        awaitPlayer("Shared service starts playback") { it.isPlaying }
        // A play request can report playing briefly before asynchronous focus denial arrives.
        SystemClock.sleep(500)
        instrumentation.runOnMainSync { assertTrue("Playback remains active after focus resolution", controller!!.isPlaying) }
        val dump = shell("dumpsys audio")
        File(context.filesDir, "audio-focus-$label.txt").writeText(dump)
        // Restrict the check to current ownership, not historical AudioService event logs.
        val focusStack = dump.substringAfter("Audio Focus stack entries", "")
            // Automotive publishes active owners through its external focus policy.
            .substringBefore("Events log:")
            .substringBefore("Audio Focus event log")
        assertTrue("No ExoPlayer audio-focus owner in current stack:\n$focusStack",
            focusStack.contains("AudioFocusManager") && focusStack.contains(context.packageName))
    }

    private fun awaitPlayer(message: String, condition: (Player) -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (SystemClock.elapsedRealtime() < deadline) {
            var ready = false
            instrumentation.runOnMainSync {
                controller!!.playerError?.let { throw AssertionError("$message: $it") }
                ready = condition(controller!!)
            }
            if (ready) return
            SystemClock.sleep(50)
        }
        fail(message)
    }

    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command),
    ).bufferedReader().use { it.readText() }

    private fun writeQuietTone() {
        val samples = 48_000 * 4
        val pcmBytes = samples * 2
        val buffer = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + pcmBytes).put("WAVEfmt ".toByteArray())
        buffer.putInt(16).putShort(1).putShort(1).putInt(48_000).putInt(96_000).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(pcmBytes)
        repeat(samples) { index -> buffer.putShort((sin(2 * PI * 440 * index / 48_000) * 500).toInt().toShort()) }
        toneFile.writeBytes(buffer.array())
    }
}
