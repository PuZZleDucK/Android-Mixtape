package com.example.androidmixtape.playback

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private const val DEFAULT_CUE_SAMPLE_RATE = 22_050
private const val CUE_CHUNK_SAMPLES = 1_024

enum class TransportCueDirection {
    FAST_FORWARD,
    REWIND,
    NONE,
}

interface TransportCuePlayer {
    suspend fun play(direction: TransportCueDirection, durationMs: Long)
    fun cancel()
    fun release()
}

object SilentTransportCuePlayer : TransportCuePlayer {
    override suspend fun play(direction: TransportCueDirection, durationMs: Long) {
        if (direction != TransportCueDirection.NONE && durationMs > 0L) {
            delay(durationMs)
        }
    }

    override fun cancel() = Unit

    override fun release() = Unit
}

class AudioTrackTransportCuePlayer : TransportCuePlayer {
    @Volatile
    private var activeTrack: AudioTrack? = null

    override suspend fun play(direction: TransportCueDirection, durationMs: Long) {
        cancel()
        if (direction == TransportCueDirection.NONE || durationMs <= 0L) return

        withContext(Dispatchers.Default) {
            val minBufferSize = AudioTrack.getMinBufferSize(
                DEFAULT_CUE_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(CUE_CHUNK_SAMPLES * 2)
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                DEFAULT_CUE_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM,
            )
            activeTrack = track
            val chunk = ShortArray(CUE_CHUNK_SAMPLES)
            val totalSamples = max(1, (DEFAULT_CUE_SAMPLE_RATE * durationMs / 1_000L).toInt())
            var sampleCursor = 0
            var phase = 0.0

            try {
                track.play()
                while (currentCoroutineContext().isActive && activeTrack === track && sampleCursor < totalSamples) {
                    val samplesThisChunk = minOf(chunk.size, totalSamples - sampleCursor)
                    for (i in 0 until samplesThisChunk) {
                        val progress = (sampleCursor + i).toDouble() / totalSamples.toDouble()
                        val sweepProgress = if (direction == TransportCueDirection.FAST_FORWARD) progress else 1.0 - progress
                        val frequency = 420.0 + (sweepProgress * 1_480.0)
                        phase += (2.0 * PI * frequency) / DEFAULT_CUE_SAMPLE_RATE
                        val flutter = if (((sampleCursor + i) / 140) % 2 == 0) 0.72 else 0.42
                        chunk[i] = (sin(phase) * Short.MAX_VALUE * 0.28 * flutter).toInt().toShort()
                    }
                    for (i in samplesThisChunk until chunk.size) {
                        chunk[i] = 0
                    }
                    track.write(chunk, 0, samplesThisChunk)
                    sampleCursor += samplesThisChunk
                }
            } finally {
                if (activeTrack === track) {
                    activeTrack = null
                }
                runCatching { track.pause() }
                runCatching { track.flush() }
                runCatching { track.release() }
            }
        }
    }

    override fun cancel() {
        val track = activeTrack
        activeTrack = null
        if (track != null) {
            runCatching { track.pause() }
            runCatching { track.flush() }
            runCatching { track.release() }
        }
    }

    override fun release() {
        cancel()
    }
}
