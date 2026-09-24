package com.example.androidmixtape.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.androidmixtape.playback.CounterWheelState
import com.example.androidmixtape.viewmodel.DeckTheme

/** One clock for all changed wheels; transport remains the only source of counter values. */
@Composable
internal fun CounterWheels(
    value: Int,
    playing: Boolean,
    revision: Long,
    color: Color,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true,
    deckTheme: DeckTheme = DeckTheme.SilverfaceHiFi,
) {
    var wheel by remember(deckTheme) { mutableStateOf(CounterWheelState.settled(value, revision)) }
    val platformMotionEnabled = platformCounterMotionEnabled()
    val next = wheel.update(value, playing, revision,
        motionEnabled && platformMotionEnabled && !deckTheme.usesDigitalCounter())
    if (next != wheel) wheel = next
    val progress = remember(wheel.generation) { Animatable(0f) }
    LaunchedEffect(wheel.generation) {
        if (wheel.rolling) {
            val generation = wheel.generation
            // Animatable uses Compose's platform MotionDurationScale, including scale zero.
            progress.animateTo(1f, tween(160, easing = LinearOutSlowInEasing))
            wheel = wheel.complete(generation)
        }
    }

    val measurer = rememberTextMeasurer()
    val style = LocalTextStyle.current.copy(
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        letterSpacing = 2.sp,
    )
    val target = wheel.target.toString().padStart(3, '0')
    val incoming = measurer.measure(target, style)
    val outgoing = measurer.measure(wheel.outgoing.toString().padStart(3, '0'), style)
    val density = LocalDensity.current
    Canvas(
        modifier = modifier
            .size(with(density) { incoming.size.width.toDp() }, with(density) { incoming.size.height.toDp() })
            .clipToBounds()
            .semantics { contentDescription = "Tape counter $target" },
    ) {
        val cellWidth = incoming.size.width / 3f
        if (deckTheme.usesDigitalCounter()) {
            val segments = arrayOf(
                intArrayOf(0, 1, 2, 4, 5, 6), intArrayOf(2, 5),
                intArrayOf(0, 2, 3, 4, 6), intArrayOf(0, 2, 3, 5, 6),
                intArrayOf(1, 2, 3, 5), intArrayOf(0, 1, 3, 5, 6),
                intArrayOf(0, 1, 3, 4, 5, 6), intArrayOf(0, 2, 5),
                intArrayOf(0, 1, 2, 3, 4, 5, 6), intArrayOf(0, 1, 2, 3, 5, 6),
            )
            val strokes = arrayOf(
                floatArrayOf(.24f, .14f, .76f, .14f),
                floatArrayOf(.19f, .19f, .19f, .48f),
                floatArrayOf(.81f, .19f, .81f, .48f),
                floatArrayOf(.24f, .50f, .76f, .50f),
                floatArrayOf(.19f, .52f, .19f, .81f),
                floatArrayOf(.81f, .52f, .81f, .81f),
                floatArrayOf(.24f, .86f, .76f, .86f),
            )
            target.forEachIndexed { index, digit ->
                strokes.forEachIndexed { segment, line ->
                    val lit = segment in segments[digit.digitToInt()]
                    drawLine(
                        color = color.copy(alpha = if (lit) 1f else .13f),
                        start = Offset((index + line[0]) * cellWidth, line[1] * size.height),
                        end = Offset((index + line[2]) * cellWidth, line[3] * size.height),
                        strokeWidth = size.height * .075f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            return@Canvas
        }
        // Draw the measured whole string through fixed per-digit clips. This preserves
        // the original Text's spacing, baseline and centering, including letter spacing.
        wheel.frame(progress.value).forEach { cell ->
            clipRect(left = cell.index * cellWidth, right = (cell.index + 1) * cellWidth) {
                cell.glyphs.forEach { glyph ->
                    val layout = if (glyph.digit == target[cell.index]) incoming else outgoing
                    // Compact decks can constrain the canvas below the inherited line height.
                    // Center the measured line inside that window, rather than clipping its bottom.
                    val centeredY = (size.height - layout.size.height) / 2f
                    drawText(layout, topLeft = Offset(0f, centeredY + glyph.offsetInCellHeights * size.height))
                }
            }
        }
    }
}

internal fun DeckTheme.usesDigitalCounter(): Boolean =
    this == DeckTheme.BlackoutPortable || this == DeckTheme.NavyMicro || this == DeckTheme.GraphiteSlim

/** Observe scale zero explicitly so disabling motion also cancels a wheel already in flight. */
@Composable
private fun platformCounterMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun readEnabled() = android.provider.Settings.Global.getFloat(
        resolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var enabled by remember(resolver) { mutableStateOf(readEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { enabled = readEnabled() }
        }
        resolver.registerContentObserver(android.provider.Settings.Global.getUriFor(
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        enabled = readEnabled()
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return enabled
}
