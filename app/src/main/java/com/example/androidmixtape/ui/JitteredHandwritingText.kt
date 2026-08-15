package com.example.androidmixtape.ui

import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.random.Random

const val HANDWRITING_PERTURBATION_COUNT: Int = 5_000
const val HANDWRITING_TRACK_OFFSET: Int = 200
const val HANDWRITING_TRACK_STRIDE: Int = 100
private const val MINIMUM_PERTURBATION_FRACTION: Float = 0.4f
private const val LARGE_HORIZONTAL_PERTURBATION: Float = 0.65f
private const val MAXIMUM_READABLE_ROTATION_DEGREES: Float = 4f
private const val PERTURBATION_TABLE_SEED: Int = 0x4D495854

data class HandwritingJitterStrength(
    val maxDxEm: Float,
    val maxDyEm: Float,
    val maxRotationDegrees: Float,
    val maxTrackingEm: Float,
)

data class HandwritingJitterSample(
    val dxEm: Float,
    val dyEm: Float,
    val rotationDegrees: Float,
    val trackingEm: Float,
)

enum class HandwritingJitterTokenization {
    Character,
    Word,
}

private val handwritingPerturbations: List<HandwritingJitterSample> by lazy {
    val random = Random(PERTURBATION_TABLE_SEED)
    val samples = ArrayList<HandwritingJitterSample>(HANDWRITING_PERTURBATION_COUNT)
    val uniqueSamples = HashSet<HandwritingJitterSample>(HANDWRITING_PERTURBATION_COUNT)
    while (samples.size < HANDWRITING_PERTURBATION_COUNT) {
        val candidate = HandwritingJitterSample(
            dxEm = random.normalizedPerturbation(),
            dyEm = random.normalizedPerturbation(),
            rotationDegrees = random.normalizedPerturbation(),
            trackingEm = random.normalizedPerturbation(),
        )
        val followsLargeRightWithLargeLeft = samples.lastOrNull()?.squishesNext(candidate) == true
        val squishesAcrossTableBoundary = samples.size == HANDWRITING_PERTURBATION_COUNT - 1 &&
            candidate.squishesNext(samples.first())
        if (!followsLargeRightWithLargeLeft && !squishesAcrossTableBoundary && uniqueSamples.add(candidate)) {
            samples += candidate
        }
    }
    samples
}

fun handwritingJitterSamples(
    startIndex: Int,
    tokenCount: Int,
    strength: HandwritingJitterStrength,
): List<HandwritingJitterSample> {
    if (tokenCount <= 0) return emptyList()
    val readableStrength = strength.copy(
        maxRotationDegrees = min(strength.maxRotationDegrees.absoluteValue, MAXIMUM_READABLE_ROTATION_DEGREES),
    )
    return List(tokenCount) { tokenIndex ->
        val normalized = handwritingPerturbations[handwritingPerturbationIndex(startIndex + tokenIndex)]
        HandwritingJitterSample(
            dxEm = normalized.dxEm * readableStrength.maxDxEm.absoluteValue,
            dyEm = normalized.dyEm * readableStrength.maxDyEm.absoluteValue,
            rotationDegrees = normalized.rotationDegrees * readableStrength.maxRotationDegrees,
            trackingEm = normalized.trackingEm * readableStrength.maxTrackingEm.absoluteValue,
        )
    }
}

fun handwritingTrackStartIndex(mixtapeStartIndex: Int, trackIndex: Int): Int =
    handwritingPerturbationIndex(mixtapeStartIndex + HANDWRITING_TRACK_OFFSET + trackIndex * HANDWRITING_TRACK_STRIDE)

fun handwritingPerturbationIndex(index: Int): Int = Math.floorMod(index, HANDWRITING_PERTURBATION_COUNT)

fun handwritingPerturbationTable(): List<HandwritingJitterSample> = handwritingPerturbations

fun handwritingTokenAcceptsPerturbation(token: String): Boolean {
    if (token.isEmpty()) return false
    var index = 0
    while (index < token.length) {
        val codePoint = Character.codePointAt(token, index)
        if (!Character.isLetterOrDigit(codePoint)) return false
        index += Character.charCount(codePoint)
    }
    return true
}

private fun HandwritingJitterSample.squishesNext(next: HandwritingJitterSample): Boolean =
    dxEm >= LARGE_HORIZONTAL_PERTURBATION && next.dxEm <= -LARGE_HORIZONTAL_PERTURBATION

private fun Random.normalizedPerturbation(): Float {
    val signedUnit = nextFloat() * 2f - 1f
    val sign = if (signedUnit < 0f) -1f else 1f
    val magnitude = MINIMUM_PERTURBATION_FRACTION +
        signedUnit.absoluteValue * (1f - MINIMUM_PERTURBATION_FRACTION)
    return sign * magnitude
}
