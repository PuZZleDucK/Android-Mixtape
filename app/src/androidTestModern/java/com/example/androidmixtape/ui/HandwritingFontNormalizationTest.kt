package com.example.androidmixtape.ui

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.viewmodel.HandwritingFontSize
import com.example.androidmixtape.viewmodel.HandwritingMessiness
import com.example.androidmixtape.viewmodel.LibraryStatus
import com.example.androidmixtape.viewmodel.MixtapeHandwritingFont
import com.example.androidmixtape.viewmodel.MixtapeScreen
import com.example.androidmixtape.viewmodel.MixtapeSettings
import com.example.androidmixtape.viewmodel.MixtapeUiState
import com.example.androidmixtape.viewmodel.MixtapeVisualProperties
import com.example.androidmixtape.viewmodel.CaseTheme
import com.example.androidmixtape.viewmodel.SleeveTheme
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/** Compare the real Now Playing track rows, not just the font-size preference value. */
class HandwritingFontNormalizationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun visibleLetterHeightIsComparableWithinEachSize() {
        val font = mutableStateOf(MixtapeHandwritingFont.Kalam)
        val size = mutableStateOf(HandwritingFontSize.Large)
        var inkHeight = 0f
        compose.setContent {
            val measurer = rememberTextMeasurer()
            val style = TextStyle(
                fontFamily = font.value.cassetteHandwritingFontFamily(),
                fontSize = 40.sp * (size.value.scale * font.value.opticalScale()),
                fontWeight = font.value.effectiveCassetteWeight(FontWeight.Bold),
            )
            val bounds = rememberHandwritingInkBounds(measurer, style)
            inkHeight = bounds.bottom - bounds.top
        }
        val failures = mutableListOf<String>()
        for (selectedSize in HandwritingFontSize.entries) {
            val heights = MixtapeHandwritingFont.entries.map { selectedFont ->
                compose.runOnIdle { size.value = selectedSize; font.value = selectedFont }
                compose.waitForIdle()
                selectedFont to inkHeight
            }
            val reference = heights.first().second
            for ((family, height) in heights) {
                if (height !in reference * 0.85f..reference * 1.15f) {
                    failures += "$selectedSize $family: ink height $height vs Kalam $reference"
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun trackRowPitchIsIndependentOfFontAtEachSize() {
        val first = Track(901, "Night drive", "Evening radio", 180_000, "preview://first")
        val second = Track(902, "Morning light", "Evening radio", 190_000, "preview://second")
        val font = mutableStateOf(MixtapeHandwritingFont.Kalam)
        val size = mutableStateOf(HandwritingFontSize.Large)
        compose.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(
                    state = MixtapeUiState(
                        status = LibraryStatus.Ready, screen = MixtapeScreen.NowPlaying,
                        tracks = listOf(first, second), queueTracks = listOf(first, second),
                        currentTrack = first, currentIndex = 0, currentMixtapeName = "Night drive",
                        currentMixtapeVisualProperties = MixtapeVisualProperties(
                            handwritingFont = font.value, sleeveTheme = SleeveTheme.BlankWhite,
                            caseTheme = CaseTheme.CrystalClear,
                        ),
                        mixtapeSettings = MixtapeSettings(
                            handwritingMessiness = HandwritingMessiness.High,
                            handwritingFontSize = size.value,
                        ),
                    ),
                    onRequestPermission = {}, onRefresh = {}, onTogglePlayPause = {},
                    onPrevious = {}, onNext = {}, onSeekTo = {},
                )
            }
        }
        val failures = mutableListOf<String>()
        val pitchesBySize = mutableMapOf<HandwritingFontSize, MutableList<Float>>()
        val screenshots = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "font-normalization",
        ).apply { mkdirs() }
        for (selectedSize in HandwritingFontSize.entries) {
            for (selectedFont in MixtapeHandwritingFont.entries) {
                compose.runOnIdle { size.value = selectedSize; font.value = selectedFont }
                compose.waitForIdle()
                val top = compose.onNodeWithContentDescription("Track actions for ${first.title}")
                    .fetchSemanticsNode().boundsInRoot
                val bottom = compose.onNodeWithContentDescription("Track actions for ${second.title}")
                    .fetchSemanticsNode().boundsInRoot
                val pitch = bottom.top - top.top
                pitchesBySize.getOrPut(selectedSize) { mutableListOf() } += pitch
                if (selectedFont == MixtapeHandwritingFont.Kalam || selectedFont == MixtapeHandwritingFont.GloriaHallelujah) {
                    val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                    File(screenshots, "${selectedSize.name}-${selectedFont.name}.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
                if (abs(top.height - bottom.height) > 1f) failures += "$selectedSize $selectedFont: unequal row heights ${top.height}, ${bottom.height}"
            }
            val pitches = pitchesBySize.getValue(selectedSize)
            if (pitches.max() - pitches.min() > 2f) failures += "$selectedSize: row pitches across fonts $pitches"
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
