package com.example.androidmixtape.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionSamplerTest {
    @Test fun onlyChangedPlayingPositionsPublish() {
        assertFalse(shouldPublishPositionSample(false, 1, 800L, 400L))
        assertFalse(shouldPublishPositionSample(true, 0, 800L, 400L))
        assertFalse(shouldPublishPositionSample(true, 1, 400L, 400L))
        assertTrue(shouldPublishPositionSample(true, 1, 800L, 400L))
        assertTrue(shouldPublishPositionSample(true, 1, 100L, 800L)) // backward seek
        assertFalse(shouldPublishPositionSample(true, 1, -10L, 0L))
    }
}
