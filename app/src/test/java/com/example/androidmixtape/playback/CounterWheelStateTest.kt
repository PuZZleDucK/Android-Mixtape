package com.example.androidmixtape.playback

import org.junit.Assert.*
import org.junit.Test

class CounterWheelStateTest {
    private fun digits(state: CounterWheelState): String =
        state.frame(1f).joinToString("") { it.glyphs.single().digit.toString() }

    @Test fun initialValuesAndClampsAreSettled() {
        listOf(-1 to "000", 0 to "000", 613 to "613", 999 to "999", 1000 to "999").forEach { (value, text) ->
            val state = CounterWheelState.settled(value)
            assertFalse(state.rolling)
            assertEquals(text, digits(state))
            assertTrue(state.frame(0f).all { it.glyphs.single().offsetInCellHeights == 0f })
        }
    }

    @Test fun singleIncrementsAndCarriesUseSharedDownwardProgress() {
        listOf(8 to setOf(2), 9 to setOf(1, 2), 99 to setOf(0, 1, 2), 998 to setOf(2)).forEach { (old, changed) ->
            val state = CounterWheelState.settled(old).update(old + 1, playing = true)
            assertTrue(state.rolling)
            listOf(0f, .25f, .5f, .75f).forEach { progress ->
                val frame = state.frame(progress)
                assertEquals(listOf(0, 1, 2), frame.map { it.index })
                frame.forEach { cell ->
                    if (cell.index in changed) {
                        assertEquals(2, cell.glyphs.size)
                        assertEquals(progress, cell.glyphs[0].offsetInCellHeights, 0f)
                        assertEquals(progress - 1f, cell.glyphs[1].offsetInCellHeights, 0f)
                        assertEquals(old.toString().padStart(3, '0')[cell.index], cell.glyphs[0].digit)
                        assertEquals((old + 1).toString().padStart(3, '0')[cell.index], cell.glyphs[1].digit)
                    } else {
                        assertEquals(1, cell.glyphs.size)
                        assertEquals(0f, cell.glyphs.single().offsetInCellHeights, 0f)
                    }
                }
            }
            assertEquals((old + 1).toString().padStart(3, '0'), digits(state))
            assertFalse(state.complete(state.generation).rolling)
        }
    }

    @Test fun resetReverseAndLargeJumpsSnapWithoutIntermediateValues() {
        listOf(999 to 0, 100 to 99, 613 to 700, 0 to 999).forEach { (old, next) ->
            val state = CounterWheelState.settled(old).update(next, playing = true)
            assertFalse(state.rolling)
            assertEquals(next.toString().padStart(3, '0'), digits(state))
            assertEquals(next, state.outgoing)
        }
    }

    @Test fun adjacentSeeksAndTrackChangesSnapUsingRevision() {
        val state = CounterWheelState.settled(9, revision = 4).update(10, playing = true, revision = 5)
        assertFalse(state.rolling)
        assertEquals("010", digits(state))
        assertTrue(state.update(11, playing = true, revision = 5).rolling)
    }

    @Test fun duplicateSamplesDoNotRestartButNewTargetsCancel() {
        val rolling = CounterWheelState.settled(8).update(9, playing = true)
        assertSame(rolling, rolling.update(9, playing = true))
        val latest = rolling.update(10, playing = true)
        assertFalse(latest.rolling)
        assertEquals("010", digits(latest.complete(rolling.generation)))
        assertSame(latest, latest.complete(rolling.generation))
        val nextRoll = latest.update(11, playing = true)
        assertSame(nextRoll, nextRoll.complete(rolling.generation))
        assertTrue(nextRoll.rolling)
    }

    @Test fun resetDuringCarryInvalidatesOldCompletion() {
        val carry = CounterWheelState.settled(99).update(100, playing = true)
        val reset = carry.update(0, playing = true, revision = 1)
        assertFalse(reset.rolling)
        assertSame(reset, reset.complete(carry.generation))
        assertEquals("000", digits(reset))
    }

    @Test fun pauseAndMotionDisableSettleWithoutSpuriousIncrement() {
        val roll = CounterWheelState.settled(9).update(10, playing = true)
        listOf(
            roll.update(10, playing = false),
            roll.update(10, playing = true, motionEnabled = false),
            roll.update(10, playing = true, revision = 1),
        ).forEach { stopped ->
            assertFalse(stopped.rolling)
            assertEquals("010", digits(stopped))
            assertSame(stopped, stopped.complete(roll.generation))
            assertSame(stopped, stopped.update(10, playing = false))
        }
        assertFalse(CounterWheelState.settled(9).update(10, playing = false).rolling)
        assertFalse(CounterWheelState.settled(9).update(10, playing = true, motionEnabled = false).rolling)
    }

    @Test fun recreationStartsAtLatestTargetAndProgressIsBounded() {
        val roll = CounterWheelState.settled(99).update(100, playing = true)
        val recreated = CounterWheelState.settled(roll.target, roll.revision)
        assertFalse(recreated.rolling)
        assertEquals("100", digits(recreated))
        assertEquals(roll.frame(0f), roll.frame(-1f))
        assertEquals(roll.frame(1f), roll.frame(2f))
        assertEquals(roll.frame(1f), roll.frame(Float.NaN))
    }
}
