package com.example.androidmixtape.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.androidmixtape.viewmodel.CaseTheme

internal data class CasePlasticPalette(
    val tint: Color,
    val haze: Color,
    val edge: Color,
    val hinge: Color,
)

// Resolve sheet opacity here, once, for every case and spine renderer.
internal fun CaseTheme.plasticPalette(): CasePlasticPalette {
    val palette = when (this) {
        CaseTheme.CrystalClear -> CasePlasticPalette(Color(0x1AEBF0FF), Color.Transparent, Color(0xA6FFFFFF), Color(0x4714141E))
        CaseTheme.CloudyClear -> CasePlasticPalette(Color(0x1FE0E8EE), Color(0x29F4F8FA), Color(0xB8FFFFFF), Color(0x4D2D343C))
        CaseTheme.SmokeTint -> CasePlasticPalette(Color(0x7312141A), Color.Transparent, Color(0x4DFFFFFF), Color(0x80000000))
        CaseTheme.AmberTint -> CasePlasticPalette(Color(0x42FF9628), Color.Transparent, Color(0x8CFFDCA0), Color(0x66783C00))
        CaseTheme.RubyClear -> CasePlasticPalette(Color(0x52EB1C2A), Color.Transparent, Color(0xC2FF808A), Color(0x75120812))
        CaseTheme.HotPinkClear -> CasePlasticPalette(Color(0x4DFF2296), Color.Transparent, Color(0xC7FF8ECC), Color(0x7019084E))
        CaseTheme.ElectricBlueClear -> CasePlasticPalette(Color(0x4A187EFF), Color.Transparent, Color(0xC787C4FF), Color(0x75053482))
        CaseTheme.AcidGreenClear -> CasePlasticPalette(Color(0x4A67F523), Color.Transparent, Color(0xC7BCFF8B), Color(0x70387805))
        CaseTheme.VioletClear -> CasePlasticPalette(Color(0x4A8943FF), Color.Transparent, Color(0xC7CA9EFF), Color(0x73411287))
        else -> CasePlasticPalette(Color(0x33ECF5F3), Color.Transparent, Color(0xA6FFFFFF), Color(0x4714141E))
    }
    return palette.copy(
        tint = palette.tint.copy(alpha = palette.tint.alpha * 0.62f),
        haze = palette.haze.copy(alpha = palette.haze.alpha * 0.62f),
    )
}

/** Draw after paper and lettering. Never give the whole content group an alpha. */
internal fun DrawScope.drawCasePlastic(
    plastic: CasePlasticPalette,
    spine: Boolean = false,
    displayScale: Float = 1f,
) {
    val short = size.minDimension
    if (!short.isFinite() || short <= 0f || !displayScale.isFinite() || displayScale <= 0f) return
    // Width is specified in displayed pixels, including the scaled current-track preview.
    val rim = (short * 0.009f).coerceIn(0.5.dp.toPx() / displayScale, 1.dp.toPx() / displayScale)
        .coerceAtMost(short / 8f)
    val radius = CornerRadius((short * 0.06f).coerceAtMost(8.dp.toPx() / displayScale))
    drawRoundRect(plastic.tint, cornerRadius = radius)
    drawRoundRect(plastic.haze, cornerRadius = radius)
    val inset = rim / 2f
    drawRoundRect(plastic.edge, Offset(inset, inset), Size(size.width - rim, size.height - rim), radius, style = Stroke(rim))
    val seam = rim * 2.5f
    drawRoundRect(
        plastic.hinge.copy(alpha = plastic.hinge.alpha * 0.55f),
        Offset(seam, seam), Size(size.width - seam * 2f, size.height - seam * 2f),
        radius, style = Stroke(rim * 0.55f),
    )
    drawLine(Color.White.copy(alpha = 0.34f), Offset(seam * 2, seam), Offset(size.width - seam * 2, seam), rim * 0.6f)
    drawLine(plastic.edge.copy(alpha = plastic.edge.alpha * 0.55f), Offset(seam * 2, size.height - seam), Offset(size.width - seam * 2, size.height - seam), rim * 0.6f)
    if (short * displayScale < 24.dp.toPx()) return
    if (spine) {
        // Small molded end seams leave the title area unobstructed.
        for (x in listOf(size.width * 0.025f, size.width * 0.975f)) {
            drawLine(plastic.hinge.copy(alpha = 0.22f), Offset(x, short * 0.18f), Offset(x, size.height - short * 0.18f), rim * 0.7f)
        }
    } else {
        for (y in listOf(size.height * 0.18f, size.height * 0.82f)) {
            drawRoundRect(plastic.hinge.copy(alpha = 0.18f), Offset(seam, y), Size(rim * 2, short * 0.07f), CornerRadius(rim), style = Stroke(rim * 0.6f))
        }
        drawLine(plastic.hinge.copy(alpha = 0.24f), Offset(size.width - seam, size.height * 0.45f), Offset(size.width - seam, size.height * 0.55f), rim)
        val reflection = Path().apply {
            moveTo(size.width * 0.65f, seam * 2)
            lineTo(size.width * 0.72f, seam * 2)
            lineTo(size.width * 0.40f, size.height - seam * 2)
            lineTo(size.width * 0.36f, size.height - seam * 2)
            close()
        }
        drawPath(reflection, Color.White.copy(alpha = 0.045f))
    }
}
