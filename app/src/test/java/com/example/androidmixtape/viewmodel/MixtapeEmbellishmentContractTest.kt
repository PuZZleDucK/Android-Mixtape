package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MixtapeEmbellishmentContractTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun embellishmentEnumDefinesOriginalTenDoodlesPlusTenAppendedMusicSymbols() {
        val names = embellishmentConstantNames()

        assertEquals(
            "The mixtape embellishment catalog should keep the original 10 persisted enum names in order, then append the 10 planned music/note symbols.",
            listOf(
                "Star",
                "Heart",
                "LightningBolt",
                "Sparkles",
                "Smiley",
                "Flower",
                "MusicNote",
                "Moon",
                "Swirl",
                "Crown",
                "QuarterNote",
                "EighthNote",
                "BeamedEighthNotes",
                "SixteenthNote",
                "WholeNote",
                "HalfNote",
                "TrebleClef",
                "BassClef",
                "SharpSign",
                "FlatSign",
            ),
            names,
        )
    }

    @Test
    fun visualPropertiesModelCarriesPersistedEmbellishmentBesideFontAndJitter() {
        val embellishmentClass = requiredClass(
            "com.example.androidmixtape.viewmodel.MixtapeEmbellishment",
            "Expected a shared MixtapeEmbellishment enum for persisted per-mixtape doodle assignment.",
        )
        val propertiesClass = requiredClass(
            "com.example.androidmixtape.viewmodel.MixtapeVisualProperties",
            "Expected MixtapeVisualProperties to remain the per-mixtape persisted visual identity model.",
        )

        assertTrue(
            "MixtapeVisualProperties must expose the selected MixtapeEmbellishment so spine rows and the Now Playing cassette can render the same persisted doodle.",
            propertiesClass.methods.any { method ->
                method.parameterCount == 0 && method.returnType == embellishmentClass
            },
        )
    }

    @Test
    fun generatedMixtapesReceiveCandidateEmbellishmentsAndKeepThemAcrossNavigation() = runTest {
        val embellishmentClass = requiredClass(
            "com.example.androidmixtape.viewmodel.MixtapeEmbellishment",
            "Expected a shared MixtapeEmbellishment enum for persisted per-mixtape doodle assignment.",
        )
        val viewModel = viewModelWith(EmbellishmentFakeRepository(numberedEmbellishmentTracks(57)), mixtapeRandom = Random(0))
        viewModel.onPermissionResult(true)

        val initialEmbellishments = viewModel.uiState.value.mixTapeGroups.map { group -> group.visualProperties.embellishmentValue() }
        assertEquals(listOf("Mix Tape 1", "Mix Tape 2"), viewModel.uiState.value.mixTapeGroups.map { it.name })
        assertEquals("Every generated mixtape must receive one persisted candidate embellishment", 2, initialEmbellishments.size)
        assertTrue(initialEmbellishments.all { embellishmentClass.isInstance(it) })

        viewModel.showSettings()
        viewModel.showMixTapes()
        viewModel.backToMixTapes()

        assertEquals(
            "Per-mixtape embellishment assignment must be stable after navigation/state refreshes",
            initialEmbellishments,
            viewModel.uiState.value.mixTapeGroups.map { group -> group.visualProperties.embellishmentValue() },
        )
    }

    @Test
    fun selectedMixtapeEmbellishmentIsExposedForNowPlayingCassetteText() = runTest {
        requiredClass(
            "com.example.androidmixtape.viewmodel.MixtapeEmbellishment",
            "Expected a shared MixtapeEmbellishment enum for persisted per-mixtape doodle assignment.",
        )
        val viewModel = viewModelWith(EmbellishmentFakeRepository(numberedEmbellishmentTracks(57)), mixtapeRandom = Random(3))
        viewModel.onPermissionResult(true)
        val selectedEmbellishment = viewModel.uiState.value.mixTapeGroups[1].visualProperties.embellishmentValue()

        viewModel.selectMixTapeGroup(1)

        val state = viewModel.uiState.value
        assertEquals(MixtapeScreen.NowPlaying, state.screen)
        assertEquals(
            "Now Playing cassette name must use the same persisted embellishment shown on the selected spine.",
            selectedEmbellishment,
            state.currentMixtapeVisualProperties.embellishmentValue(),
        )
    }

    private fun embellishmentConstantNames(): List<String> {
        val embellishmentClass = requiredClass(
            "com.example.androidmixtape.viewmodel.MixtapeEmbellishment",
            "Expected a shared MixtapeEmbellishment enum with 20 hand-drawn doodle/music choices.",
        )
        val constants = embellishmentClass.enumConstants
            ?: throw AssertionError("MixtapeEmbellishment must be an enum so persisted names remain stable.")
        return constants.map { (it as Enum<*>).name }
    }

    private fun Any.embellishmentValue(): Any {
        val getter = javaClass.methods.firstOrNull { method ->
            method.parameterCount == 0 && method.name == "getEmbellishment"
        } ?: throw AssertionError("MixtapeVisualProperties must expose getEmbellishment().")
        return getter.invoke(this) ?: throw AssertionError("MixtapeVisualProperties.embellishment must never be null.")
    }

    private fun requiredClass(name: String, message: String): Class<*> = try {
        Class.forName(name)
    } catch (_: ClassNotFoundException) {
        throw AssertionError(message)
    }

    private fun viewModelWith(
        repository: AudioRepository,
        controller: MixtapeController = MixtapeController(FakePlayerEngine()),
        mixtapeRandom: Random = Random(0),
    ): MixtapeViewModel = MixtapeViewModel(
        repository = repository,
        controller = controller,
        mixtapeRandom = mixtapeRandom,
    )
}

private class EmbellishmentFakeRepository(
    private val tracks: List<Track>,
) : AudioRepository {
    override suspend fun loadTracks(): List<Track> = tracks
}

private fun numberedEmbellishmentTracks(count: Int): List<Track> = (1..count).map { number ->
    Track(
        id = number.toLong(),
        title = "Embellishment Track $number",
        artist = "Artist $number",
        durationMs = number * 1_000L,
        uri = "content://embellishment-track/$number",
    )
}
