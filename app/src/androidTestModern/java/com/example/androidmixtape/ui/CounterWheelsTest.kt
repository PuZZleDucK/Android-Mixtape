package com.example.androidmixtape.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidmixtape.viewmodel.DeckTheme
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.math.roundToInt
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterWheelsTest {
    @get:Rule val compose = createComposeRule()
    private val value = mutableStateOf(8)
    private val playing = mutableStateOf(true)
    private val revision = mutableStateOf(0L)
    private val motion = mutableStateOf(true)
    private val counterVisible = mutableStateOf(true)
    private val deckTheme = mutableStateOf(DeckTheme.SilverfaceHiFi)

    private fun show(windowHeight: androidx.compose.ui.unit.Dp = 30.dp) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AndroidMixtapeTheme {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(84.dp, windowHeight).testTag("window"), contentAlignment = Alignment.Center) {
                        if (counterVisible.value) {
                            CounterWheels(value.value, playing.value, revision.value, Color.White,
                                Modifier.testTag("digits"), motion.value, deckTheme.value)
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        // Activity window transitions are outside the frozen Compose clock.
        android.os.SystemClock.sleep(500)
    }

    private fun update(next: Int, discontinuity: Boolean = false) {
        compose.runOnIdle { value.value = next; if (discontinuity) revision.value++ }
        // First frame recomposes, second frame establishes the animation's start time.
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun frame(name: String): Bitmap {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // API 24 screenshots use SurfaceFlinger, not PixelCopy. Give the Android
        // draw pass time to submit the frame without advancing the Compose clock.
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(100)
        val screen = instrumentation.uiAutomation.takeScreenshot()
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "counter-wheels").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { screen.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val bounds = compose.onNodeWithTag("digits").fetchSemanticsNode().boundsInWindow
        return Bitmap.createBitmap(screen, bounds.left.roundToInt(), bounds.top.roundToInt(),
            bounds.width.roundToInt(), bounds.height.roundToInt())
    }

    private fun equalPixels(a: Bitmap, b: Bitmap, fromX: Int = 0, toX: Int = a.width): Boolean {
        if (a.width != b.width || a.height != b.height) return false
        return (fromX until toX).all { x -> (0 until a.height).all { y -> a.getPixel(x, y) == b.getPixel(x, y) } }
    }

    @Test fun digitalCarryIsImmediateAndThemeSwitchSettlesMechanicalRoll() {
        deckTheme.value = DeckTheme.BlackoutPortable
        value.value = 9
        show()
        val nine = frame("digital-009")
        update(10)
        val ten = frame("digital-010")
        assertFalse(equalPixels(nine, ten))
        compose.mainClock.advanceTimeBy(250)
        assertTrue(equalPixels(ten, frame("digital-010-still")))
        compose.onNodeWithContentDescription("Tape counter 010").assertExists()
        compose.runOnIdle { deckTheme.value = DeckTheme.SilverfaceHiFi }
        compose.mainClock.advanceTimeByFrame()
        val mechanical = frame("mechanical-010")
        assertFalse("Both themes must use different digit shapes", equalPixels(mechanical, ten))
        compose.runOnIdle { deckTheme.value = DeckTheme.BlackoutPortable }
        compose.mainClock.advanceTimeByFrame()
        assertTrue(equalPixels(ten, frame("digital-010-return")))
        compose.runOnIdle { motion.value = false }
        update(11)
        val reduced = frame("digital-reduced-011")
        compose.mainClock.advanceTimeBy(250)
        assertTrue(equalPixels(reduced, frame("digital-reduced-011-still")))
    }

    @Test fun compactWindowKeepsSettledNumeralsInsideBothEdges() {
        value.value = 20
        show(windowHeight = 16.dp)
        val image = frame("compact-window-020")
        val litRows = (0 until image.height).filter { y ->
            (0 until image.width).any { x -> android.graphics.Color.red(image.getPixel(x, y)) > 128 }
        }
        assertTrue("Digits must be visible", litRows.isNotEmpty())
        assertTrue("Top of numeral must not touch clip", litRows.first() > 0)
        assertTrue("Bottom of numeral must not touch clip", litRows.last() < image.height - 1)
    }

    @Test fun singleIncrementAndCarriesKeepFixedCellsAndFinishCorrectly() {
        show()
        for ((from, to) in listOf(8 to 9, 9 to 10, 99 to 100)) {
            update(from, discontinuity = true)
            val before = frame("${from}-${to}-00-before")
            update(to)
            frame("${from}-${to}-01-start")
            compose.mainClock.advanceTimeBy(32)
            val early = frame("${from}-${to}-015-early")
            if (from == 8) {
                fun litRows(image: Bitmap) = (0 until image.height).filter { y ->
                    (image.width * 2 / 3 until image.width).any { x ->
                        android.graphics.Color.red(image.getPixel(x, y)) > 128
                    }
                }
                assertTrue("Incoming digit enters above the old baseline", litRows(early).first() < litRows(before).first())
                assertTrue("Outgoing digit exits below the old baseline", litRows(early).last() > litRows(before).last())
            }
            compose.mainClock.advanceTimeBy(32)
            val middle = frame("${from}-${to}-02-middle")
            assertEquals(before.width, middle.width)
            assertEquals(before.height, middle.height)
            assertFalse("Moving glyph pixels must differ", equalPixels(before, middle))
            if (from == 8) assertTrue("Hundreds and tens must stay still", equalPixels(before, middle, 0, before.width * 2 / 3))
            if (from == 9) assertTrue("Hundreds must stay still", equalPixels(before, middle, 0, before.width / 3))
            compose.mainClock.advanceTimeBy(200)
            val finished = frame("${from}-${to}-03-finished")
            compose.onNodeWithContentDescription("Tape counter ${to.toString().padStart(3, '0')}").assertExists()
            update(to, discontinuity = true)
            assertTrue("Completed pixels must equal an immediate settled render", equalPixels(finished, frame("${from}-${to}-04-reference")))
        }
    }

    @Test fun platformZeroScaleSettlesWithoutWheelMotion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        org.junit.Assume.assumeTrue(android.provider.Settings.Global.getFloat(
            context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f)
        show()
        update(9)
        // Scale zero still needs a frame to process animation completion, not 160ms.
        compose.mainClock.advanceTimeByFrame()
        val immediate = frame("platform-zero-009")
        update(9, discontinuity = true)
        assertTrue("Platform zero scale must match a settled render", equalPixels(immediate, frame("platform-zero-009-reference")))
    }

    @Test fun disablingPlatformMotionDuringCarrySettlesImmediately() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val key = android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
        val original = android.provider.Settings.Global.getString(resolver, key)
        fun scale(value: String?) {
            val command = if (value == null) "settings delete global $key" else "settings put global $key $value"
            instrumentation.uiAutomation.executeShellCommand(command).use {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
            }
            instrumentation.waitForIdleSync()
            android.os.SystemClock.sleep(200)
        }
        try {
            scale("1")
            show()
            update(99, discontinuity = true)
            val before = frame("scale-change-099")
            update(100)
            compose.mainClock.advanceTimeBy(32)
            assertFalse(equalPixels(before, frame("scale-change-moving-100")))
            scale("0")
            compose.mainClock.advanceTimeByFrame()
            val stopped = frame("scale-change-stopped-100")
            update(100, discontinuity = true)
            assertTrue("Scale change must settle without finishing the tween", equalPixels(stopped, frame("scale-change-reference-100")))
            compose.mainClock.advanceTimeBy(500)
            assertTrue("Cancelled carry must not return", equalPixels(stopped, frame("scale-change-later-100")))
        } finally {
            scale(original)
        }
    }

    @Test fun remountDuringCarryStartsAtLatestValueWithoutStaleCompletion() {
        show()
        update(99, discontinuity = true)
        val before = frame("remount-099")
        update(100)
        compose.mainClock.advanceTimeBy(32)
        assertFalse(equalPixels(before, frame("remount-moving-100")))
        compose.runOnIdle { counterVisible.value = false }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("digits").assertDoesNotExist()
        // The composition and its animation coroutine are destroyed. The current
        // player value survives outside it, as it does across layout replacement.
        compose.runOnIdle { value.value = 321; counterVisible.value = true }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithContentDescription("Tape counter 321").assertExists()
        val remounted = frame("remount-current-321")
        compose.mainClock.advanceTimeBy(500)
        assertTrue("Disposed carry must not restore 100", equalPixels(remounted, frame("remount-later-321")))
        update(321, discontinuity = true)
        assertTrue("Remount must start settled", equalPixels(remounted, frame("remount-reference-321")))
    }

    @Test fun interruptionsAndRepeatedUpdatesNeverRestoreStaleDigits() {
        show()
        update(9)
        compose.mainClock.advanceTimeBy(32)
        update(10)
        val cancelled = frame("cancelled-010")
        compose.mainClock.advanceTimeBy(500)
        assertTrue(equalPixels(cancelled, frame("cancelled-010-later")))
        update(11)
        compose.runOnIdle { playing.value = false }
        compose.mainClock.advanceTimeByFrame()
        val paused = frame("paused-011")
        compose.mainClock.advanceTimeBy(500)
        assertTrue(equalPixels(paused, frame("paused-011-later")))
        compose.runOnIdle { playing.value = true; motion.value = false }
        update(12)
        val disabled = frame("disabled-012")
        compose.mainClock.advanceTimeBy(500)
        assertTrue(equalPixels(disabled, frame("disabled-012-later")))
        compose.runOnIdle { motion.value = true }
        update(13, discontinuity = true)
        val seek = frame("seek-013")
        compose.mainClock.advanceTimeBy(500)
        assertTrue(equalPixels(seek, frame("seek-013-later")))
        update(0)
        compose.onNodeWithContentDescription("Tape counter 000").assertExists()
    }
}
