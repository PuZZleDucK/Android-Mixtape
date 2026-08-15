package com.example.androidmixtape.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.example.androidmixtape.viewmodel.MixtapeEmbellishment
import com.example.androidmixtape.viewmodel.MixtapeSpineSkin
import com.example.androidmixtape.viewmodel.MixtapeSymbolColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Produces small, binder-safe cassette-spine images for the car template.
 *
 * Android Auto owns the grid layout, but the artwork mirrors the phone's
 * spine-out cassette briefcase: dark case, brass rails, coloured end caps,
 * ruled paper labels, handwriting-like titles, and a hand-drawn symbol.
 */
object MixtapeCarArtworkRenderer {
    private const val WIDTH = 256
    private const val HEIGHT = 128

    fun icon(mixtape: CarMixtape, sideNumber: Int): CarIcon {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.RGB_565)
        drawCassetteSpine(Canvas(bitmap), mixtape, sideNumber)
        return CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
    }

    private fun drawCassetteSpine(canvas: Canvas, mixtape: CarMixtape, sideNumber: Int) {
        val palette = palette(mixtape.visualProperties.spineSkin)
        val casePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(Color.rgb(38, 42, 51))
        casePaint.color = Color.rgb(75, 53, 40)
        canvas.drawRoundRect(RectF(4f, 5f, 252f, 21f), 9f, 9f, casePaint)
        canvas.drawRoundRect(RectF(4f, 107f, 252f, 123f), 9f, 9f, casePaint)
        casePaint.color = Color.rgb(167, 122, 53)
        canvas.drawRoundRect(RectF(18f, 12f, 238f, 14f), 1f, 1f, casePaint)

        val spine = RectF(10f, 27f, 246f, 103f)
        casePaint.color = palette.shell
        canvas.drawRoundRect(spine, 10f, 10f, casePaint)
        casePaint.style = Paint.Style.STROKE
        casePaint.strokeWidth = 3f
        casePaint.color = palette.border
        canvas.drawRoundRect(RectF(12f, 29f, 244f, 101f), 8f, 8f, casePaint)
        casePaint.style = Paint.Style.FILL

        casePaint.color = palette.paper
        canvas.drawRoundRect(RectF(29f, 34f, 227f, 96f), 5f, 5f, casePaint)
        casePaint.color = palette.label
        canvas.drawRoundRect(RectF(43f, 42f, 214f, 88f), 5f, 5f, casePaint)
        casePaint.color = palette.strip
        canvas.drawRoundRect(RectF(34f, 42f, 40f, 88f), 3f, 3f, casePaint)

        casePaint.color = palette.rule
        casePaint.strokeWidth = 1f
        canvas.drawLine(49f, 56f, 207f, 56f, casePaint)
        canvas.drawLine(49f, 75f, 207f, 75f, casePaint)

        casePaint.color = palette.endCap
        canvas.drawRoundRect(RectF(10f, 27f, 31f, 103f), 8f, 8f, casePaint)
        canvas.drawRoundRect(RectF(225f, 27f, 246f, 103f), 8f, 8f, casePaint)
        casePaint.color = palette.border
        canvas.drawCircle(20f, 44f, 3.5f, casePaint)
        canvas.drawCircle(236f, 86f, 3.5f, casePaint)
        casePaint.color = palette.label
        canvas.drawCircle(20f, 44f, 1.2f, casePaint)
        canvas.drawCircle(236f, 86f, 1.2f, casePaint)

        drawEmbellishment(
            canvas = canvas,
            embellishment = mixtape.visualProperties.embellishment,
            color = symbolColor(mixtape.visualProperties.symbolColor),
            centerX = 57f,
            centerY = 65f,
        )

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = symbolColor(mixtape.visualProperties.nameColor)
            textSize = 19f
            typeface = Typeface.create("cursive", Typeface.BOLD_ITALIC)
        }
        val title = ellipsize(mixtape.displayName, titlePaint, maxWidth = 132f)
        canvas.drawText(title, 72f, 72f, titlePaint)

        val sidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.border
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("S$sideNumber", 235f, 68f, sidePaint)
    }

    private fun drawEmbellishment(
        canvas: Canvas,
        embellishment: MixtapeEmbellishment,
        color: Int,
        centerX: Float,
        centerY: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        when (embellishment) {
            MixtapeEmbellishment.Heart -> {
                val path = Path().apply {
                    moveTo(centerX, centerY + 9f)
                    cubicTo(centerX - 18f, centerY - 2f, centerX - 8f, centerY - 16f, centerX, centerY - 6f)
                    cubicTo(centerX + 8f, centerY - 16f, centerX + 18f, centerY - 2f, centerX, centerY + 9f)
                }
                canvas.drawPath(path, paint)
            }
            MixtapeEmbellishment.LightningBolt -> {
                val path = Path().apply {
                    moveTo(centerX + 3f, centerY - 15f)
                    lineTo(centerX - 8f, centerY + 1f)
                    lineTo(centerX, centerY + 1f)
                    lineTo(centerX - 3f, centerY + 15f)
                    lineTo(centerX + 10f, centerY - 4f)
                    lineTo(centerX + 2f, centerY - 4f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            MixtapeEmbellishment.Smiley -> {
                canvas.drawCircle(centerX, centerY, 13f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(centerX - 5f, centerY - 3f, 1.7f, paint)
                canvas.drawCircle(centerX + 5f, centerY - 3f, 1.7f, paint)
                paint.style = Paint.Style.STROKE
                canvas.drawArc(RectF(centerX - 7f, centerY - 2f, centerX + 7f, centerY + 9f), 15f, 150f, false, paint)
            }
            MixtapeEmbellishment.MusicNote,
            MixtapeEmbellishment.QuarterNote,
            MixtapeEmbellishment.EighthNote,
            MixtapeEmbellishment.BeamedEighthNotes,
            MixtapeEmbellishment.SixteenthNote,
            MixtapeEmbellishment.WholeNote,
            MixtapeEmbellishment.HalfNote,
            -> {
                canvas.drawCircle(centerX - 5f, centerY + 8f, 4f, paint)
                canvas.drawLine(centerX - 1f, centerY + 7f, centerX - 1f, centerY - 13f, paint)
                canvas.drawLine(centerX - 1f, centerY - 13f, centerX + 10f, centerY - 9f, paint)
            }
            else -> drawStar(canvas, centerX, centerY, paint)
        }
    }

    private fun drawStar(canvas: Canvas, centerX: Float, centerY: Float, paint: Paint) {
        val path = Path()
        repeat(10) { point ->
            val radius = if (point % 2 == 0) 14f else 5.5f
            val angle = -PI / 2 + point * PI / 5
            val x = centerX + cos(angle).toFloat() * radius
            val y = centerY + sin(angle).toFloat() * radius
            if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val suffix = "…"
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + suffix) > maxWidth) {
            end -= 1
        }
        return text.substring(0, end).trimEnd() + suffix
    }

    private fun symbolColor(color: MixtapeSymbolColor): Int = when (color) {
        MixtapeSymbolColor.Navy -> Color.rgb(30, 55, 102)
        MixtapeSymbolColor.Red -> Color.rgb(176, 52, 67)
        MixtapeSymbolColor.Green -> Color.rgb(34, 111, 82)
        MixtapeSymbolColor.Purple -> Color.rgb(104, 63, 143)
        MixtapeSymbolColor.Amber -> Color.rgb(172, 106, 24)
    }

    private fun palette(skin: MixtapeSpineSkin): SpinePalette = PALETTES[skin.ordinal % PALETTES.size]

    private data class SpinePalette(
        val shell: Int,
        val paper: Int,
        val label: Int,
        val strip: Int,
        val border: Int,
        val rule: Int,
        val endCap: Int,
    )

    private val PALETTES = listOf(
        SpinePalette(0xFFF3AEC8.toInt(), 0xFFFFD8E7.toInt(), 0xFFFFF0F5.toInt(), 0xFFE9468A.toInt(), 0xFFBA577B.toInt(), 0xFFDCA5B8.toInt(), 0xFFF6B5CF.toInt()),
        SpinePalette(0xFFA9D8F2.toInt(), 0xFFDDF3FF.toInt(), 0xFFF7FCFF.toInt(), 0xFF2E81B8.toInt(), 0xFF3F7192.toInt(), 0xFF9BC6DE.toInt(), 0xFFB8E2F7.toInt()),
        SpinePalette(0xFFA7E1C1.toInt(), 0xFFDDF7E8.toInt(), 0xFFF5FFF9.toInt(), 0xFF2E9D72.toInt(), 0xFF39785F.toInt(), 0xFF9BCBB1.toInt(), 0xFFB9E8CF.toInt()),
        SpinePalette(0xFFF4D47A.toInt(), 0xFFFFF0B8.toInt(), 0xFFFFFAE7.toInt(), 0xFFDB7B2B.toInt(), 0xFF8D682D.toInt(), 0xFFE0C36E.toInt(), 0xFFF8DE90.toInt()),
        SpinePalette(0xFFC9B5EB.toInt(), 0xFFECE2FF.toInt(), 0xFFFAF7FF.toInt(), 0xFF7752B5.toInt(), 0xFF705B98.toInt(), 0xFFC1AFE0.toInt(), 0xFFD4C3F2.toInt()),
        SpinePalette(0xFFFFB29B.toInt(), 0xFFFFDCD1.toInt(), 0xFFFFF5F0.toInt(), 0xFFD84C3E.toInt(), 0xFF9C574B.toInt(), 0xFFE2A395.toInt(), 0xFFFFC1AE.toInt()),
    )
}
