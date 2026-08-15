package com.example.androidmixtape.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.example.androidmixtape.viewmodel.HandwritingMessiness
import kotlin.math.max
import kotlin.math.min

val LocalHandwritingMessiness = compositionLocalOf { HandwritingMessiness.Low }

@Composable
fun JitteredHandwritingText(
    text: String,
    startIndex: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 14.sp,
    fontFamily: FontFamily? = null,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    lineHeightScale: Float = 1f,
    tokenization: HandwritingJitterTokenization = HandwritingJitterTokenization.Character,
    strength: HandwritingJitterStrength = HandwritingJitterStrength(
        maxDxEm = 0.05f,
        maxDyEm = 0.085f,
        maxRotationDegrees = 2.4f,
        maxTrackingEm = 0.018f,
    ),
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val messiness = LocalHandwritingMessiness.current
    val lineHeightDp = with(density) {
        max(fontSize.toPx(), 12.sp.toPx())
            .times(1.45f * lineHeightScale.coerceAtLeast(0.1f))
            .toDp()
    }
    val tokens = remember(text, tokenization) { text.jitterTokens(tokenization) }
    val effectiveStrength = remember(strength, messiness) {
        val multiplier = messiness.strengthMultiplier
        HandwritingJitterStrength(
            maxDxEm = strength.maxDxEm * multiplier,
            maxDyEm = strength.maxDyEm * multiplier,
            maxRotationDegrees = strength.maxRotationDegrees * multiplier,
            maxTrackingEm = strength.maxTrackingEm * multiplier,
        )
    }
    val samples = remember(startIndex, tokens.size, effectiveStrength, messiness) {
        if (messiness == HandwritingMessiness.Off) {
            emptyList()
        } else {
            handwritingJitterSamples(startIndex, tokens.size, effectiveStrength)
        }
    }
    // Keep geometry independent from the draw-time color so highlighting the
    // current row does not remeasure every character.
    val measuredTokens = remember(
        tokens,
        textMeasurer,
        fontSize,
        fontFamily,
        fontStyle,
        fontWeight,
        density.density,
        density.fontScale,
    ) {
        val measurementStyle = TextStyle(
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
        )
        tokens.map { token ->
            token to textMeasurer.measure(AnnotatedString(token), style = measurementStyle, maxLines = 1)
        }
    }
    val ellipsisLayout = remember(
        overflow,
        textMeasurer,
        fontSize,
        fontFamily,
        fontStyle,
        fontWeight,
        density.density,
        density.fontScale,
    ) {
        if (overflow == TextOverflow.Ellipsis) {
            textMeasurer.measure(
                AnnotatedString("…"),
                style = TextStyle(
                    fontSize = fontSize,
                    fontFamily = fontFamily,
                    fontStyle = fontStyle,
                    fontWeight = fontWeight,
                ),
                maxLines = 1,
            )
        } else {
            null
        }
    }

    Canvas(
        modifier = modifier
            .height(lineHeightDp)
            .semantics { this.text = AnnotatedString(text) },
    ) {
        val minimumDrawableWidth = 1f
        if (text.isEmpty() || maxLines <= 0 || size.width < minimumDrawableWidth) return@Canvas

        val emPx = max(fontSize.toPx(), 1f)
        var measuredWidth = 0f
        val visibleTokens = mutableListOf<Pair<String, androidx.compose.ui.text.TextLayoutResult>>()
        for ((token, layout) in measuredTokens) {
            val sample = samples.getOrNull(visibleTokens.size)
                ?.takeIf { handwritingTokenAcceptsPerturbation(token) }
            val tokenAdvance = layout.size.width + (sample?.trackingEm ?: 0f) * emPx
            val wouldOverflow = measuredWidth + tokenAdvance > size.width && visibleTokens.isNotEmpty()
            if (wouldOverflow && maxLines == 1) break
            visibleTokens += token to layout
            measuredWidth += tokenAdvance
            if (measuredWidth >= size.width && maxLines == 1) break
        }
        val shouldDrawEllipsis = overflow == TextOverflow.Ellipsis && visibleTokens.size < measuredTokens.size
        if (shouldDrawEllipsis && ellipsisLayout != null) {
            measuredWidth = min(measuredWidth + ellipsisLayout.size.width, size.width)
        }

        var cursorX = when (textAlign) {
            TextAlign.Center -> ((size.width - measuredWidth) / 2f).coerceAtLeast(0f)
            TextAlign.End, TextAlign.Right -> (size.width - measuredWidth).coerceAtLeast(0f)
            else -> 0f
        }
        val baseY = ((size.height - visibleTokens.maxOfOrNull { it.second.size.height }?.toFloat().orZero()) / 2f)
            .coerceAtLeast(0f)

        visibleTokens.forEachIndexed { index, (token, layout) ->
            val sample = samples.getOrNull(index)
                ?.takeIf { handwritingTokenAcceptsPerturbation(token) }
                ?: HandwritingJitterSample(0f, 0f, 0f, 0f)
            val dx = sample.dxEm * emPx
            val dy = sample.dyEm * emPx
            val topLeft = Offset(cursorX + dx, baseY + dy)
            rotate(
                degrees = sample.rotationDegrees,
                pivot = Offset(topLeft.x + layout.size.width / 2f, topLeft.y + layout.size.height / 2f),
            ) {
                drawText(layout, color = color, topLeft = topLeft)
            }
            cursorX += layout.size.width + sample.trackingEm * emPx
        }

        if (shouldDrawEllipsis && ellipsisLayout != null) {
            drawText(
                ellipsisLayout,
                color = color,
                topLeft = Offset((size.width - ellipsisLayout.size.width).coerceAtLeast(0f), baseY),
            )
        }
    }
}

private fun String.jitterTokens(tokenization: HandwritingJitterTokenization): List<String> = when (tokenization) {
    HandwritingJitterTokenization.Character -> codePointTokens()
    HandwritingJitterTokenization.Word -> Regex("\\S+|\\s+").findAll(this).map { it.value }.toList()
}

private fun String.codePointTokens(): List<String> = buildList {
    var index = 0
    while (index < length) {
        val codePoint = Character.codePointAt(this@codePointTokens, index)
        val nextIndex = index + Character.charCount(codePoint)
        add(substring(index, nextIndex))
        index = nextIndex
    }
}

private fun Float?.orZero(): Float = this ?: 0f
