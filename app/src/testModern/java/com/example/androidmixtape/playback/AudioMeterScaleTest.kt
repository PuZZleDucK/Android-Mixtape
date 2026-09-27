package com.example.androidmixtape.playback

import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioMeterScaleTest {
    @Test fun decibelReferenceLevelsHavePredictableHeights() {
        listOf(-30.0 to 0f, -24.0 to 0.2f, -18.0 to 0.4f, -12.0 to 0.6f, -6.0 to 0.8f, 0.0 to 1f).forEach { (db, height) ->
            assertEquals("$db dBFS", height, rmsToMeter(10.0.pow(db / 20.0)), 0.000_001f)
        }
    }

    @Test fun ordinaryMusicNoLongerLooksNearlyFull() {
        assertEquals(0.6f, rmsToMeter(10.0.pow(-12.0 / 20.0)), 0.000_001f)
        assertTrue(rmsToMeter(0.25) < 0.61f)
    }

    @Test fun silenceAndSignalsBelowTheDisplayFloorStayAtZero() {
        listOf(0.0, 0.000_001, 0.001, 0.01).forEach { assertEquals(0f, rmsToMeter(it), 0f) }
    }

    @Test fun invalidInputIsSafeAndOverRangeInputIsClamped() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.0).forEach {
            assertEquals(0f, rmsToMeter(it), 0f)
        }
        assertEquals(1f, rmsToMeter(1.0), 0f)
        assertEquals(1f, rmsToMeter(2.0), 0f)
    }

    @Test fun increasingAmplitudeNeverLowersTheMeter() {
        val heights = (0..1500).map { rmsToMeter(it / 1000.0) }
        assertTrue(heights.all { it in 0f..1f })
        assertTrue(heights.zipWithNext().all { (previous, next) -> next >= previous })
    }
}
