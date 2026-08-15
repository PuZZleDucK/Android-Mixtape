package com.example.androidmixtape.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class HandwritingMessinessSettingsTest {
    @Test
    fun defaultMessinessIsLow() {
        assertEquals(HandwritingMessiness.Low, MixtapeSettings().handwritingMessiness)
    }

    @Test
    fun levelsExposeFullHalfAndZeroStrength() {
        assertEquals(1f, HandwritingMessiness.High.strengthMultiplier)
        assertEquals(0.5f, HandwritingMessiness.Low.strengthMultiplier)
        assertEquals(0f, HandwritingMessiness.Off.strengthMultiplier)
    }

    @Test
    fun settingsStoreRetainsSelectedMessiness() {
        val store = InMemoryMixtapeSettingsStore()
        store.saveSettings(store.settings().copy(handwritingMessiness = HandwritingMessiness.Off))

        assertEquals(HandwritingMessiness.Off, store.settings().handwritingMessiness)
    }
}
