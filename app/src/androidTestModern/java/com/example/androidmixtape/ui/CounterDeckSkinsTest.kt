package com.example.androidmixtape.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.viewmodel.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Run on the Kunlun emulator in each physical orientation. No production debug UI. */
class CounterDeckSkinsTest {
    @get:Rule val compose = createComposeRule()
    private val track = Track(1, "Counter wheel verification", "Test artist", 999_000L, "content://counter-test")
    private val state = mutableStateOf(MixtapeUiState(
        status = LibraryStatus.Ready,
        screen = MixtapeScreen.NowPlaying,
        tracks = listOf(track),
        queueTracks = listOf(track),
        mixTapeGroups = buildMixTapeGroups(listOf(track)),
        currentTrack = track,
        currentIndex = 0,
        currentMixtapeIndex = 0,
        currentMixtapeName = "Counter wheel verification",
        positionMs = 99_000L,
        durationMs = track.durationMs,
        isPlaying = true,
    ))

    @Test fun allDeckSkinsContainCounterAndRetainAccessibleSmallerToggle() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(state.value, onRequestPermission = {}, onRefresh = {},
                    onTogglePlayPause = {}, onPrevious = {}, onNext = {}, onSeekTo = {})
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        android.os.SystemClock.sleep(500)
        for (theme in DeckTheme.entries) {
            compose.runOnIdle {
                state.value = state.value.copy(positionMs = 99_000L,
                    counterRevision = state.value.counterRevision + 1L,
                    mixtapeThemeSettings = MixtapeThemeSettings(deckTheme = theme))
            }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            val bounds = compose.onNodeWithContentDescription("Tape counter 099").getUnclippedBoundsInRoot()
            assertTrue(bounds.right > bounds.left && bounds.bottom > bounds.top)
            val toggle = compose.onNodeWithContentDescription("Show mix tape list")
            val toggleBounds = toggle.getUnclippedBoundsInRoot()
            assertEquals(48.dp, toggleBounds.right - toggleBounds.left)
            assertEquals(48.dp, toggleBounds.bottom - toggleBounds.top)
            save("${theme.name}-099")
            compose.runOnIdle { state.value = state.value.copy(positionMs = 100_000L) }
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeBy(64)
            save("${theme.name}-carry")
            assertEquals(bounds, compose.onNodeWithContentDescription("Tape counter 100").getUnclippedBoundsInRoot())
            compose.mainClock.advanceTimeBy(200)
            toggle.performClick()
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithContentDescription("Show current track list").performClick()
            compose.mainClock.advanceTimeByFrame()
        }
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(100)
        val context = instrumentation.targetContext
        val orientation = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
        val dir = File(context.getExternalFilesDir(null), "counter-wheels/skins").apply { mkdirs() }
        File(dir, "$orientation-$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
