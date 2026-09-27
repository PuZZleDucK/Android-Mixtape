package com.example.androidmixtape.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.name.SharedPreferencesMixtapeNameStore
import com.example.androidmixtape.playback.ExternalPlaybackSnapshot
import com.example.androidmixtape.playback.MixtapeController
import com.example.androidmixtape.playback.PlayerEngine
import com.example.androidmixtape.viewmodel.*
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MixtapeRemovalUiTest {
    @get:Rule val composeRule = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val preferenceNames = mutableSetOf<String>()
    private val prefix = "removal-test-${UUID.randomUUID()}-"
    // Keep test stores separate from the installed app's music and preferences.
    private val context = object : ContextWrapper(instrumentation.targetContext) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            preferenceNames += prefix + name
            return super.getSharedPreferences(prefix + name, mode)
        }
    }
    private val tracks = (1..9).map { Track(it.toLong(), "Test song $it", "Test artist", 180_000, "content://test/$it") }
    private val controllers = mutableListOf<MixtapeController>()

    private fun createPlayer(): MixtapeViewModel {
        val controller = MixtapeController(RemovalTestPlayer()).also { controllers += it }
        return MixtapeViewModel(
            repository = object : AudioRepository { override suspend fun loadTracks() = tracks },
            controller = controller,
            settingsStore = InMemoryMixtapeSettingsStore(MixtapeSettings(3, ArtistGrouping.NoGrouping)),
            nameStore = SharedPreferencesMixtapeNameStore(context),
            visualPropertiesStore = SharedPreferencesMixtapeVisualPropertiesStore(context),
            membershipStore = SharedPreferencesMixtapeMembershipStore(context),
            themeSettingsStore = InMemoryMixtapeThemeSettingsStore(MixtapeThemeSettings(deckTheme = DeckTheme.BlackoutPortable)),
        ).also { it.onPermissionResult(true) }
    }

    @After fun cleanUp() {
        instrumentation.runOnMainSync { controllers.forEach { it.release() } }
        preferenceNames.forEach { instrumentation.targetContext.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
    }

    @Test fun removeMenuOnlyEditsCurrentTapeAndSavedStateSurvivesRecreation() {
        val player = mutableStateOf<MixtapeViewModel?>(null)
        instrumentation.runOnMainSync {
            player.value = createPlayer().also { vm ->
                vm.uiState.value.mixtapeNameInfos.forEachIndexed { index, tape ->
                    vm.editMixtapeName(tape.stableKey, listOf("Road songs", "Night bus", "Sunday mix")[index])
                }
                vm.selectMixTapeGroup(0)
            }
        }
        val before = player.value!!.uiState.value
        composeRule.setContent {
            val vm = player.value!!
            val state by vm.uiState.collectAsState()
            Box(Modifier.requiredSize(900.dp, 360.dp)) {
                AndroidMixtapeTheme {
                    MixtapeApp(
                        state = state,
                        onRequestPermission = {}, onRefresh = vm::refresh,
                        onTogglePlayPause = vm::togglePlayPause, onPrevious = vm::previous, onNext = vm::next,
                        onSeekTo = vm::seekTo,
                        onRemoveTrackFromMixtape = { vm.removeTrackFromCurrentMixtape(it.id) },
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("Track actions for Test song 2").performTouchInput { longClick() }
        composeRule.onNodeWithText("Remove from mixtape").performClick()
        composeRule.runOnIdle {
            val after = player.value!!.uiState.value
            assertEquals(listOf(1L, 3L), after.mixTapeGroups[0].tracks.map { it.id })
            assertEquals(before.mixTapeGroups.drop(1).map { it.tracks }, after.mixTapeGroups.drop(1).map { it.tracks })
            assertEquals(before.mixtapeNameInfos, after.mixtapeNameInfos)
            assertEquals(before.mixTapeGroups.map { it.visualProperties }, after.mixTapeGroups.map { it.visualProperties })
            assertEquals(tracks, after.tracks)
        }
        composeRule.onNodeWithContentDescription("Show mix tape list").performClick()
        saveScreenshot()
        composeRule.runOnIdle {
            // Construct fresh preference-store instances, not just another view of an in-memory list.
            player.value = createPlayer()
            val reopened = player.value!!.uiState.value
            assertEquals(before.mixtapeNameInfos, reopened.mixtapeNameInfos)
            assertEquals(before.mixTapeGroups.drop(1).map { it.tracks }, reopened.mixTapeGroups.drop(1).map { it.tracks })
            assertEquals(before.mixTapeGroups.map { it.visualProperties }, reopened.mixTapeGroups.map { it.visualProperties })
            assertFalse(reopened.mixTapeGroups.any { group -> group.tracks.any { it.id == 2L } })
        }
    }

    private fun saveScreenshot() {
        composeRule.waitForIdle()
        android.os.SystemClock.sleep(700)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "mixtape-removal.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}

private class RemovalTestPlayer : PlayerEngine {
    override fun loadPlaylist(tracks: List<Track>) {}
    override fun replacePlaylistPreservingPlayback(tracks: List<Track>, currentIndex: Int) {}
    override fun playIndex(index: Int) {}
    override fun play() {}
    override fun pause() {}
    override fun seekTo(positionMs: Long) {}
    override fun setOnCurrentIndexChanged(listener: ((Int) -> Unit)?) {}
    override fun setOnExternalPlaybackSnapshotChanged(listener: ((ExternalPlaybackSnapshot) -> Unit)?) {}
    override fun release() {}
}
