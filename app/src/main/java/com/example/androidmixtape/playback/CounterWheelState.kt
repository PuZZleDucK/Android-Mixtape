package com.example.androidmixtape.playback

/** Presentation state only. The playback counter remains the authoritative value. */
data class CounterWheelState private constructor(
    val target: Int,
    val outgoing: Int,
    val revision: Long,
    val generation: Long,
    val rolling: Boolean,
) {
    companion object {
        fun settled(value: Int, revision: Long = 0L): CounterWheelState {
            val target = value.coerceIn(0, 999)
            return CounterWheelState(target, target, revision, 0L, false)
        }
    }

    /**
     * revision identifies a transport discontinuity, including an adjacent-value seek.
     * Duplicate samples preserve an active roll; a competing target cancels it rather
     * than queuing another roll. Pause and disabled motion settle even duplicate samples.
     */
    fun update(
        value: Int,
        playing: Boolean,
        revision: Long = this.revision,
        motionEnabled: Boolean = true,
    ): CounterWheelState {
        val next = value.coerceIn(0, 999)
        val eligible = playing && motionEnabled && revision == this.revision
        if (next == target && eligible) return this
        if (next == target && !rolling && revision == this.revision) return this
        val animate = eligible && !rolling && next == target + 1
        return CounterWheelState(
            target = next,
            outgoing = if (animate) target else next,
            revision = revision,
            generation = generation + 1L,
            rolling = animate,
        )
    }

    /** A cancelled animation's completion must never restore an older target. */
    fun complete(animationGeneration: Long): CounterWheelState =
        if (rolling && generation == animationGeneration) copy(outgoing = target, rolling = false) else this

    /** Offsets are fractions of the fixed clipped cell height; positive is downward. */
    fun frame(progress: Float): List<CounterWheelCell> {
        val p = if (progress.isNaN()) 1f else progress.coerceIn(0f, 1f)
        val from = outgoing.toString().padStart(3, '0')
        val to = target.toString().padStart(3, '0')
        return to.indices.map { index ->
            if (rolling && from[index] != to[index] && p < 1f) {
                CounterWheelCell(index, listOf(
                    CounterWheelGlyph(from[index], p),
                    CounterWheelGlyph(to[index], p - 1f),
                ))
            } else {
                CounterWheelCell(index, listOf(CounterWheelGlyph(to[index], 0f)))
            }
        }
    }
}

data class CounterWheelCell(val index: Int, val glyphs: List<CounterWheelGlyph>)
data class CounterWheelGlyph(val digit: Char, val offsetInCellHeights: Float)
