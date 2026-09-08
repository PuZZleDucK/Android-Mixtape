package com.example.androidmixtape.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.AudioLevelMonitor
import com.example.androidmixtape.playback.MeteringAudioProcessor
import com.example.androidmixtape.viewmodel.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises production PCM scaling and Compose collection, not playback or AudioTrack timing. */
@UnstableApi
@RunWith(AndroidJUnit4::class)
class SegmentedMeterUiTest {
    @get:Rule val compose = createComposeRule()
    private val processor = MeteringAudioProcessor()
    @After fun cleanup() { processor.reset(); AudioLevelMonitor.reset() }

    @Test fun bothSkinsFollowStereoPcmAndClearOnPauseAndFlush() {
        val track = Track(1, "Segment response", "PCM fixture", 180_000, "content://test/segments")
        val group = MixTapeGroup("Meter check", listOf(track), 0)
        val state = mutableStateOf(MixtapeUiState(
            status = LibraryStatus.Ready, screen = MixtapeScreen.NowPlaying,
            tracks = listOf(track), mixTapeGroups = listOf(group), queueTracks = listOf(track),
            currentTrack = track, currentIndex = 0, currentMixtapeIndex = 0,
            currentMixtapeName = group.name, isPlaying = true,
        ))
        val dark = mutableStateOf(false)
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                MixtapeApp(state = state.value,
                    onRequestPermission = {}, onRefresh = {}, onTogglePlayPause = {},
                    onPrevious = {}, onNext = {}, onSeekTo = {})
            }
        }
        processor.configure(AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        for (skin in listOf(DeckTheme.BlackoutPortable, DeckTheme.SunsetBoombox)) {
            for (night in listOf(false, true)) {
                compose.runOnIdle {
                    dark.value = night
                    state.value = state.value.copy(isPlaying = true,
                        mixtapeThemeSettings = MixtapeThemeSettings(deckTheme = skin))
                    processor.flush()
                }
                val prefix = "${skin.name}-${if (night) "dark" else "light"}"
                feed(0.25, 0.55); assertCounts(2, 5); capture("$prefix-low-mid")
                feed(0.95, 0.25, 24); assertCounts(9, 2); capture("$prefix-high-low")
                feed(0.25, 0.95, 24); assertCounts(2, 9); capture("$prefix-low-high")
                compose.runOnIdle { state.value = state.value.copy(isPlaying = false) }
                assertCounts(0, 0); capture("$prefix-paused")
                compose.runOnIdle { processor.flush(); state.value = state.value.copy(isPlaying = true) }
                assertCounts(0, 0)
                feed(0.55, 0.25); assertCounts(5, 2)
                feed(0.0, 0.0, 40); assertCounts(0, 0); capture("$prefix-silence")
                feed(0.95, 0.95, 20); assertCounts(9, 9)
                // Identical sustained PCM must keep the monitor alive, even when StateFlow deduplicates it.
                SystemClock.sleep(700)
                assertCounts(0, 0); capture("$prefix-missing-pcm")
                feed(0.95, 0.95); assertCounts(9, 9)
                compose.runOnIdle { processor.flush() }
                assertCounts(0, 0)
            }
        }
    }

    @Test fun actualPlaybackRisesFallsPausesAndStops() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wav = File(context.cacheDir, "segmented-meter-playback.wav")
        val sampleRate = 48_000
        val phases = listOf(0.25 to 0.55, 0.95 to 0.25, 0.25 to 0.95, 0.0 to 0.0)
        val pcmBytes = sampleRate * 3 * phases.size * 4
        val bytes = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36 + pcmBytes).put("WAVEfmt ".toByteArray())
        bytes.putInt(16).putShort(1).putShort(2).putInt(sampleRate).putInt(sampleRate * 4)
        bytes.putShort(4).putShort(16).put("data".toByteArray()).putInt(pcmBytes)
        for ((left, right) in phases) {
            repeat(sampleRate * 3) { frame ->
                for (level in listOf(left, right)) {
                    val rms = if (level == 0.0) 0.0 else 10.0.pow((level * 30.0 - 30.0) / 20.0)
                    val sample = sin(2.0 * PI * frame / 48.0) * sqrt(2.0) * rms
                    bytes.putShort((sample * 32768.0).toInt().coerceIn(-32768, 32767).toShort())
                }
            }
        }
        wav.writeBytes(bytes.array())
        val track = Track(1, "Stereo PCM playback", "Level fixture", 12_000, wav.toURI().toString())
        val group = MixTapeGroup("Playback", listOf(track), 0)
        val state = mutableStateOf(MixtapeUiState(
            status = LibraryStatus.Ready, screen = MixtapeScreen.NowPlaying,
            tracks = listOf(track), mixTapeGroups = listOf(group), queueTracks = listOf(track),
            currentTrack = track, currentIndex = 0, currentMixtapeIndex = 0,
            currentMixtapeName = group.name,
        ))
        compose.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(state = state.value,
                    onRequestPermission = {}, onRefresh = {}, onTogglePlayPause = {},
                    onPrevious = {}, onNext = {}, onSeekTo = {})
            }
        }
        lateinit var player: androidx.media3.exoplayer.ExoPlayer
        compose.runOnIdle {
            player = androidx.media3.exoplayer.ExoPlayer.Builder(context,
                com.example.androidmixtape.playback.MeteringRenderersFactory(context, processor)).build()
            player.addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    state.value = state.value.copy(isPlaying = isPlaying)
                }
            })
        }
        try {
            for (skin in listOf(DeckTheme.BlackoutPortable, DeckTheme.SunsetBoombox)) {
                compose.runOnIdle {
                    state.value = state.value.copy(mixtapeThemeSettings = MixtapeThemeSettings(deckTheme = skin))
                    player.setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(wav)))
                    player.prepare(); player.play()
                }
                for ((index, counts) in listOf(2 to 5, 9 to 2, 2 to 9, 0 to 0).withIndex()) {
                    compose.waitUntil(10_000) {
                        val levels = AudioLevelMonitor.levels.value
                        state.value.isPlaying &&
                            levels.left in (counts.first / 10f)..(counts.first / 10f + 0.07f) &&
                            levels.right in (counts.second / 10f)..(counts.second / 10f + 0.07f)
                    }
                    assertCounts(counts.first, counts.second)
                    capture("${skin.name}-playback-$index")
                    if (index == 0) {
                        compose.runOnIdle { player.pause() }
                        assertCounts(0, 0); capture("${skin.name}-playback-pause")
                        SystemClock.sleep(700)
                        compose.runOnIdle { player.play() }
                    }
                }
                compose.runOnIdle { player.seekTo(3_000) }
                compose.waitUntil(5_000) { AudioLevelMonitor.levels.value.left > 0.9f }
                assertCounts(9, 2)
                // Replace a loud track with its silent tail, then stop during loud playback.
                compose.runOnIdle {
                    player.setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(wav)), 9_000)
                    player.prepare()
                }
                compose.waitUntil(5_000) { AudioLevelMonitor.levels.value.left == 0f && AudioLevelMonitor.levels.value.right == 0f }
                assertCounts(0, 0); capture("${skin.name}-playback-replacement")
                compose.runOnIdle { player.seekTo(3_000) }
                compose.waitUntil(5_000) { AudioLevelMonitor.levels.value.left > 0.9f }
                assertCounts(9, 2)
                compose.runOnIdle { player.stop() }
                assertCounts(0, 0); capture("${skin.name}-playback-stop")
            }
        } finally {
            compose.runOnIdle { player.release() }
            wav.delete()
        }
    }

    private fun assertCounts(left: Int, right: Int) {
        compose.waitForIdle()
        compose.onNodeWithContentDescription("L segmented meter $left of 10", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("R segmented meter $right of 10", useUnmergedTree = true).assertExists()
    }

    private fun feed(left: Double, right: Double, blocks: Int = 1) {
        repeat(blocks) {
            SystemClock.sleep(35)
            val buffer = ByteBuffer.allocateDirect(2400 * 4).order(ByteOrder.nativeOrder())
            repeat(2400) { frame ->
                for (level in listOf(left, right)) {
                    val rms = if (level == 0.0) 0.0 else 10.0.pow((level * 30.0 - 30.0) / 20.0)
                    val sample = sin(2.0 * PI * frame / 48.0) * sqrt(2.0) * rms
                    buffer.putShort((sample * 32768.0).toInt().coerceIn(-32768, 32767).toShort())
                }
            }
            buffer.flip()
            compose.runOnIdle { processor.queueInput(buffer); processor.output }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        // Semantics can settle before SurfaceFlinger presents the new frame.
        SystemClock.sleep(150)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val orientation = instrumentation.targetContext.resources.configuration.orientation
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "segmented-meter-test").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(directory, "$orientation-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
