package com.example.androidmixtape.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.viewmodel.LibraryStatus
import com.example.androidmixtape.viewmodel.MixtapeScreen
import com.example.androidmixtape.viewmodel.MixtapeUiState
import com.example.androidmixtape.viewmodel.buildMixTapeGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MixtapeAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun permissionPromptExplainsAudioAccessAndRequestsPermission() {
        var requested = false
        composeRule.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(
                    state = MixtapeUiState(status = LibraryStatus.PermissionRequired),
                    onRequestPermission = { requested = true },
                    onRefresh = {},
                    onTogglePlayPause = {},
                    onPrevious = {},
                    onNext = {},
                    onSeekTo = {},
                )
            }
        }

        composeRule.onNodeWithText("Mixtape needs permission to read audio files stored on this device.").assertIsDisplayed()
        composeRule.onNodeWithText("Allow audio access").performClick()
        assertEquals(true, requested)
    }

    @Test
    fun readyStateDefaultsToMixTapesWithoutLibraryNavigation() {
        val tracks = numberedTracks(13)
        composeRule.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(
                    state = MixtapeUiState(
                        status = LibraryStatus.Ready,
                        tracks = tracks,
                        mixTapeGroups = buildMixTapeGroups(tracks),
                    ),
                    onRequestPermission = {},
                    onRefresh = {},
                    onMixTapeGroupClick = {},
                    onBackToMixTapes = {},
                    onTogglePlayPause = {},
                    onPrevious = {},
                    onNext = {},
                    onSeekTo = {},
                )
            }
        }

        composeRule.onNodeWithText("Mix Tape 1").assertIsDisplayed()
        composeRule.onAllNodesWithText("Library").assertCountEquals(0)
    }

    @Test
    fun mixTapesScreenOffersSettingsEntryPoint() {
        var openedSettings = false
        val tracks = numberedTracks(13)
        composeRule.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(
                    state = MixtapeUiState(
                        status = LibraryStatus.Ready,
                        screen = MixtapeScreen.MixTapes,
                        tracks = tracks,
                        mixTapeGroups = buildMixTapeGroups(tracks),
                    ),
                    onRequestPermission = {},
                    onRefresh = {},
                    onShowSettings = { openedSettings = true },
                    onMixTapeGroupClick = {},
                    onBackToMixTapes = {},
                    onTogglePlayPause = {},
                    onPrevious = {},
                    onNext = {},
                    onSeekTo = {},
                )
            }
        }

        composeRule.onNodeWithText("Settings").performClick()
        assertEquals(true, openedSettings)
    }

    @Test
    fun mixTapesScreenDoesNotOfferLibraryNavigation() {
        val tracks = numberedTracks(13)
        composeRule.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(
                    state = MixtapeUiState(
                        status = LibraryStatus.Ready,
                        screen = MixtapeScreen.MixTapes,
                        tracks = tracks,
                        mixTapeGroups = buildMixTapeGroups(tracks),
                    ),
                    onRequestPermission = {},
                    onRefresh = {},
                    onShowSettings = {},
                    onMixTapeGroupClick = {},
                    onBackToMixTapes = {},
                    onTogglePlayPause = {},
                    onPrevious = {},
                    onNext = {},
                    onSeekTo = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Library").assertCountEquals(0)
    }

    @Test
    fun nowPlayingScreenDoesNotOfferHeaderNavigationOrTitle() {
        val tracks = numberedTracks(13)
        composeRule.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(
                    state = MixtapeUiState(
                        status = LibraryStatus.Ready,
                        screen = MixtapeScreen.NowPlaying,
                        tracks = tracks,
                        mixTapeGroups = buildMixTapeGroups(tracks),
                        queueTracks = tracks,
                        currentTrack = tracks.first(),
                        currentIndex = 0,
                        durationMs = tracks.first().durationMs,
                        canGoNext = true,
                    ),
                    onRequestPermission = {},
                    onRefresh = {},
                    onShowSettings = {},
                    onMixTapeGroupClick = {},
                    onBackToMixTapes = {},
                    onTogglePlayPause = {},
                    onPrevious = {},
                    onNext = {},
                    onSeekTo = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Settings").assertCountEquals(0)
        composeRule.onAllNodesWithText("Mix Tapes").assertCountEquals(0)
        composeRule.onAllNodesWithText("Now Playing").assertCountEquals(0)
    }

    @Test
    fun settingsScreenShowsResetSectionAndCallsResetAllMixtapes() {
        var resetCount = 0
        val tracks = numberedTracks(13)
        composeRule.setContent {
            AndroidMixtapeTheme {
                MixtapeApp(
                    state = MixtapeUiState(
                        status = LibraryStatus.Ready,
                        screen = MixtapeScreen.Settings,
                        tracks = tracks,
                        mixTapeGroups = buildMixTapeGroups(tracks),
                        message = "2 mix tapes ready",
                    ),
                    onRequestPermission = {},
                    onRefresh = {},
                    onMixTapeGroupClick = {},
                    onBackToMixTapes = {},
                    onTogglePlayPause = {},
                    onPrevious = {},
                    onNext = {},
                    onSeekTo = {},
                    onResetAllMixTapes = { resetCount += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Library").performClick()
        composeRule.onNodeWithText("Reset all mixtapes")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(0, resetCount)
        composeRule.onNodeWithText("Reset mixtapes").performClick()
        assertEquals(1, resetCount)
    }

}

private fun numberedTracks(count: Int): List<Track> = (1..count).map { number ->
    Track(
        id = number.toLong(),
        title = "Track $number",
        artist = "Artist $number",
        durationMs = number * 1_000L,
        uri = "content://track/$number",
    )
}
