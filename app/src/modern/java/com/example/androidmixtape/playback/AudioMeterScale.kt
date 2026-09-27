package com.example.androidmixtape.playback

import kotlin.math.log10

// Display range only: no gain or normalization is applied to the playback PCM.
// A -60dB floor put ordinary -12dB RMS audio at 80% height; -30dB puts it at 60%.
private const val METER_MIN_DB = -30.0

internal fun rmsToMeter(rms: Double): Float {
    if (!rms.isFinite() || rms <= 0.0) return 0f
    val decibels = 20.0 * log10(rms)
    return ((decibels - METER_MIN_DB) / -METER_MIN_DB).toFloat().coerceIn(0f, 1f)
}
