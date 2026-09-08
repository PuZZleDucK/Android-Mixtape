package com.example.androidmixtape.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.viewmodel.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NowPlayingSettingsGestureTest {
    @get:Rule val rule = createComposeRule()

    @Test fun tapsLongPressCancellationAndAccessibleSettingsKeepMode() {
        val tracks = (1..4).map { Track(it.toLong(), "Track $it", "Artist", 180_000L, "content://track/$it") }
        val state = mutableStateOf(MixtapeUiState(
            status = LibraryStatus.Ready, screen = MixtapeScreen.NowPlaying,
            tracks = tracks, queueTracks = tracks, mixTapeGroups = buildMixTapeGroups(tracks),
            currentMixtapeIndex = 0, currentTrack = tracks.first(), currentIndex = 0,
        ))
        var settingsCalls = 0
        rule.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(
                    state = state.value, onRequestPermission = {}, onRefresh = {},
                    onTogglePlayPause = {}, onPrevious = {}, onNext = {}, onSeekTo = {},
                    onShowSettings = {
                        settingsCalls++
                        state.value = state.value.copy(screen = MixtapeScreen.Settings)
                    },
                    onExitSettings = { state.value = state.value.copy(screen = MixtapeScreen.NowPlaying) },
                )
            }
        }
        val tracksLabel = "Show mix tape list"
        val tapesLabel = "Show current track list"
        rule.onNodeWithContentDescription(tracksLabel).performTouchInput { click() }
        rule.onNodeWithContentDescription(tapesLabel).assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, settingsCalls) }
        rule.onNodeWithContentDescription(tapesLabel).performTouchInput { click() }
        for (label in listOf(tracksLabel, tapesLabel)) {
            rule.onNodeWithContentDescription(label).performTouchInput { down(center); advanceEventTime(50); cancel() }
            rule.onNodeWithContentDescription(label).assertIsDisplayed()
            rule.onNodeWithContentDescription(label).performTouchInput { longClick() }
            rule.runOnIdle { assertEquals(MixtapeScreen.Settings, state.value.screen) }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            rule.onNodeWithContentDescription(label).assertIsDisplayed()
            rule.onNodeWithContentDescription(label).performSemanticsAction(SemanticsActions.OnLongClick) { action -> action() }
            rule.runOnIdle { assertEquals(MixtapeScreen.Settings, state.value.screen) }
            rule.onNodeWithContentDescription("Back").performClick()
            rule.onNodeWithContentDescription(label).assertIsDisplayed().performTouchInput { click() }
        }
        rule.runOnIdle { assertEquals(4, settingsCalls) }
    }
}
