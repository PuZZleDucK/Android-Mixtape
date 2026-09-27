package com.example.androidmixtape.ui

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.viewmodel.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HandwritingFontSizeUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("font_size_test_$name", mode)
    }

    @Test fun preferencesDefaultToLargeAndRoundTripEverySize() {
        val prefs = context.getSharedPreferences("mixtape_settings", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            assertEquals(HandwritingFontSize.Large, SharedPreferencesMixtapeSettingsStore(context).settings().handwritingFontSize)
            prefs.edit().putString("handwriting_font_size", "unknown").commit()
            assertEquals(HandwritingFontSize.Large, SharedPreferencesMixtapeSettingsStore(context).settings().handwritingFontSize)
            for (size in HandwritingFontSize.entries) {
                SharedPreferencesMixtapeSettingsStore(context).saveSettings(MixtapeSettings(handwritingFontSize = size))
                assertEquals(size, SharedPreferencesMixtapeSettingsStore(context).settings().handwritingFontSize)
            }
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun bothSettingsPagesChangeSizeAndNowPlayingRowsShrink() {
        val track = Track(801, "Night drive", "Evening radio", 180_000, "preview://font-size")
        val properties = MixtapeVisualProperties(handwritingFont = MixtapeHandwritingFont.Kalam)
        val group = MixTapeGroup("Night drive", listOf(track), 0, properties)
        val state = mutableStateOf(MixtapeUiState(
            status = LibraryStatus.Ready, screen = MixtapeScreen.Settings,
            tracks = listOf(track), queueTracks = listOf(track), mixTapeGroups = listOf(group),
            currentTrack = track, currentIndex = 0, currentMixtapeIndex = 0,
            currentMixtapeName = group.name, currentMixtapeVisualProperties = properties,
            mixtapeSettings = MixtapeSettings(handwritingMessiness = HandwritingMessiness.Off),
        ))
        compose.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(state.value, onRequestPermission = {}, onRefresh = {},
                    onTogglePlayPause = {}, onPrevious = {}, onNext = {}, onSeekTo = {},
                    onHandwritingFontSizeChange = { state.value = state.value.copy(mixtapeSettings = state.value.mixtapeSettings.copy(handwritingFontSize = it)) },
                    onShowHandwritingFontSettings = { state.value = state.value.copy(screen = MixtapeScreen.HandwritingFontSettings) },
                )
            }
        }
        compose.onNodeWithText("Appearance").performClick()
        compose.onNodeWithText("Large").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Small").performClick().assertIsSelected()
        saveScreenshot("appearance-small")
        compose.onNodeWithText("Handwriting fonts").performScrollTo().performClick()
        compose.onNodeWithText("Small").assertIsSelected()
        val heights = mutableListOf<Float>()
        for (size in HandwritingFontSize.entries) {
            compose.onNodeWithText(size.name).performClick().assertIsSelected()
            if (size == HandwritingFontSize.Medium) saveScreenshot("font-settings-medium")
            compose.runOnIdle { state.value = state.value.copy(screen = MixtapeScreen.NowPlaying) }
            val bounds = compose.onNodeWithContentDescription("Track actions for ${track.title}").fetchSemanticsNode().boundsInRoot
            heights += bounds.height
            saveScreenshot("playing-${size.name}")
            compose.runOnIdle { state.value = state.value.copy(screen = MixtapeScreen.HandwritingFontSettings) }
            compose.onNodeWithText(size.name).assertIsSelected()
        }
        assertTrue("Expected increasing row sizes, got $heights", heights.zipWithNext().all { (small, large) -> small < large })
        assertEquals(0.7f, heights[0] / heights[2], 0.03f)
        assertEquals(0.85f, heights[1] / heights[2], 0.03f)
    }

    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(300)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "font-size").apply { mkdirs() }
        val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
