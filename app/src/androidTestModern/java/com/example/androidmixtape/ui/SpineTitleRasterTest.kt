package com.example.androidmixtape.ui

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidmixtape.viewmodel.MixtapeHandwritingFont
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpineTitleRasterTest {
    @get:Rule val compose = createComposeRule()

    @Test fun visibleInkFitsAllFontsSizesJitterAndLongTitles() {
        lateinit var measurer: TextMeasurer
        compose.setContent { measurer = rememberTextMeasurer() }
        compose.runOnIdle {
            val titles = listOf("restless neon weekend", "Agjpqy", "ÉÅgj", " ", "", "W", "🎵gj", "longunbrokentitle".repeat(6), "long spaced title ".repeat(8))
            var cases = 0
            for (font in MixtapeHandwritingFont.entries) {
                for (widthDp in listOf(180f, 240f, 280f, 360f, 480f)) {
                    for (densityValue in listOf(1f, 2.75f)) {
                        val density = Density(densityValue)
                        val scale = widthDp / 280f
                        val height = (44f * scale * densityValue).toInt()
                        val width = (206f * scale * densityValue).toInt()
                        for (title in titles) {
                            val style = TextStyle(fontFamily = font.cassetteHandwritingFontFamily(), fontSize = (font.cassetteSpineFontSize().value * scale).sp, fontWeight = font.effectiveCassetteWeight(FontWeight.ExtraBold), fontStyle = FontStyle.Italic)
                            val tokens = title.codePoints().toArray().map { String(Character.toChars(it)) }.map {
                                it to measurer.measure(AnnotatedString(it), style, density = density)
                            }
                            for (strength in listOf(0f, 1f, 3f)) {
                                val samples = handwritingJitterSamples(971, tokens.size, HandwritingJitterStrength(.054f * strength, .105f * strength, 3f * strength, .023f * strength))
                                val raster = rasterizeSpineTitle(tokens, samples, style.fontSize.value * densityValue, width, height, density, LayoutDirection.Ltr)
                                val label = "$font $widthDp $densityValue $strength $title"
                                if (title.isBlank()) assertNull(label, raster) else {
                                    assertNotNull(label, raster)
                                    raster!!
                                    val inkHeight = raster.bottom - raster.top
                                    assertTrue(label, inkHeight in 1..height - 2)
                                    val offset = (height - inkHeight) / 2
                                    assertTrue(label, offset >= 1 && offset + inkHeight < height)
                                    assertTrue(label, kotlin.math.abs(offset + inkHeight / 2f - height / 2f) <= .5f)
                                    val bitmap = raster.image.asAndroidBitmap()
                                    assertTrue(label, (0 until width).any { bitmap.getPixel(it, raster.top) ushr 24 != 0 })
                                    assertTrue(label, (0 until width).any { bitmap.getPixel(it, raster.bottom - 1) ushr 24 != 0 })
                                    bitmap.recycle()
                                }
                                cases++
                            }
                        }
                    }
                }
            }
            assertEquals(2160, cases)
            assertNull(rasterizeSpineTitle(emptyList(), emptyList(), 52f, 0, 0, Density(1f), LayoutDirection.Ltr))
        }
    }
}
