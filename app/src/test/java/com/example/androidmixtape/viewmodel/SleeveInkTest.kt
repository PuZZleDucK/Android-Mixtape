package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.pow
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class SleeveInkTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun everySleeveHasThreeDistinctReadableInks() {
        val backgrounds = mapOf(
            SleeveTheme.StudioIndex to 0xFFE8E1C8,
            SleeveTheme.NotebookPage to 0xFFEEE4C6,
            SleeveTheme.AfterDark to 0xFF242C45,
            SleeveTheme.CottonPaper to 0xFFF2F0E2,
            SleeveTheme.BlankWhite to 0xFFFBFAF5,
            SleeveTheme.RuledNotebook to 0xFFF8F6EC,
            SleeveTheme.AlbumPrint to 0xFF101319,
            SleeveTheme.KraftBrown to 0xFFC9A876,
            SleeveTheme.MidnightGrid to 0xFF182238,
            SleeveTheme.CoralAlbum to 0xFFF1D8CB,
            SleeveTheme.ForestFleck to 0xFFD3D9BD,
            SleeveTheme.BlueprintGrid to 0xFF155281,
            SleeveTheme.GraphPaper to 0xFFF7F6EE,
            SleeveTheme.SkinCreamRed to 0xFFFFF6DD,
            SleeveTheme.SkinBlushBlue to 0xFFFFF4F7,
            SleeveTheme.SkinSkyGreen to 0xFFF3FAFF,
            SleeveTheme.SkinMintAmber to 0xFFF6FFF6,
            SleeveTheme.SkinLemonNavy to 0xFFFFF6CA,
            SleeveTheme.SkinPinkZineBorder to 0xFFFFF8F1,
            SleeveTheme.SkinVioletLibraryStripe to 0xFFF8F2DA,
            SleeveTheme.SkinMintCollageTab to 0xFFFFFBEB,
            SleeveTheme.SkinAmberIndexBlock to 0xFFFFF5DC,
            SleeveTheme.SkinPowderBlueMarker to 0xFFD5ECFA,
            SleeveTheme.SkinCoralStickerRail to 0xFFFFE2D8,
            SleeveTheme.SkinEmeraldNotebookLines to 0xFFF9FFE9,
            SleeveTheme.SkinSepiaNewsprintFrame to 0xFFF4E5C5,
            SleeveTheme.SkinBlackPhotoNegative to 0xFFECE5D3,
            SleeveTheme.SkinRainbowCutout to 0xFFFFFBEC,
            SleeveTheme.SkinSunburstCreamRail to 0xFFFFF7D8,
            SleeveTheme.SkinAquaLibraryTab to 0xFFF7F1D2,
            SleeveTheme.SkinCandyStripePink to 0xFFFFF6F0,
            SleeveTheme.SkinGoldenMemoBlock to 0xFFFFF2BF,
            SleeveTheme.SkinTomatoBorderLabel to 0xFFFFF7E8,
            SleeveTheme.SkinTealNotebookRail to 0xFFF3FFE8,
            SleeveTheme.SkinVioletIndexPanel to 0xFFFFF3CF,
            SleeveTheme.SkinLimeCutoutStripe to 0xFFFFF8DB,
            SleeveTheme.SkinPeachGridSticker to 0xFFFFF4D9,
            SleeveTheme.SkinMidnightRainbowFrame to 0xFFF1E8C8,
        )
        for (theme in SleeveTheme.entries) {
            val colors = theme.inkColors()
            assertEquals(3, colors.size)
            assertEquals(3, colors.map { it.argb }.distinct().size)
            assertEquals(3, colors.map { it.label }.distinct().size)
            for (ink in SleeveInk.entries) {
                val color = theme.inkColor(ink)
                assertEquals(255L, color.argb ushr 24)
                val foreground = luminance(color.argb)
                val background = luminance(backgrounds.getValue(theme))
                val contrast = (maxOf(foreground, background) + 0.05) / (minOf(foreground, background) + 0.05)
                assertTrue("$theme ${color.label} contrast $contrast", contrast >= 4.5)
            }
        }
    }

    @Test fun originalPaletteColorsAreUnchanged() {
        val expected = listOf(0xFF23272E, 0xFF202634, 0xFFF1EFE8, 0xFF33261A, 0xFFEDF3FF,
            0xFF38252A, 0xFF263426, 0xFFF2F8FC, 0xFF27313B)
        assertEquals(expected, SleeveTheme.entries.take(9).map { it.inkColor(SleeveInk.Original).argb })
    }

    @Test fun jitterMapsDeterministicallyToAllThreeSlots() {
        for (seed in 0 until 5_000) assertEquals(SleeveInk.entries[seed % 3], SleeveInk.fromJitter(seed))
        assertEquals(SleeveInk.AlternateTwo, SleeveInk.fromJitter(-1))
    }

    @Test fun newTapesPickInkFromJitterAndSaveIt() = runTest {
        val store = InMemoryMixtapeVisualPropertiesStore()
        val vm = createViewModel(store, count = 60)
        vm.onPermissionResult(true)
        vm.uiState.value.mixTapeGroups.forEach { group ->
            assertEquals(SleeveInk.fromJitter(group.visualProperties.jitterStartIndex), group.visualProperties.sleeveInk)
            assertEquals(group.visualProperties, store.propertiesFor(group.stableKey))
        }
    }

    @Test fun editedInkSurvivesReloadAndOtherCustomizationWithoutChangingJitter() = runTest {
        val store = InMemoryMixtapeVisualPropertiesStore()
        val vm = createViewModel(store)
        vm.onPermissionResult(true)
        vm.selectMixTapeGroup(0)
        vm.seekTo(12_000)
        val before = vm.uiState.value
        val original = before.currentMixtapeVisualProperties
        val chosen = SleeveInk.entries[(original.sleeveInk.ordinal + 1) % 3]
        vm.updateCurrentMixtapeCustomization(MixtapeCustomization(
            name = "Custom tape", decorativeId = original.decorativeId, handwritingFont = original.handwritingFont,
            embellishment = original.embellishment, symbolColor = original.symbolColor, nameColor = original.nameColor,
            cassetteTheme = original.cassetteTheme, screwTheme = original.screwTheme, stickerTheme = original.stickerTheme,
            caseTheme = original.caseTheme, sleeveTheme = SleeveTheme.GraphPaper, sleeveInk = chosen,
        ))
        val after = vm.uiState.value
        assertEquals(chosen, after.currentMixtapeVisualProperties.sleeveInk)
        assertEquals(original.jitterStartIndex, after.currentMixtapeVisualProperties.jitterStartIndex)
        assertEquals(before.queueTracks, after.queueTracks)
        assertEquals(before.positionMs, after.positionMs)
        assertEquals(before.isPlaying, after.isPlaying)
        val reloaded = createViewModel(store)
        reloaded.onPermissionResult(true)
        assertEquals(chosen, reloaded.uiState.value.mixTapeGroups.single().visualProperties.sleeveInk)
    }

    private fun createViewModel(store: MixtapeVisualPropertiesStore, count: Int = 3): MixtapeViewModel {
        val tracks = (1..count).map { Track(it.toLong(), "Song $it", "Artist", 180_000, "content://song/$it") }
        return MixtapeViewModel(
            repository = object : AudioRepository { override suspend fun loadTracks() = tracks },
            controller = MixtapeController(FakePlayerEngine()), visualPropertiesStore = store,
            mixtapeRandom = Random(23),
        )
    }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val value = ((argb shr shift) and 255).toDouble() / 255
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return channel(16) * 0.2126 + channel(8) * 0.7152 + channel(0) * 0.0722
    }
}
