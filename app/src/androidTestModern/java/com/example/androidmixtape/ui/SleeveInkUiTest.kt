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
import kotlin.math.roundToInt
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SleeveInkUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun inkPreferencesPersistAndOldTapesKeepTheirOriginalColor() {
        val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("ink_test_$name", mode)
        }
        val prefs = context.getSharedPreferences("mixtape_visual_properties", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            val original = MixtapeVisualProperties(jitterStartIndex = 11)
            for (ink in SleeveInk.entries) {
                val value = original.copy(sleeveInk = ink)
                SharedPreferencesMixtapeVisualPropertiesStore(context).saveProperties("tape", value)
                assertEquals(value, SharedPreferencesMixtapeVisualPropertiesStore(context).propertiesFor("tape"))
            }
            prefs.edit().remove("sleeve_ink:tape").commit()
            assertEquals(original, SharedPreferencesMixtapeVisualPropertiesStore(context).propertiesFor("tape"))
            prefs.edit().putString("sleeve_ink:tape", "unknown").commit()
            assertEquals(original, SharedPreferencesMixtapeVisualPropertiesStore(context).propertiesFor("tape"))
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun editorCyclesAndSavesInkWhileSelectedTrackAccentStaysUnchanged() {
        val tracks = listOf(
            Track(701, "Night drive", "Radio", 180_000, "preview://ink/1"),
            Track(702, "Evening walk", "Radio", 180_000, "preview://ink/2"),
        )
        val initial = MixtapeVisualProperties(sleeveTheme = SleeveTheme.GraphPaper)
        fun state(properties: MixtapeVisualProperties) = MixtapeUiState(
            status = LibraryStatus.Ready, screen = MixtapeScreen.NowPlaying,
            tracks = tracks, queueTracks = tracks, currentTrack = tracks.first(), currentIndex = 0,
            mixTapeGroups = listOf(MixTapeGroup("Night drive", tracks, 0, properties)),
            currentMixtapeIndex = 0, currentMixtapeName = "Night drive", currentMixtapeVisualProperties = properties,
            mixtapeSettings = MixtapeSettings(handwritingMessiness = HandwritingMessiness.Off),
        )
        val current = mutableStateOf(state(initial))
        compose.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(current.value, onRequestPermission = {}, onRefresh = {},
                    onTogglePlayPause = {}, onPrevious = {}, onNext = {}, onSeekTo = {},
                    onUpdateCurrentMixtapeCustomization = { edit ->
                        current.value = state(current.value.currentMixtapeVisualProperties.copy(sleeveInk = edit.sleeveInk, sleeveTheme = edit.sleeveTheme))
                    },
                )
            }
        }
        saveScreenshot("playing-before")
        val selectedBefore = rowPixels("Night drive")
        val inkBefore = rowPixels("Evening walk")
        openEditor()
        inkButton().performScrollTo().performClick()
        compose.onNodeWithContentDescription("Ink color: Plum, 2 of 3").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(SleeveInk.Original, current.value.currentMixtapeVisualProperties.sleeveInk) }
        for (ink in listOf(SleeveInk.AlternateOne, SleeveInk.AlternateTwo, SleeveInk.Original)) {
            openEditor()
            inkButton().performScrollTo().performClick()
            val label = SleeveTheme.GraphPaper.inkColor(ink).label
            compose.onNodeWithContentDescription("Ink color: $label, ${ink.ordinal + 1} of 3").assertIsDisplayed()
            compose.onNodeWithContentDescription("Ink color preview").performScrollTo().assertIsDisplayed()
            saveScreenshot("editor-${ink.name}")
            compose.onNodeWithText("Save").performClick()
            compose.runOnIdle { assertEquals(ink, current.value.currentMixtapeVisualProperties.sleeveInk) }
            saveScreenshot("playing-${ink.name}")
            assertArrayEquals("Selected-track accent must not change", selectedBefore, rowPixels("Night drive"))
            val pixels = rowPixels("Evening walk")
            if (ink == SleeveInk.Original) assertArrayEquals(inkBefore, pixels)
            else assertFalse("Normal track ink must change", inkBefore.contentEquals(pixels))
        }
    }

    private fun openEditor() {
        compose.onNodeWithContentDescription("Current mixtape preview").performTouchInput { longClick() }
        compose.onNodeWithText("Customize mixtape").assertIsDisplayed()
        android.os.SystemClock.sleep(300)
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitForIdle()
    }

    private fun inkButton() = compose.onNodeWithContentDescription("Ink color:", substring = true)

    private fun rowPixels(title: String): IntArray {
        compose.waitForIdle()
        android.os.SystemClock.sleep(750)
        val bounds = compose.onNodeWithContentDescription("Track actions for $title").fetchSemanticsNode().boundsInWindow
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val left = bounds.left.roundToInt().coerceAtLeast(0)
        val top = bounds.top.roundToInt().coerceAtLeast(0)
        val width = minOf(bounds.width.roundToInt(), bitmap.width - left)
        val height = minOf(bounds.height.roundToInt(), bitmap.height - top)
        return IntArray(width * height).also {
            bitmap.getPixels(it, 0, width, left, top, width, height)
            bitmap.recycle()
        }
    }

    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(750)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "sleeve-ink").apply { mkdirs() }
        val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
