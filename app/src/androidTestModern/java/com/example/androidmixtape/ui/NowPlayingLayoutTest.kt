package com.example.androidmixtape.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.viewmodel.DeckTheme
import com.example.androidmixtape.viewmodel.LibraryStatus
import com.example.androidmixtape.viewmodel.MixtapeScreen
import com.example.androidmixtape.viewmodel.MixtapeThemeSettings
import com.example.androidmixtape.viewmodel.MixtapeUiState
import com.example.androidmixtape.viewmodel.buildMixTapeGroups
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NowPlayingLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val viewport = mutableStateOf(900.dp to 360.dp)
    private val tracks = (1..12).map { index ->
        Track(index.toLong(), "A long track title $index", "Test artist", 180_000L, "content://track/$index")
    }
    private val groups = buildMixTapeGroups(tracks)

    private fun showPlayer() {
        composeRule.setContent {
            Box(Modifier.requiredSize(viewport.value.first, viewport.value.second).testTag("viewport")) {
                AndroidMixtapeTheme {
                    MixtapeApp(
                        state = MixtapeUiState(
                            status = LibraryStatus.Ready,
                            screen = MixtapeScreen.NowPlaying,
                            tracks = tracks,
                            mixTapeGroups = groups,
                            queueTracks = tracks,
                            currentTrack = tracks.first(),
                            currentIndex = 0,
                            currentMixtapeIndex = 0,
                            mixtapeThemeSettings = MixtapeThemeSettings(deckTheme = DeckTheme.BlackoutPortable),
                        ),
                        onRequestPermission = {},
                        onRefresh = {},
                        onTogglePlayPause = {},
                        onPrevious = {},
                        onNext = {},
                        onSeekTo = {},
                    )
                }
            }
        }
    }

    @Test
    fun shortLandscapeKeepsSpineInsidePlayer() {
        showPlayer()
        saveScreenshot("landscape-spine")
        assertFooterFits("Current mixtape preview", "Show mix tape list")
    }

    @Test
    fun shortLandscapeKeepsTrackPreviewInsidePlayer() {
        showPlayer()
        composeRule.onNodeWithContentDescription("Show mix tape list").performClick()
        saveScreenshot("landscape-preview")
        assertFooterFits("Current track preview", "Show current track list")
    }

    @Test
    fun resizingBetweenPortraitAndLandscapeKeepsFooterInsidePlayer() {
        showPlayer()
        for (size in listOf(360.dp to 640.dp, 900.dp to 480.dp, 900.dp to 360.dp, 700.dp to 280.dp)) {
            composeRule.runOnIdle { viewport.value = size }
            assertFooterFits("Current mixtape preview", "Show mix tape list")
            composeRule.onNodeWithContentDescription("Show mix tape list").performClick()
            assertFooterFits("Current track preview", "Show current track list")
            composeRule.onNodeWithContentDescription("Show current track list").performClick()
        }
    }

    private fun assertFooterFits(previewDescription: String, toggleDescription: String) {
        val viewportBounds = composeRule.onNodeWithTag("viewport").getUnclippedBoundsInRoot()
        val preview = composeRule.onNodeWithContentDescription(previewDescription).getUnclippedBoundsInRoot()
        val toggle = composeRule.onNodeWithContentDescription(toggleDescription).getUnclippedBoundsInRoot()
        // The outer player has at least 8dp of bottom padding, even without system insets.
        assertTrue("Preview $preview spills outside viewport $viewportBounds", preview.bottom <= viewportBounds.bottom - 7.dp)
        assertTrue("Toggle $toggle spills outside viewport $viewportBounds", toggle.bottom <= viewportBounds.bottom - 7.dp)
        assertTrue("Toggle was crushed: $toggle", toggle.bottom - toggle.top >= 47.dp)
        val deck = composeRule.onNodeWithContentDescription("Cassette deck").getUnclippedBoundsInRoot()
        assertTrue("Preview $preview overlaps deck $deck", preview.top >= deck.bottom + 3.dp)
        assertTrue("Toggle $toggle overlaps deck $deck", toggle.top >= deck.bottom + 3.dp)
    }

    private fun saveScreenshot(name: String) {
        composeRule.waitForIdle()
        // Allow the Android activity/window animation to finish before capturing evidence.
        android.os.SystemClock.sleep(700)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // UiAutomation also supports the API 24 target, unlike Compose's window PixelCopy.
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val directory = instrumentation.targetContext.getExternalFilesDir(null)!!
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
