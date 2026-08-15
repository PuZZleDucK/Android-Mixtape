package com.example.androidmixtape.playback

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class StereoAudioLevels(
    val left: Float = 0f,
    val right: Float = 0f,
)

internal object AudioLevelMonitor {
    private val mutableLevels = MutableStateFlow(StereoAudioLevels())
    val levels: StateFlow<StereoAudioLevels> = mutableLevels.asStateFlow()

    fun update(left: Float, right: Float) {
        mutableLevels.value = StereoAudioLevels(left.coerceIn(0f, 1f), right.coerceIn(0f, 1f))
    }

    fun reset() {
        mutableLevels.value = StereoAudioLevels()
    }
}

/** Pass-through PCM processor that measures each stereo channel before playback. */
@UnstableApi
internal class MeteringAudioProcessor : BaseAudioProcessor() {
    private var channelCount = 0
    private var smoothedLeft = 0f
    private var smoothedRight = 0f
    private var lastPublishMs = 0L

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount < 1) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        channelCount = inputAudioFormat.channelCount
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputSize = inputBuffer.remaining()
        if (inputSize == 0) return

        measure(inputBuffer.duplicate().order(ByteOrder.nativeOrder()))
        val outputBuffer = replaceOutputBuffer(inputSize)
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }

    override fun onFlush() {
        smoothedLeft = 0f
        smoothedRight = 0f
        lastPublishMs = 0L
        AudioLevelMonitor.reset()
    }

    override fun onReset() {
        onFlush()
        channelCount = 0
    }

    private fun measure(buffer: ByteBuffer) {
        val bytesPerFrame = channelCount * 2
        val frameCount = buffer.remaining() / bytesPerFrame
        if (frameCount <= 0) return

        var leftSquares = 0.0
        var rightSquares = 0.0
        repeat(frameCount) {
            val left = buffer.short.toInt() / 32768.0
            val right = if (channelCount > 1) buffer.short.toInt() / 32768.0 else left
            leftSquares += left * left
            rightSquares += right * right
            repeat((channelCount - 2).coerceAtLeast(0)) { buffer.short }
        }

        val leftLevel = rmsToMeter(sqrt(leftSquares / frameCount))
        val rightLevel = rmsToMeter(sqrt(rightSquares / frameCount))
        smoothedLeft = smooth(smoothedLeft, leftLevel)
        smoothedRight = smooth(smoothedRight, rightLevel)

        val now = SystemClock.elapsedRealtime()
        if (now - lastPublishMs >= PUBLISH_INTERVAL_MS) {
            lastPublishMs = now
            AudioLevelMonitor.update(smoothedLeft, smoothedRight)
        }
    }

    private fun rmsToMeter(rms: Double): Float {
        if (rms <= 0.000_001) return 0f
        val decibels = 20.0 * log10(rms)
        return ((decibels + METER_FLOOR_DB) / METER_FLOOR_DB).toFloat().coerceIn(0f, 1f)
    }

    private fun smooth(previous: Float, measured: Float): Float =
        if (measured >= previous) measured else max(measured, previous * RELEASE_FACTOR)

    private companion object {
        const val METER_FLOOR_DB = 60.0
        const val RELEASE_FACTOR = 0.82f
        const val PUBLISH_INTERVAL_MS = 33L
    }
}

@UnstableApi
internal class MeteringRenderersFactory(
    context: Context,
    private val meteringAudioProcessor: MeteringAudioProcessor,
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setAudioProcessors(arrayOf(meteringAudioProcessor))
        .setEnableFloatOutput(false)
        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
        .build()
}
