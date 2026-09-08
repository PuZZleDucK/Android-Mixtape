package com.example.androidmixtape.ui

import android.graphics.Bitmap
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.viewmodel.*
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in emulator captures of production screens, not a replacement renderer. */
class SpineFontVisualAuditTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captureSupportedFontsInProductionContexts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("spineAuditPhase")
        assumeTrue("Pass spineAuditPhase to write visual audit captures", phase != null)
        val orientation = instrumentation.targetContext.resources.configuration.orientation
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "spine-audit/$phase-$orientation").apply { mkdirs() }
        val state = mutableStateOf(MixtapeUiState())
        val revision = mutableStateOf(0)
        compose.setContent {
            key(revision.value) {
                AndroidMixtapeTheme {
                    MixtapeApp(state.value, onRequestPermission = {}, onRefresh = {},
                        onTogglePlayPause = {}, onPrevious = {}, onNext = {}, onSeekTo = {})
                }
            }
        }
        val tracks = listOf(Track(4833, "Neon Moon", "Night Drive", 201_000, "preview://audit"))
        fun show(value: MixtapeUiState) {
            compose.runOnIdle { state.value = value; revision.value++ }
            compose.waitForIdle()
        }
        fun capture(name: String) {
            compose.waitForIdle()
            android.os.SystemClock.sleep(400) // Allow SurfaceFlinger to present the settled Compose frame.
            // uiAutomation captures the emulator framebuffer including the system bars.
            val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            image.recycle()
        }
        for (font in MixtapeHandwritingFont.entries) {
            val properties = MixtapeVisualProperties(handwritingFont = font, jitterStartIndex = 971,
                decorativeId = "45", sleeveTheme = SleeveTheme.BlankWhite, caseTheme = CaseTheme.CrystalClear)
            val titles = listOf("Agjpqy", "long spaced title ".repeat(8))
            val groups = titles.mapIndexed { index, title -> MixTapeGroup(title, tracks, index,
                properties, stableKey = "audit-$index") }
            val base = MixtapeUiState(status = LibraryStatus.Ready, tracks = tracks, mixTapeGroups = groups)
            show(base)
            compose.onNodeWithText(titles.first()).assertIsDisplayed()
            capture("${font.name}-list")
            titles.forEachIndexed { index, title ->
                show(base.copy(screen = MixtapeScreen.NowPlaying, queueTracks = tracks,
                    currentTrack = tracks.first(), currentIndex = 0, durationMs = 201_000,
                    currentMixtapeStableKey = groups[index].stableKey, currentMixtapeIndex = index,
                    currentMixtapeName = title, currentMixtapeVisualProperties = properties))
                compose.onNodeWithText(title).assertIsDisplayed()
                capture("${font.name}-playing-$index")
            }
        }
        show(MixtapeUiState(status = LibraryStatus.Ready, screen = MixtapeScreen.HandwritingFontSettings))
        for (font in MixtapeHandwritingFont.entries) {
            compose.onNodeWithContentDescription("Settings options").performScrollToIndex(font.ordinal)
            compose.waitForIdle()
            android.os.SystemClock.sleep(400)
            capture("${font.name}-preview")
        }
        File(directory, "configuration.txt").writeText(instrumentation.targetContext.resources.configuration.toString())
    }
}
