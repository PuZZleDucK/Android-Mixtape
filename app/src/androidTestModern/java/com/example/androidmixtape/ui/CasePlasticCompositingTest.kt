package com.example.androidmixtape.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidmixtape.viewmodel.CaseTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class CasePlasticCompositingTest {
    private fun render(theme: CaseTheme, spine: Boolean, parent: Color, density: Float, width: Int = 600, height: Int = 200): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val palette = theme.plasticPalette()
        CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(width.toFloat(), height.toFloat())) {
            drawRect(parent)
            drawRect(Color(0xFFE8D8B0), Offset(size.width * 0.1f, size.height * 0.1f), Size(size.width * 0.4f, size.height * 0.8f))
            drawRect(Color(0xFF263040), Offset(size.width * 0.5f, size.height * 0.1f), Size(size.width * 0.4f, size.height * 0.8f))
            drawCasePlastic(palette, spine = spine)
        }
        return bitmap
    }

    private fun near(expected: Int, actual: Int) {
        for (shift in listOf(0, 8, 16, 24)) {
            assertTrue("expected ${expected.toUInt().toString(16)}, actual ${actual.toUInt().toString(16)}", abs((expected shr shift and 255) - (actual shr shift and 255)) <= 2)
        }
    }

    @Test fun everyThemeTransmitsPaperAndParentWithIdenticalCaseAndSpineOptics() {
        for (theme in CaseTheme.entries) {
            val palette = theme.plasticPalette()
            val transmission = (1f - palette.tint.alpha) * (1f - palette.haze.alpha)
            assertTrue("$theme transmission $transmission", transmission >= 0.65f)
            for (density in listOf(1f, 2f, 3f)) {
                for (parent in listOf(Color.White, Color(0xFF101820))) {
                    for (spine in listOf(false, true)) {
                        val bitmap = render(theme, spine, parent, density)
                        // All samples avoid the rim, seam and diagonal reflection.
                        for ((x, paper) in listOf(150 to Color(0xFFE8D8B0), 480 to Color(0xFF263040), 30 to parent)) {
                            near(palette.haze.compositeOver(palette.tint.compositeOver(paper)).toArgb(), bitmap.getPixel(x, 100))
                        }
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    @Test fun moldedRimsRemainVisibleOnLightAndDarkParents() {
        for (theme in CaseTheme.entries) for (parent in listOf(Color.White, Color(0xFF101820))) {
            val bitmap = render(theme, false, parent, 2f)
            val sheet = bitmap.getPixel(30, 100)
            assertTrue("$theme has no visible rim on $parent", (0..8).any { bitmap.getPixel(300, it) != sheet })
            bitmap.recycle()
        }
    }

    @Test fun tinyTallAndWideGeometryDoesNotThrow() {
        for ((width, height) in listOf(1 to 1, 2 to 20, 20 to 2, 200 to 600, 600 to 200)) {
            for (density in listOf(1f, 2f, 3f)) for (spine in listOf(false, true)) {
                render(CaseTheme.CloudyClear, spine, Color.White, density, width, height).recycle()
            }
        }
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()), Size.Zero) {
            drawCasePlastic(CaseTheme.CrystalClear.plasticPalette())
        }
    }
}
