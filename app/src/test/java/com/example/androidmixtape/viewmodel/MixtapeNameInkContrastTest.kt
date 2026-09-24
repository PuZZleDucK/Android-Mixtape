package com.example.androidmixtape.viewmodel

import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Check the ink on the actual paper/label colors drawn by both renderers. */
class MixtapeNameInkContrastTest {
    private val newInks = listOf("Cyan", "Fuchsia", "Orange", "Teal")

    @Test fun newInksRemainDistinctAndReadableOnPhoneSpines() {
        val source = source("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt")
        val mapping = source.substringAfter("private fun MixtapeSymbolColor.toComposeColor()")
            .substringBefore("private fun mixtapeTrackCountLabel")
        val inks = newInks.map { name ->
            val hex = Regex("MixtapeSymbolColor\\.$name -> Color\\(0xFF([0-9A-Fa-f]{6})\\)")
                .find(mapping)?.groupValues?.get(1)
            requireNotNull(hex) { "Missing phone ink $name" }.toInt(16)
        }
        val palettes = source.substringAfter("private fun MixtapeSpineSkin.palette()")
            .substringBefore("private fun MixtapeSymbolColor.readableName()")
        val labels = Regex("label = Color\\(0xFF([0-9A-Fa-f]{6})\\)")
            .findAll(palettes).map { it.groupValues[1].toInt(16) }.toList()
        assertEquals(MixtapeSpineSkin.entries.size, labels.size)
        assertReadableAndDistinct(inks, labels)
    }

    @Test fun activeMixTapeSpinesUseTheSelectedNameColorOnSleevePaper() {
        val source = source("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt")
        val spine = source.substringAfter("private fun CassetteSpineRow(")
            .substringBefore("private fun LegacyCassetteSpineRow(")
        assertTrue(spine.contains("nameColor.toComposeColor().onPaper(paper.base)"))
        assertTrue(spine.contains("color = nameInk,"))
        val paperColors = Regex("SleevePaperPalette\\([^\\n]*?Color\\(0xFF([0-9A-Fa-f]{6})\\)")
            .findAll(source.substringAfter("private fun SleeveTheme.paperPalette(")
                .substringBefore("@OptIn(ExperimentalFoundationApi::class)"))
            .map { it.groupValues[1].toInt(16) }.toList()
        assertEquals(SleeveTheme.entries.size, paperColors.size)
        // The renderer adjusts the ink toward white on dark sleeves, black on light ones.
        val inks = listOf(0x006B88, 0xA00070, 0xA44200, 0x00733D)
        inks.forEach { ink -> paperColors.forEach { paper ->
            val background = luminance(paper)
            val target = if (background < 0.25) 0xFFFFFF else 0x000000
            val adjusted = (0..20).map { step ->
                (0..2).fold(0) { rgb, channel ->
                    val shift = (2 - channel) * 8
                    val a = (ink shr shift) and 255
                    val b = (target shr shift) and 255
                    (rgb shl 8) or (a + (b - a) * step / 20.0).toInt()
                }
            }.first { candidate ->
                val foreground = luminance(candidate)
                (maxOf(background, foreground) + 0.05) / (minOf(background, foreground) + 0.05) >= 4.5
            }
            val ratio = (maxOf(background, luminance(adjusted)) + 0.05) /
                (minOf(background, luminance(adjusted)) + 0.05)
            assertTrue("Sleeve paper %06X hides ink %06X".format(paper, ink), ratio >= 4.5)
        } }
    }

    @Test fun carArtworkUsesMatchingInksOnItsRenderedLabels() {
        val phone = source("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt")
        val car = source("src/modern/java/com/example/androidmixtape/car/MixtapeCarArtworkRenderer.kt")
        val phoneMapping = phone.substringAfter("private fun MixtapeSymbolColor.toComposeColor()")
            .substringBefore("private fun mixtapeTrackCountLabel")
        val carMapping = car.substringAfter("private fun symbolColor(color: MixtapeSymbolColor)")
            .substringBefore("private fun palette(skin:")
        val inks = newInks.map { name ->
            val phoneHex = Regex("MixtapeSymbolColor\\.$name -> Color\\(0xFF([0-9A-Fa-f]{6})\\)")
                .find(phoneMapping)?.groupValues?.get(1)
            requireNotNull(phoneHex) { "Missing phone ink $name" }
            val components = Regex("MixtapeSymbolColor\\.$name -> Color\\.rgb\\((\\d+), (\\d+), (\\d+)\\)")
                .find(carMapping)?.groupValues?.drop(1)?.map(String::toInt)
            requireNotNull(components) { "Missing car ink $name" }
            val rgb = components.fold(0) { result, part -> (result shl 8) or part }
            assertEquals("Car and phone ink differ for $name", phoneHex.toInt(16), rgb)
            rgb
        }
        val palettes = car.substringAfter("private val PALETTES = listOf(")
        val labels = Regex("SpinePalette\\([^\\n]+\\)") // Label is the third argument, after shell and paper.
            .findAll(palettes).map { match ->
                val values = Regex("0xFF([0-9A-Fa-f]{6})").findAll(match.value).map { it.groupValues[1] }.toList()
                values[2].toInt(16)
            }.toList()
        assertEquals(6, labels.size)
        assertReadableAndDistinct(inks, labels)
    }

    private fun assertReadableAndDistinct(inks: List<Int>, labels: List<Int>) {
        inks.forEach { ink -> labels.forEach { label ->
            val light = maxOf(luminance(ink), luminance(label))
            val dark = minOf(luminance(ink), luminance(label))
            assertTrue("Ink %06X on label %06X has poor contrast".format(ink, label), (light + 0.05) / (dark + 0.05) >= 4.5)
        } }
        inks.forEachIndexed { index, ink -> inks.drop(index + 1).forEach { other ->
            val distance = sqrt((0..2).sumOf { channel ->
                val shift = (2 - channel) * 8
                val delta = ((ink shr shift) and 255) - ((other shr shift) and 255)
                (delta * delta).toDouble()
            })
            assertTrue("Inks %06X and %06X look too similar".format(ink, other), distance >= 60)
        } }
    }

    private fun luminance(rgb: Int): Double = listOf(16, 8, 0).zip(listOf(0.2126, 0.7152, 0.0722))
        .sumOf { (shift, weight) ->
            val channel = ((rgb shr shift) and 255) / 255.0
            (if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)) * weight
        }

    private fun source(path: String): String = listOf(File("app/$path"), File(path))
        .first { it.exists() }.readText()
}
