package com.example.androidmixtape.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText

/** Ink bounds relative to drawText's origin, independent of a font's line padding. */
data class HandwritingInkBounds(val top: Float, val bottom: Float) {
    val centerY: Float get() = (top + bottom) / 2f
    // Reserve room for the track rows' vertical jitter and character rotation.
    fun paddedHeight(emPx: Float): Float = bottom - top + emPx * 0.24f
}

/** Measure once per row style, not per song or frame. A shared alphabet keeps the
 * baseline and row height stable as selection, track names and jitter change.
 */
@Composable
internal fun rememberHandwritingInkBounds(textMeasurer: TextMeasurer, style: TextStyle): HandwritingInkBounds {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    return remember(textMeasurer, style, density.density, density.fontScale, direction) {
        val layout = textMeasurer.measure(
            text = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
            style = style,
            maxLines = 1,
        )
        val image = ImageBitmap(layout.size.width, layout.size.height)
        CanvasDrawScope().draw(density, direction, Canvas(image), Size(layout.size.width.toFloat(), layout.size.height.toFloat())) {
            drawText(layout, Color.White)
        }
        val bitmap = image.asAndroidBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val first = pixels.indexOfFirst { it ushr 24 != 0 }
        val last = pixels.indexOfLast { it ushr 24 != 0 }
        val bounds = if (first < 0) {
            HandwritingInkBounds(0f, layout.size.height.toFloat())
        } else {
            HandwritingInkBounds((first / bitmap.width).toFloat(), (last / bitmap.width + 1).toFloat())
        }
        bitmap.recycle()
        bounds
    }
}
