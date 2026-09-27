package com.example.androidmixtape.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.example.androidmixtape.viewmodel.*
import org.json.JSONObject
import kotlin.math.sqrt

/** Artist tokens, not a web renderer. All geometry and interaction live in native Compose. */
internal class DemoStyle(private val json: JSONObject) {
    private val colors = mutableMapOf<String, Color>()
    fun text(key: String, fallback: String = "") = json.optString(key, fallback)
    fun number(key: String, fallback: Float = 0f) = json.optDouble(key, fallback.toDouble()).toFloat()
    fun flag(key: String, fallback: Boolean = false) = json.optBoolean(key, fallback)
    fun has(key: String) = json.has(key)
    fun color(key: String, fallback: Color = Color.Transparent): Color =
        if (!has(key)) fallback else colors.getOrPut(key) { runCatching { Color(android.graphics.Color.parseColor(text(key))) }.getOrDefault(fallback) }
    val name get() = text("name")
}

internal class DemoThemeCatalog(context: Context) {
    private val root = JSONObject(context.assets.open("demo-themes.json").bufferedReader().use { it.readText() })
    private val styles = buildMap<String, DemoStyle> {
        val categories = root.getJSONObject("catalog")
        categories.keys().forEach { category ->
            val values = categories.getJSONArray(category)
            for (i in 0 until values.length()) {
                val value = values.getJSONObject(i)
                put(value.getString("id"), DemoStyle(value))
            }
        }
    }
    private fun get(category: String, value: Enum<*>) = styles.getValue(
        root.getJSONObject("bindings").getJSONObject(category).getString(value.name))
    fun deck(value: DeckTheme) = get("decks", value)
    fun tape(value: CassetteTheme) = get("tapes", value)
    fun sticker(value: StickerTheme) = get("labels", value)
    fun case(value: CaseTheme) = get("cases", value)
    // Both sleeve faces deliberately resolve the SAME theme, not independently random skins.
    fun sleeve(value: SleeveTheme) = get("sleeves", value)
}

private var sharedDemoCatalog: DemoThemeCatalog? = null

@Composable
internal fun rememberDemoThemes(): DemoThemeCatalog {
    val context = LocalContext.current.applicationContext
    return remember(context) { synchronized(DemoThemeCatalog::class.java) { sharedDemoCatalog ?: DemoThemeCatalog(context).also { sharedDemoCatalog = it } } }
}

internal fun demoReadableInk(ink: Color, paper: Color): Color {
    val background = paper.luminance()
    val target = if (background < .179f) Color.White else Color.Black
    for (step in 0..20) {
        val result = lerp(ink, target, step / 20f)
        val foreground = result.luminance()
        if ((maxOf(background, foreground) + .05f) / (minOf(background, foreground) + .05f) >= 4.5f) return result
    }
    return target
}

/** Area-conserving windings reach the bare hub at either endpoint. */
internal fun demoWindingRadii(progress: Float): Pair<Float, Float> {
    val p = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
    val area = 79f * 79f - 30f * 30f
    return sqrt(900f + area * (1f - p)) to sqrt(900f + area * p)
}

internal const val DEMO_SPINE_ASPECT_RATIO = 6.5f

internal data class DemoCounterGeometry(val bezel: Rect, val display: Rect, val labelY: Float, val wheelScale: Float)

/** Grow the readable digits, not empty framing; all counters clear the well and transport. */
internal fun demoCounterGeometry(geometry: DemoDeckGeometry, digital: Boolean): DemoCounterGeometry {
    val centerX = if (geometry.android) 490f else if (geometry.right) 473f else 398f
    val top = if (geometry.android) 272f else if (geometry.right) 211f else 367f
    val width = if (digital) 76f else 62f
    val height = if (digital) 36f else 34f
    val bezel = Rect(centerX-width/2, top, centerX+width/2, top+height)
    val display = Rect(bezel.left+5f, top+4f, bezel.right-5f, bezel.bottom-4f)
    return DemoCounterGeometry(bezel, display, bezel.bottom+10f, 1.3f)
}

internal data class DemoDeckGeometry(val height: Float, val bay: Rect, val tape: Rect, val transport: Rect, val right: Boolean, val android: Boolean) {
    val aspectRatio get() = 560f / height
}

internal fun demoDeckGeometry(theme: DeckTheme): DemoDeckGeometry = when (theme) {
    DeckTheme.GraphiteTall, DeckTheme.ParkLife, DeckTheme.ChalkEdition -> DemoDeckGeometry(
        512f, Rect(17f, 17f, 543f, 347f), Rect(17f, 15f, 543f, 349.39f), Rect(17f, 430f, 543f, 495f), false, false)
    DeckTheme.StudioSilver -> DemoDeckGeometry(
        347.34f, Rect(17f, 17f, 407f, 262f), Rect(17f, 15f, 407f, 262.93f), Rect(17f, 270f, 543f, 333f), true, false)
    else -> DemoDeckGeometry(
        422f, Rect(22.4f, 30f, 448f, 308.77f), Rect(33.04f, 41.01f, 437.36f, 298.04f), Rect(30f, 326.8f, 530f, 388.5f), true, true)
}

internal fun demoCaseTransmission(tint: Color, opacity: Float): Color {
    val density = (opacity * 1.8f).coerceIn(0f, .94f)
    return Color(1f - density * (1f - tint.red), 1f - density * (1f - tint.green), 1f - density * (1f - tint.blue))
}

/** Assess lettering after the case optics, not only against untinted paper. */
internal fun demoCaseInk(ink: Color, paper: Color, style: DemoStyle): Color {
    val transmission = demoCaseTransmission(style.color("tint"), style.number("opacity"))
    fun viewed(color: Color): Color {
        val transmitted = Color(color.red * transmission.red, color.green * transmission.green, color.blue * transmission.blue)
        val absorption = lerp(transmitted, style.color("tint"), style.number("opacity") * .23f)
        val haze = lerp(absorption, style.color("hazeColor", Color(0xFFF4F8FA)), style.number("haze"))
        return lerp(haze, Color.White, .055f)
    }
    val background = viewed(paper).luminance()
    val target = if (background < .179f) Color.White else Color.Black
    for (step in 0..20) {
        val candidate = lerp(ink, target, step / 20f)
        val foreground = viewed(candidate).luminance()
        if ((maxOf(background, foreground) + .05f) / (minOf(background, foreground) + .05f) >= 4.5f) return candidate
    }
    return target
}
