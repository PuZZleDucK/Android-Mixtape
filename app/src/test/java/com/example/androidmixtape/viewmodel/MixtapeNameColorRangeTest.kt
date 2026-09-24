package com.example.androidmixtape.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeNameColorRangeTest {
    @Test fun nameColorOffersMoreThanTheFiveExistingInks() {
        val colors = MixtapeSymbolColor.entries
        assertTrue("Name color needs at least nine choices, including new hue families", colors.size >= 9)
        assertEquals(
            "Keep the five saved enum names and their cycle order for existing tapes",
            listOf("Navy", "Red", "Green", "Purple", "Amber"),
            colors.take(5).map { it.name },
        )
    }

    @Test fun tappingCanReachEveryNameColorAndWrapWithoutGettingStuck() {
        val colors = MixtapeSymbolColor.entries
        var current = colors.first()
        val visited = mutableSetOf<MixtapeSymbolColor>()
        repeat(colors.size) {
            assertTrue("Cycle repeated $current before visiting the whole palette", visited.add(current))
            current = current.next()
        }
        assertEquals(colors.first(), current)
    }
}
