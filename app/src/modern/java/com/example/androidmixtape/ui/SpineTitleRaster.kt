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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.max

internal data class SpineTitleRaster(val image: ImageBitmap, val top: Int, val bottom: Int)

/** Measure transformed ink, not advance widths or family-specific baseline guesses.
 * Fitting runs center inside the title viewport, which already excludes the badge and margins.
 * Overflow keeps its original left-anchored clip. Only the resulting visible ink determines
 * vertical fitting. Re-render after shrinking because that can expose more characters.
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
        // Bounded scratch space retains negative bearings and rotated end glyphs. Its size
        // depends on the viewport and font height, never on the length of an overflowing title.
        val padding = ceil(max(em, rasterHeight / 4f) * 2f).toInt().coerceAtLeast(1)
        val scratchWidth = width + padding * 2
        val scratch = ImageBitmap(scratchWidth, rasterHeight)
        var complete = true
        CanvasDrawScope().draw(density, layoutDirection, Canvas(scratch), Size(scratchWidth.toFloat(), rasterHeight.toFloat())) {
            translate(left = padding.toFloat()) {
                scale(factor, factor, pivot = Offset.Zero) {
                    var cursor = 0f
                    for ((index, entry) in tokens.withIndex()) {
                        val (token, layout) = entry
                        if (cursor * factor > width + padding) { complete = false; break }
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
        }
        val scratchBitmap = scratch.asAndroidBitmap()
        val scratchRow = IntArray(scratchWidth)
        var left = scratchWidth
        var right = 0
        for (y in 0 until rasterHeight) {
            scratchBitmap.getPixels(scratchRow, 0, scratchWidth, 0, y, scratchWidth, 1)
            for (x in scratchRow.indices) {
                if (scratchRow[x] ushr 24 != 0) { left = minOf(left, x); right = maxOf(right, x + 1) }
            }
        }
        val fitting = complete && left > 0 && right < scratchWidth && right > left && right - left <= width - 2
        val sourceX = if (fitting) left - (width - (right - left)) / 2 else padding
        val image = ImageBitmap(width, rasterHeight)
        val bitmap = image.asAndroidBitmap()
        // Integer translation preserves the measured pixels, weight and jitter without a
        // second antialiasing pass. Overflow uses exactly the old viewport at cursor zero.
        android.graphics.Canvas(bitmap).drawBitmap(scratchBitmap, -sourceX.toFloat(), 0f, null)
        scratchBitmap.recycle()
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
