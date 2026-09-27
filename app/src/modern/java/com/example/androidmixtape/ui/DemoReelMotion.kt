package com.example.androidmixtape.ui

import com.example.androidmixtape.playback.TransportCueDirection

/** Viewed from the labelled face: play/forward is anticlockwise, rewind is clockwise.
 * Constant ribbon speed makes the smaller pack turn faster. Cue winding is six times play.
 */
internal fun demoReelDegreesPerSecond(radius: Float, playing: Boolean, cue: TransportCueDirection): Float {
    val speed = when(cue) {
        TransportCueDirection.FAST_FORWARD -> -540f
        TransportCueDirection.REWIND -> 540f
        TransportCueDirection.NONE -> if(playing) -90f else 0f
    }
    val safeRadius = if(radius.isFinite()) radius.coerceIn(30f,79f) else 79f
    return speed*79f/safeRadius
}

internal fun demoAdvanceReel(angle: Float, seconds: Float, speed: Float): Float {
    val advance = if(seconds.isFinite()) seconds.coerceIn(0f,.05f)*speed else 0f
    return ((angle+advance)%360f+360f)%360f
}
