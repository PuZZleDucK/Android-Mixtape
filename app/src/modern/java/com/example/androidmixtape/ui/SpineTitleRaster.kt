package com.example.androidmixtape.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.max

internal data class SpineTitleRaster(val image: ImageBitmap, val top: Int, val bottom: Int)

/** Raster bounds include font fallback, italic overhang and the actual jitter transform.
 * Only horizontally visible ink participates. Re-render after shrinking because that
 * can expose more characters. Color is applied later, without measuring again.
 */
internal fun rasterizeSpineTitle(
    tokens: List<Pair<String, TextLayoutResult>>,
    samples: List<HandwritingJitterSample>,
    em: Float,
    width: Int,
    height: Int,
    density: Density,
    layoutDirection: LayoutDirection,
): SpineTitleRaster? {
    if (width <= 0 || height <= 2 || tokens.isEmpty()) return null
    val rasterHeight = ceil(max(tokens.maxOf { it.second.size.height }.toFloat(), em) * 4f).toInt().coerceAtLeast(1)
    var factor = 1f
    repeat(16) {
        val image = ImageBitmap(width, rasterHeight)
        CanvasDrawScope().draw(density, layoutDirection, Canvas(image), Size(width.toFloat(), rasterHeight.toFloat())) {
            scale(factor, factor, pivot = Offset.Zero) {
                var cursor = 0f
                for ((index, entry) in tokens.withIndex()) {
                    val (token, layout) = entry
                    if (cursor * factor > width + em * factor) break
                    val sample = samples.getOrNull(index)?.takeIf { handwritingTokenAcceptsPerturbation(token) }
                        ?: HandwritingJitterSample(0f, 0f, 0f, 0f)
                    val origin = Offset(cursor + sample.dxEm * em, em + sample.dyEm * em)
                    rotate(sample.rotationDegrees, Offset(origin.x + layout.size.width / 2f, origin.y + layout.size.height / 2f)) {
                        drawText(layout, Color.White, topLeft = origin)
                    }
                    cursor += layout.size.width + sample.trackingEm * em
                }
            }
        }
        val bitmap = image.asAndroidBitmap()
        val row = IntArray(width)
        var top = rasterHeight
        var bottom = 0
        for (y in 0 until rasterHeight) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            if (row.any { (it ushr 24) != 0 }) {
                top = minOf(top, y)
                bottom = y + 1
            }
        }
        if (bottom <= top) { bitmap.recycle(); return null }
        if (bottom - top <= height - 2) return SpineTitleRaster(image, top, bottom)
        factor *= (height - 2f) / (bottom - top) * 0.98f
        bitmap.recycle()
    }
    return null
}
