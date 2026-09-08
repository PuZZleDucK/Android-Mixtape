package com.example.androidmixtape.ui

import com.example.androidmixtape.viewmodel.DeckTheme

internal fun DeckTheme.usesSegmentedMeter(): Boolean =
    this == DeckTheme.BlackoutPortable || this == DeckTheme.SunsetBoombox

/** Input is already scaled by AudioMeterScale. Hysteresis applies only to falling cells. */
internal fun litMeterSegments(level: Float?, isPlaying: Boolean, previousCount: Int): Int {
    if (!isPlaying || level == null || !level.isFinite() || level <= 0f) return 0
    val normalized = level.coerceIn(0f, 1f)
    val previous = previousCount.coerceIn(0, 10)
    return (1..10).count { cell ->
        val threshold = cell / 10f
        normalized >= if (cell <= previous) threshold - 0.02f else threshold
    }
}
