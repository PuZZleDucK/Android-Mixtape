package com.example.androidmixtape.ui

import com.example.androidmixtape.viewmodel.DeckTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Planning-first executable specification. Production helpers intentionally absent.
class SegmentedDeckMeterTest {
    @Test fun onlyTwoSkinsOptIn() {
        DeckTheme.values().forEach { theme ->
            assertEquals(theme == DeckTheme.BlackoutPortable || theme == DeckTheme.SunsetBoombox,
                theme.usesSegmentedMeter())
        }
    }

    @Test fun levelsRiseFromTheBottomAtExactThresholds() {
        assertEquals(0, litMeterSegments(0f, true, 0))
        for (count in 1..10) {
            val threshold = count / 10f
            assertEquals(count - 1, litMeterSegments(threshold - 0.001f, true, 0))
            assertEquals(count, litMeterSegments(threshold, true, 0))
        }
        assertEquals(2, litMeterSegments(0.25f, true, 0))
        assertEquals(5, litMeterSegments(0.55f, true, 0))
        assertEquals(9, litMeterSegments(0.95f, true, 0))
    }

    @Test fun clampingAndInactiveInputsNeverLeaveFalseLights() {
        assertEquals(10, litMeterSegments(2f, true, 0))
        for (level in listOf(-1f, 0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(0, litMeterSegments(level, true, 10))
        }
        assertEquals(0, litMeterSegments(1f, false, 10))
        assertEquals(0, litMeterSegments(null, true, 10))
        assertEquals(5, litMeterSegments(0.5f, true, -100))
        assertEquals(10, litMeterSegments(1f, true, 100))
    }

    @Test fun fallingThresholdHasTwoPercentHysteresis() {
        assertEquals(5, litMeterSegments(0.49f, true, 5))
        assertEquals(5, litMeterSegments(0.48f, true, 5))
        assertEquals(4, litMeterSegments(0.479f, true, 5))
        assertEquals(2, litMeterSegments(0.25f, true, 10))
        assertEquals(0, litMeterSegments(0.079f, true, 1))
    }

    @Test fun stereoStateIsIndependentAndPauseClearsHistory() {
        val left = litMeterSegments(0.8f, true, 0)
        val right = litMeterSegments(0.2f, true, 0)
        assertEquals(8, left)
        assertEquals(2, right)
        val paused = litMeterSegments(0.8f, false, left)
        assertEquals(0, paused)
        assertEquals(0, litMeterSegments(0.09f, true, paused))
    }
}
