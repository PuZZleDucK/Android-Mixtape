package com.example.androidmixtape.playback

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.ui.AndroidMixtapeTheme
import com.example.androidmixtape.ui.MixtapeApp
import com.example.androidmixtape.viewmodel.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class AudioMeterTest {
    @get:Rule val composeRule = createComposeRule()
    @After fun resetMonitor() { AudioLevelMonitor.reset() }

    @Test fun stereoPcmPassesThroughUnchangedAndDeckShowsSixtyAndFortyPercent() {
        val processor = configuredProcessor(2)
        val input = sinePcm(-12.0, -18.0)
        val original = ByteArray(input.remaining()).also { input.duplicate().get(it) }
        processor.queueInput(input)
        assertFalse(input.hasRemaining())
        val output = processor.output
        val actual = ByteArray(output.remaining()).also { output.get(it) }
        assertArrayEquals("Metering must never change playback samples or volume", original, actual)
        assertEquals(0.6f, AudioLevelMonitor.levels.value.left, 0.001f)
        assertEquals(0.4f, AudioLevelMonitor.levels.value.right, 0.001f)

        // A live meter needs live PCM; a single buffer expires after 500 ms.
        val feeder = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
        feeder.scheduleAtFixedRate({ processor.queueInput(sinePcm(-12.0, -18.0)); processor.output }, 0, 50, java.util.concurrent.TimeUnit.MILLISECONDS)
        try {
        val track = Track(1, "Stereo level check", "Test signal", 180_000, "content://test/meter")
        val group = MixTapeGroup("Level check", listOf(track), 0)
        composeRule.setContent {
            Box(Modifier.requiredSize(900.dp, 440.dp)) {
                AndroidMixtapeTheme {
                    MixtapeApp(
                        state = MixtapeUiState(
                            status = LibraryStatus.Ready, screen = MixtapeScreen.NowPlaying,
                            tracks = listOf(track), mixTapeGroups = listOf(group), queueTracks = listOf(track),
                            currentTrack = track, currentIndex = 0, currentMixtapeIndex = 0,
                            currentMixtapeName = group.name, isPlaying = true,
                            mixtapeThemeSettings = MixtapeThemeSettings(deckTheme = DeckTheme.BlackoutPortable),
                        ),
                        onRequestPermission = {}, onRefresh = {}, onTogglePlayPause = {}, onPrevious = {}, onNext = {}, onSeekTo = {},
                    )
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(300)
        listOf("L" to 60, "R" to 40).forEach { (channel, expected) ->
            val description = composeRule.onNodeWithContentDescription("$channel audio level", substring = true)
                .fetchSemanticsNode().config[SemanticsProperties.ContentDescription].first()
            val percent = Regex("(\\d+) percent").find(description)!!.groupValues[1].toInt()
            assertTrue(description, percent in (expected - 1)..(expected + 1))
        }
        composeRule.waitForIdle()
        android.os.SystemClock.sleep(700)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val path = File(instrumentation.targetContext.getExternalFilesDir(null), "audio-meter-scale.png")
        path.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        } finally {
            feeder.shutdownNow()
            feeder.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
            processor.reset()
        }
    }

    @Test fun monoIsMirroredAndFlushAndSilenceClearBothChannels() {
        val processor = configuredProcessor(1)
        processor.queueInput(sinePcm(-12.0))
        assertEquals(0.6f, AudioLevelMonitor.levels.value.left, 0.001f)
        assertEquals(AudioLevelMonitor.levels.value.left, AudioLevelMonitor.levels.value.right, 0f)
        processor.flush()
        assertEquals(StereoAudioLevels(), AudioLevelMonitor.levels.value)
        processor.queueInput(ByteBuffer.allocateDirect(4800).order(ByteOrder.nativeOrder()))
        assertEquals(StereoAudioLevels(), AudioLevelMonitor.levels.value)
        processor.reset()
        assertEquals(StereoAudioLevels(), AudioLevelMonitor.levels.value)
    }

    private fun configuredProcessor(channels: Int) = MeteringAudioProcessor().apply {
        configure(AudioFormat(48_000, channels, C.ENCODING_PCM_16BIT))
        flush()
    }

    private fun sinePcm(vararg channelDb: Double): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(2400 * channelDb.size * 2).order(ByteOrder.nativeOrder())
        repeat(2400) { frame ->
            channelDb.forEach { db ->
                val sample = sin(2.0 * PI * frame / 48.0) * sqrt(2.0) * 10.0.pow(db / 20.0)
                buffer.putShort((sample * 32768.0).toInt().toShort())
            }
        }
        buffer.flip()
        return buffer
    }
}
