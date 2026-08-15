package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.DeleteTrackResult
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import java.lang.reflect.Method
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MixtapeSymbolInteractionViewModelContractTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun cyclingCurrentMixtapeSymbolColorRequiresPlayingTapeAndPersistsWithoutResettingPlayback() = runTest {
        val visualStore = InMemoryMixtapeVisualPropertiesStore()
        val viewModel = viewModelWith(
            repository = SymbolInteractionFakeRepository(symbolInteractionTracks(3)),
            visualPropertiesStore = visualStore,
        )
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)

        val cycleSymbolColor = requiredNoArgMethod(
            viewModel,
            "cycleCurrentMixtapeSymbolColor",
            "MixtapeViewModel must expose cycleCurrentMixtapeSymbolColor() for the Now Playing symbol tap action.",
        )
        val before = viewModel.uiState.value
        val beforeColor = symbolColorOf(before.currentMixtapeVisualProperties)

        cycleSymbolColor.invoke(viewModel)

        val after = viewModel.uiState.value
        val afterColor = symbolColorOf(after.currentMixtapeVisualProperties)
        assertNotEquals(
            "Tapping the current mixtape symbol while playback is active should cycle to a different persisted symbol color.",
            beforeColor,
            afterColor,
        )
        assertEquals("Color cycling must not switch tracks.", before.currentTrack, after.currentTrack)
        assertEquals("Color cycling must not reset the queue index.", before.currentIndex, after.currentIndex)
        assertTrue("Color cycling must leave active playback running.", after.isPlaying)

        val stableKey = after.currentMixtapeStableKey ?: missing("Expected the selected mixtape to expose a stable key.")
        val savedProperties = visualStore.propertiesFor(stableKey)
            ?: missing("Color cycling must save updated visual properties for the selected stable mixtape key.")
        assertEquals("The cycled color must be persisted per mixtape.", afterColor, symbolColorOf(savedProperties))
        assertEquals(
            "The selected Mix Tapes spine group must refresh to the same cycled symbol color as Now Playing.",
            afterColor,
            symbolColorOf(selectedMixTapeGroup(after).visualProperties),
        )

        viewModel.togglePlayPause()
        val pausedColor = symbolColorOf(viewModel.uiState.value.currentMixtapeVisualProperties)
        cycleSymbolColor.invoke(viewModel)

        assertEquals(
            "Tapping the current mixtape symbol while paused/stopped should be a no-op.",
            pausedColor,
            symbolColorOf(viewModel.uiState.value.currentMixtapeVisualProperties),
        )
    }

    @Test
    fun cyclingCurrentMixtapeNameColorPersistsAndRefreshesSelectedListGroup() = runTest {
        val visualStore = InMemoryMixtapeVisualPropertiesStore()
        val viewModel = viewModelWith(
            repository = SymbolInteractionFakeRepository(symbolInteractionTracks(3)),
            visualPropertiesStore = visualStore,
        )
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)

        val cycleNameColor = requiredNoArgMethod(
            viewModel,
            "cycleCurrentMixtapeNameColor",
            "MixtapeViewModel must expose cycleCurrentMixtapeNameColor() for the current tape/name font color tap action.",
        )
        val before = viewModel.uiState.value
        val beforeColor = before.currentMixtapeVisualProperties.nameColor

        cycleNameColor.invoke(viewModel)

        val after = viewModel.uiState.value
        val afterColor = after.currentMixtapeVisualProperties.nameColor
        assertNotEquals(
            "Tapping the current mixtape name while playback is active should cycle to a different persisted name/font color.",
            beforeColor,
            afterColor,
        )
        assertEquals("Name-color cycling must not switch tracks.", before.currentTrack, after.currentTrack)
        assertEquals("Name-color cycling must not reset the queue index.", before.currentIndex, after.currentIndex)
        assertTrue("Name-color cycling must leave active playback running.", after.isPlaying)

        val stableKey = after.currentMixtapeStableKey ?: missing("Expected the selected mixtape to expose a stable key.")
        assertEquals(
            "The cycled name/font color must be persisted per mixtape.",
            afterColor,
            visualStore.propertiesFor(stableKey)?.nameColor,
        )
        assertEquals(
            "The selected Mix Tapes spine group must refresh to the same cycled name/font color as Now Playing.",
            afterColor,
            selectedMixTapeGroup(after).visualProperties.nameColor,
        )
    }

    @Test
    fun selectingCurrentMixtapeEmbellishmentAcceptsOnlyEnabledSymbolsAndPersistsCurrentTape() = runTest {
        val visualStore = InMemoryMixtapeVisualPropertiesStore()
        val symbolSettingsStore = InMemoryMixtapeSymbolSettingsStore(
            MixtapeSymbolSettings(setOf(MixtapeEmbellishment.Star, MixtapeEmbellishment.Heart)),
        )
        val viewModel = viewModelWith(
            repository = SymbolInteractionFakeRepository(symbolInteractionTracks(3)),
            visualPropertiesStore = visualStore,
            symbolSettingsStore = symbolSettingsStore,
        )
        viewModel.onPermissionResult(true)
        viewModel.selectMixTapeGroup(0)

        val selectSymbol = requiredMethod(
            viewModel,
            "selectCurrentMixtapeEmbellishment",
            parameterTypes = arrayOf(MixtapeEmbellishment::class.java),
            message = "MixtapeViewModel must expose selectCurrentMixtapeEmbellishment(MixtapeEmbellishment) for the long-press selector.",
        )
        val before = viewModel.uiState.value

        selectSymbol.invoke(viewModel, MixtapeEmbellishment.Heart)

        val afterEnabledSelection = viewModel.uiState.value
        assertEquals(
            "Choosing an enabled selector item should update the current tape symbol.",
            MixtapeEmbellishment.Heart,
            afterEnabledSelection.currentMixtapeVisualProperties.embellishment,
        )
        assertEquals("Symbol selection must not switch tracks.", before.currentTrack, afterEnabledSelection.currentTrack)
        assertEquals("Symbol selection must not reset the queue index.", before.currentIndex, afterEnabledSelection.currentIndex)
        assertEquals("Symbol selection must not pause playback.", before.isPlaying, afterEnabledSelection.isPlaying)

        val stableKey = afterEnabledSelection.currentMixtapeStableKey ?: missing("Expected the selected mixtape to expose a stable key.")
        assertEquals(
            "Selected symbol must be persisted per stable mixtape key.",
            MixtapeEmbellishment.Heart,
            visualStore.propertiesFor(stableKey)?.embellishment,
        )
        assertEquals(
            "The selected Mix Tapes spine group must refresh to the same selected symbol as Now Playing.",
            MixtapeEmbellishment.Heart,
            selectedMixTapeGroup(afterEnabledSelection).visualProperties.embellishment,
        )

        selectSymbol.invoke(viewModel, MixtapeEmbellishment.Crown)

        assertEquals(
            "Selecting a globally disabled symbol should be ignored rather than persisted.",
            MixtapeEmbellishment.Heart,
            viewModel.uiState.value.currentMixtapeVisualProperties.embellishment,
        )
        assertEquals(
            "A rejected disabled symbol must not overwrite persisted visual properties.",
            MixtapeEmbellishment.Heart,
            visualStore.propertiesFor(stableKey)?.embellishment,
        )
    }

    private fun viewModelWith(
        repository: AudioRepository,
        visualPropertiesStore: MixtapeVisualPropertiesStore = InMemoryMixtapeVisualPropertiesStore(),
        symbolSettingsStore: MixtapeSymbolSettingsStore = InMemoryMixtapeSymbolSettingsStore(),
    ): MixtapeViewModel = MixtapeViewModel(
        repository = repository,
        controller = MixtapeController(FakePlayerEngine()),
        visualPropertiesStore = visualPropertiesStore,
        symbolSettingsStore = symbolSettingsStore,
    )

    private fun symbolColorOf(properties: MixtapeVisualProperties): Any {
        val getter = properties.javaClass.methods.singleOrNull { method ->
            method.parameterCount == 0 && method.name in setOf("getSymbolColor", "getSymbolInk")
        } ?: missing(
            "MixtapeVisualProperties must expose a persisted symbolColor/symbolInk property for the current mixtape symbol color.",
        )
        return getter.invoke(properties)
            ?: missing("Mixtape symbol color/ink must never be null.")
    }

    private fun selectedMixTapeGroup(state: MixtapeUiState): MixTapeGroup {
        val stableKey = state.currentMixtapeStableKey ?: missing("Expected the selected mixtape to expose a stable key.")
        return state.mixTapeGroups.singleOrNull { group -> group.contractStableKey() == stableKey }
            ?: missing("Expected the selected stable key to identify exactly one Mix Tapes spine group.")
    }

    private fun MixTapeGroup.contractStableKey(): String = buildString {
        append(startIndex)
        append('|')
        tracks.joinTo(this, separator = ",") { it.id.toString() }
    }

    private fun requiredNoArgMethod(target: Any, name: String, message: String): Method =
        requiredMethod(target, name, emptyArray(), message)

    private fun requiredMethod(target: Any, name: String, parameterTypes: Array<Class<*>>, message: String): Method =
        target.javaClass.methods.singleOrNull { method ->
            method.name == name && method.parameterTypes.contentEquals(parameterTypes)
        } ?: missing(message)

    private fun missing(message: String): Nothing {
        fail(message)
        throw AssertionError(message)
    }
}

private class SymbolInteractionFakeRepository(
    private val tracks: List<Track>,
) : AudioRepository {
    override suspend fun loadTracks(): List<Track> = tracks

    override suspend fun deleteTrack(track: Track): DeleteTrackResult =
        DeleteTrackResult.Failure("Delete from device is not needed for this contract test")
}

private fun symbolInteractionTracks(count: Int): List<Track> = (1..count).map { number ->
    Track(
        id = number.toLong(),
        title = "Track $number",
        artist = "Artist $number",
        durationMs = number * 1_000L,
        uri = "content://track/$number",
    )
}
