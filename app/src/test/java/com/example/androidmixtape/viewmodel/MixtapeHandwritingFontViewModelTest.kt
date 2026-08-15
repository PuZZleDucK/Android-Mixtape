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
class MixtapeHandwritingFontViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun generatedMixtapesReceiveCandidateHandwritingFontsAndKeepThemAcrossNavigation() = runTest {
        val viewModel = viewModelWith(MixtapeFontFakeRepository(numberedFontTracks(57)), mixtapeRandom = Random(0))
        viewModel.onPermissionResult(true)

        val initialFonts = viewModel.uiState.value.mixTapeGroups.map { it.handwritingFont }
        assertEquals(listOf("Mix Tape 1", "Mix Tape 2"), viewModel.uiState.value.mixTapeGroups.map { it.name })
        assertEquals("Every generated mixtape must receive one persisted candidate font", 2, initialFonts.size)
        assertTrue(initialFonts.all { it in MixtapeHandwritingFont.entries })

        viewModel.showSettings()
        viewModel.showMixTapes()
        viewModel.backToMixTapes()

        assertEquals(
            "Per-mixtape font assignment must be stable after navigation/state refreshes",
            initialFonts,
            viewModel.uiState.value.mixTapeGroups.map { it.handwritingFont },
        )
    }

    @Test
    fun selectedMixtapeFontIsExposedForNowPlayingCassetteText() = runTest {
        val viewModel = viewModelWith(MixtapeFontFakeRepository(numberedFontTracks(57)), mixtapeRandom = Random(3))
        viewModel.onPermissionResult(true)
        val selectedFont = viewModel.uiState.value.mixTapeGroups[1].handwritingFont

        viewModel.selectMixTapeGroup(1)

        val state = viewModel.uiState.value
        assertEquals(MixtapeScreen.NowPlaying, state.screen)
        assertEquals(
            "Now Playing cassette name and track list must use the same font as the selected spine",
            selectedFont,
            state.currentMixtapeHandwritingFont,
        )
    }

    @Test
    fun injectedRandomMakesFontAssignmentsDeterministicForTests() = runTest {
        val first = fontsFor(Random(7))
        val second = fontsFor(Random(7))

        assertEquals(
            "A fixed injected Random should produce repeatable handwriting font assignments",
            first,
            second,
        )
    }

    private suspend fun fontsFor(random: Random): List<MixtapeHandwritingFont> {
        val viewModel = viewModelWith(MixtapeFontFakeRepository(numberedFontTracks(30)), mixtapeRandom = random)
        viewModel.onPermissionResult(true)
        return viewModel.uiState.value.mixTapeGroups.map { it.handwritingFont }
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

private class MixtapeFontFakeRepository(
    private val tracks: List<Track>,
) : AudioRepository {
    override suspend fun loadTracks(): List<Track> = tracks
}

private fun numberedFontTracks(count: Int): List<Track> = (1..count).map { number ->
    Track(
        id = number.toLong(),
        title = "Font Track $number",
        artist = "Artist $number",
        durationMs = number * 1_000L,
        uri = "content://font-track/$number",
    )
}
