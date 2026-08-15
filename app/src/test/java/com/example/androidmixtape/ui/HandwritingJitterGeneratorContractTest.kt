package com.example.androidmixtape.ui

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwritingJitterGeneratorContractTest {
    @Test
    fun sharedPerturbationTableContainsFiveThousandUniquePatterns() {
        val table = handwritingPerturbationTable()

        assertEquals(HANDWRITING_PERTURBATION_COUNT, table.size)
        assertEquals("Each four-value perturbation pattern should be unique.", table.size, table.distinct().size)
    }

    @Test
    fun largeRightShiftIsNeverFollowedByLargeLeftShift() {
        val table = handwritingPerturbationTable()
        val adjacentPairs = table.zipWithNext() + listOf(table.last() to table.first())

        assertTrue(
            "The perturbation array must not deliberately squeeze adjacent characters together.",
            adjacentPairs.none { (current, next) -> current.dxEm >= 0.65f && next.dxEm <= -0.65f },
        )
    }

    @Test
    fun punctuationSpacesAndEmojiDoNotAcceptPerturbations() {
        listOf("!", "?", ".", ",", "—", " ", "😍", "🫠").forEach { token ->
            assertTrue("Expected '$token' to remain unmodified.", !handwritingTokenAcceptsPerturbation(token))
        }
        listOf("a", "Z", "5", "é").forEach { token ->
            assertTrue("Expected '$token' to receive handwriting perturbation.", handwritingTokenAcceptsPerturbation(token))
        }
    }

    @Test
    fun charactersConsumeSequentialPerturbationsFromTheMixtapeStartIndex() {
        val unitStrength = HandwritingJitterStrength(1f, 1f, 1f, 1f)
        val table = handwritingPerturbationTable()

        val samples = handwritingJitterSamples(startIndex = 3, tokenCount = 4, strength = unitStrength)

        assertEquals(table[3], samples[0])
        assertEquals(table[4], samples[1])
        assertEquals(table[5], samples[2])
        assertEquals(table[6], samples[3])
    }

    @Test
    fun eachTrackStartsOneHundredEntriesAfterThePreviousTrack() {
        assertEquals(203, handwritingTrackStartIndex(mixtapeStartIndex = 3, trackIndex = 0))
        assertEquals(303, handwritingTrackStartIndex(mixtapeStartIndex = 3, trackIndex = 1))
        assertEquals(403, handwritingTrackStartIndex(mixtapeStartIndex = 3, trackIndex = 2))
    }

    @Test
    fun perturbationsWrapAroundTheSharedTable() {
        val strength = HandwritingJitterStrength(1f, 1f, 1f, 1f)
        val samples = handwritingJitterSamples(
            startIndex = HANDWRITING_PERTURBATION_COUNT - 1,
            tokenCount = 2,
            strength = strength,
        )
        val table = handwritingPerturbationTable()

        assertEquals(table.last(), samples[0])
        assertEquals(table.first(), samples[1])
    }

    @Test
    fun generatedValuesHaveVisibleMinimumMagnitudeAndStayWithinBounds() {
        val strength = HandwritingJitterStrength(
            maxDxEm = 0.06f,
            maxDyEm = 0.105f,
            maxRotationDegrees = 3f,
            maxTrackingEm = 0.023f,
        )
        val samples = handwritingJitterSamples(startIndex = 37, tokenCount = 256, strength = strength)

        samples.forEach { sample ->
            assertInVisibleRange(sample.dxEm, strength.maxDxEm)
            assertInVisibleRange(sample.dyEm, strength.maxDyEm)
            assertInVisibleRange(sample.rotationDegrees, strength.maxRotationDegrees)
            assertInVisibleRange(sample.trackingEm, strength.maxTrackingEm)
        }
    }

    private fun assertInVisibleRange(value: Float, maximum: Float) {
        val magnitude = abs(value)
        assertTrue("Expected $value to stay within +/-$maximum.", magnitude <= maximum + EPSILON)
        assertTrue("Expected $value to be at least 40% of $maximum.", magnitude + EPSILON >= maximum * 0.4f)
    }

    private companion object {
        const val EPSILON = 0.0001f
    }
}
