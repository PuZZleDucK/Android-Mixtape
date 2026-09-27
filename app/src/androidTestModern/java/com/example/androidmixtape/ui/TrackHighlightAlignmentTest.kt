package com.example.androidmixtape.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.viewmodel.*
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TrackHighlightAlignmentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun trackInkFitsAndCentersWithinItsHighlightForEveryFont() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("alignmentPhase") ?: "checked"
        val messiness = HandwritingMessiness.valueOf(args.getString("alignmentMessiness") ?: "Off")
        val fontSize = HandwritingFontSize.valueOf(args.getString("alignmentFontSize") ?: "Large")
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "track-alignment/$phase").apply { mkdirs() }
        val font = mutableStateOf(MixtapeHandwritingFont.Kalam)
        val track = Track(901, "Agjpqy Night drive", "Agjpqy", 181_000, "preview://alignment")
        compose.setContent {
            key(font.value) {
                AndroidMixtapeTheme {
                    MixtapeApp(
                        state = MixtapeUiState(
                            status = LibraryStatus.Ready, screen = MixtapeScreen.NowPlaying,
                            tracks = listOf(track), queueTracks = listOf(track),
                            currentTrack = track, currentIndex = 0, currentMixtapeName = "Night drive",
                            currentMixtapeVisualProperties = MixtapeVisualProperties(
                                handwritingFont = font.value, sleeveTheme = SleeveTheme.BlankWhite,
                                caseTheme = CaseTheme.CrystalClear,
                            ),
                            mixtapeSettings = MixtapeSettings(handwritingMessiness = messiness, handwritingFontSize = fontSize),
                        ),
                        onRequestPermission = {}, onRefresh = {}, onTogglePlayPause = {},
                        onPrevious = {}, onNext = {}, onSeekTo = {},
                    )
                }
            }
        }
        val failures = mutableListOf<String>()
        val measurements = mutableListOf<String>()
        for (family in MixtapeHandwritingFont.entries) {
            compose.runOnIdle { font.value = family }
            compose.waitForIdle()
            android.os.SystemClock.sleep(400)
            val node = compose.onNodeWithContentDescription("Track actions for ${track.title}")
            val bounds = node.fetchSemanticsNode().boundsInWindow
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(directory, "${family.name}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val scanTop = (bounds.top - bounds.height * 0.2f).roundToInt().coerceAtLeast(0)
            val scanBottom = (bounds.bottom + bounds.height).roundToInt().coerceAtMost(bitmap.height)
            var inkTop = bitmap.height
            var inkBottom = -1
            for (y in scanTop until scanBottom) {
                for (x in bounds.left.roundToInt().coerceAtLeast(0) until bounds.right.roundToInt().coerceAtMost(bitmap.width)) {
                    val pixel = bitmap.getPixel(x, y)
                    // Selected track ink is red; paper, plastic and unselected text are neutral.
                    if (Color.red(pixel) > Color.green(pixel) + 50 && Color.green(pixel) < 150 && Color.blue(pixel) < 150) {
                        inkTop = minOf(inkTop, y)
                        inkBottom = maxOf(inkBottom, y)
                    }
                }
            }
            bitmap.recycle()
            val centerError = abs((inkTop + inkBottom + 1) / 2f - bounds.center.y)
            val description = "${family.name}: row=${bounds.top}..${bounds.bottom}, ink=$inkTop..${inkBottom + 1}, centerError=$centerError"
            measurements += description
            if (inkBottom < inkTop || inkTop < bounds.top - 1 || inkBottom + 1 > bounds.bottom + 1 || centerError > bounds.height * 0.10f) {
                failures += description
            }
            // Capture the actual pressed indication while the finger is down.
            node.performTouchInput { down(center) }
            android.os.SystemClock.sleep(180)
            val pressed = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(directory, "${family.name}-pressed.png").outputStream().use { pressed.compress(Bitmap.CompressFormat.PNG, 100, it) }
            pressed.recycle()
            node.performTouchInput { up() }
        }
        File(directory, "measurements.txt").writeText(measurements.joinToString("\n"))
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
