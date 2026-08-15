package com.example.androidmixtape.playback

import com.example.androidmixtape.data.Track
import kotlin.math.roundToInt

/**
 * Position of playback across the complete mixtape, rather than within only
 * the currently playing track.
 */
fun mixtapePlaybackProgress(
    tracks: List<Track>,
    currentIndex: Int,
    currentTrackPositionMs: Long,
): Float {
    if (tracks.isEmpty() || currentIndex !in tracks.indices) return 0f

    val durations = tracks.map { it.durationMs.coerceAtLeast(0L) }
    val totalDurationMs = durations.fold(0L) { total, duration ->
        if (Long.MAX_VALUE - total < duration) Long.MAX_VALUE else total + duration
    }
    if (totalDurationMs <= 0L) return 0f

    val elapsedBeforeCurrentTrackMs = durations
        .take(currentIndex)
        .fold(0L) { total, duration ->
            if (Long.MAX_VALUE - total < duration) Long.MAX_VALUE else total + duration
        }
    val elapsedInCurrentTrackMs = currentTrackPositionMs.coerceIn(0L, durations[currentIndex])
    val elapsedMs = if (Long.MAX_VALUE - elapsedBeforeCurrentTrackMs < elapsedInCurrentTrackMs) {
        Long.MAX_VALUE
    } else {
        elapsedBeforeCurrentTrackMs + elapsedInCurrentTrackMs
    }

    return (elapsedMs.toDouble() / totalDurationMs.toDouble()).toFloat().coerceIn(0f, 1f)
}

fun mixtapeCounterValue(progress: Float): Int =
    (progress.coerceIn(0f, 1f) * 999f).roundToInt().coerceIn(0, 999)
