package com.example.androidmixtape.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.example.androidmixtape.viewmodel.MixtapeHandwritingFont
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SpineTitleOverhangTest {
    @get:Rule val compose = createComposeRule()

    @Test fun paddedReferencePreservesInkAndOverflowAtFitThreshold() {
        lateinit var measurer: TextMeasurer
        compose.setContent { measurer = rememberTextMeasurer() }
        compose.runOnIdle {
            var cases = 0
            var negativeBearingCases = 0
            for (font in MixtapeHandwritingFont.entries) {
                for (fontScale in listOf(1f, 1.3f)) {
                    val density = Density(1f, fontScale)
                    val style = TextStyle(fontFamily = font.cassetteHandwritingFontFamily(),
                        fontSize = (font.cassetteSpineFontSize().value / 2f).sp,
                        fontWeight = font.effectiveCassetteWeight(FontWeight.ExtraBold), fontStyle = FontStyle.Italic)
                    val em = style.fontSize.value * fontScale
                    for (title in listOf("W", "Agjpqy", "Night Drive", "ÉÅgj", "restless neon weekend",
                        "long spaced title ".repeat(8), "Wgj".repeat(60), " ", "")) {
                        val tokens = title.map { it.toString() }.map {
                            it to measurer.measure(AnnotatedString(it), style, density = density)
                        }
                        for (seed in listOf(971, 42)) {
                            val samples = handwritingJitterSamples(seed, tokens.size,
                                HandwritingJitterStrength(.162f, .315f, 9f, .069f)).mapIndexed { index, sample ->
                                // Exercise negative bearing explicitly, independent of device fonts.
                                if (seed == 42 && index == 0) sample.copy(dxEm = -1f) else sample
                            }
                            // Independent roomy reference. No calls to the production bounds or placement code.
                            val reference = ImageBitmap(4096, 256)
                            CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(reference), Size(4096f, 256f)) {
                                translate(left = 256f) {
                                    var cursor = 0f
                                    tokens.forEachIndexed { index, (token, layout) ->
                                        val sample = samples[index].takeIf { handwritingTokenAcceptsPerturbation(token) }
                                            ?: HandwritingJitterSample(0f, 0f, 0f, 0f)
                                        val origin = Offset(cursor + sample.dxEm * em, em + sample.dyEm * em)
                                        rotate(sample.rotationDegrees, Offset(origin.x + layout.size.width / 2f, origin.y + layout.size.height / 2f)) {
                                            drawText(layout, Color.White, topLeft = origin)
                                        }
                                        cursor += layout.size.width + sample.trackingEm * em
                                    }
                                }
                            }
                            val bitmap = reference.asAndroidBitmap()
                            val pixels = IntArray(4096 * 256)
                            bitmap.getPixels(pixels, 0, 4096, 0, 0, 4096, 256)
                            var left = 4096
                            var right = 0
                            pixels.forEachIndexed { index, pixel ->
                                if (pixel ushr 24 != 0) { left = minOf(left, index % 4096); right = maxOf(right, index % 4096 + 1) }
                            }
                            val inkWidth = right - left
                            if (left < 256) negativeBearingCases++
                            val widths = if (inkWidth > 0 && inkWidth < 600) {
                                listOf(132, 206, 353, inkWidth + 2, inkWidth + 1, (inkWidth - 1).coerceAtLeast(1)).distinct()
                            } else listOf(132, 206, 353)
                            for (width in widths) {
                                val label = "$font fontScale=$fontScale title=$title seed=$seed width=$width"
                                val raster = rasterizeSpineTitle(tokens, samples, em, width, 200, density, LayoutDirection.Ltr)
                                if (inkWidth <= 0) {
                                    assertNull(label, raster)
                                } else {
                                    val sourceX = if (inkWidth <= width - 2) left - (width - inkWidth) / 2 else 256
                                    val visibleInk = pixels.indices.any { index ->
                                        index % 4096 in sourceX until sourceX + width && pixels[index] ushr 24 != 0
                                    }
                                    if (!visibleInk) {
                                        assertNull("$label has no ink inside its clipped viewport", raster)
                                        cases++
                                        continue
                                    }
                                    val actual = checkNotNull(raster) { label }.image.asAndroidBitmap()
                                    // All pixels match, so overhang, weight, jitter, blank margins and the
                                    // original size survive. Just-overflow runs remain left anchored.
                                    val actualPixels = IntArray(width * actual.height)
                                    actual.getPixels(actualPixels, 0, width, 0, 0, width, actual.height)
                                    var mismatch = -1
                                    for (index in actualPixels.indices) {
                                        val x = index % width + sourceX
                                        val y = index / width
                                        val expectedAlpha = if (x in 0 until 4096 && y < 256) pixels[y * 4096 + x] ushr 24 else 0
                                        if (actualPixels[index] ushr 24 != expectedAlpha) { mismatch = index; break }
                                    }
                                    assertEquals("$label first changed pixel", -1, mismatch)
                                    actual.recycle()
                                }
                                cases++
                            }
                            bitmap.recycle()
                        }
                    }
                }
            }
            assertTrue("Reference must exercise negative overhang", negativeBearingCases > 0)
            android.util.Log.i("SpineOverhang", "passed $cases cases; $negativeBearingCases negative-bearing references")
        }
    }
}
