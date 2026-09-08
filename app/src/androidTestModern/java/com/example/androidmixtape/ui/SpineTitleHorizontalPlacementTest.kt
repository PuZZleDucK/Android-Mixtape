package com.example.androidmixtape.ui

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidmixtape.viewmodel.MixtapeHandwritingFont
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Card 4833: deliberately red until fitting ink is centered in the title-only viewport. */
@RunWith(AndroidJUnit4::class)
class SpineTitleHorizontalPlacementTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fittingInkCentersWithinReservedTitleViewport() {
        lateinit var measurer: TextMeasurer
        compose.setContent { measurer = rememberTextMeasurer() }
        compose.runOnIdle {
            val failures = mutableListOf<String>()
            for (font in MixtapeHandwritingFont.entries) {
                for (widthDp in listOf(180f, 280f, 480f)) {
                    val scale = widthDp / 280f
                    val width = (206f * scale).toInt()
                    val height = (44f * scale).toInt()
                    val style = TextStyle(
                        fontFamily = font.cassetteHandwritingFontFamily(),
                        fontSize = (font.cassetteSpineFontSize().value * scale).sp,
                        fontWeight = font.effectiveCassetteWeight(FontWeight.ExtraBold),
                        fontStyle = FontStyle.Italic,
                    )
                    for (title in listOf("W", "gj")) {
                        val tokens = title.map { it.toString() }.map {
                            it to measurer.measure(AnnotatedString(it), style, density = Density(1f))
                        }
                        for (seed in listOf(971, 42)) {
                            val samples = handwritingJitterSamples(seed, tokens.size,
                                HandwritingJitterStrength(.054f, .105f, 3f, .023f))
                            val raster = checkNotNull(rasterizeSpineTitle(tokens, samples,
                                style.fontSize.value, width, height, Density(1f), LayoutDirection.Ltr))
                            val bitmap = raster.image.asAndroidBitmap()
                            var left = width
                            var right = 0
                            for (y in raster.top until raster.bottom) {
                                for (x in 0 until width) {
                                    if (bitmap.getPixel(x, y) ushr 24 != 0) {
                                        left = minOf(left, x)
                                        right = maxOf(right, x + 1)
                                    }
                                }
                            }
                            val label = "$font width=$widthDp title=$title seed=$seed ink=[$left,$right) viewport=$width"
                            if (left < 1 || right >= width || kotlin.math.abs((left + right) / 2f - width / 2f) > .5f) {
                                failures += label
                            }
                            bitmap.recycle()
                        }
                    }
                }
            }
            assertTrue("${failures.size}/96 placement cases failed:\n${failures.joinToString("\n")}", failures.isEmpty())
        }
    }
}
