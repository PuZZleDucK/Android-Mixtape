package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MixtapeHandwritingFontResourceContractTest {
    @Test
    fun allCandidateFontsArePackagedAsModernAndroidFontResources() {
        val fontDirectory = fontResourceDirectory()
        assertTrue(
            "Expected handwritten cassette fonts in app/src/modern/res/font so Compose can load them as R.font resources.",
            fontDirectory != null && fontDirectory.isDirectory,
        )

        val expectedResourceFiles = listOf(
            "kalam.ttf",
            "patrick_hand.ttf",
            "caveat.ttf",
            "nanum_pen_script.ttf",
            "indie_flower.ttf",
            "gloria_hallelujah.ttf",
            "architects_daughter.ttf",
            "shadows_into_light.ttf",
        )

        val missing = expectedResourceFiles.filterNot { File(fontDirectory!!, it).isFile }
        assertTrue(
            "Missing handwritten font resource files: ${missing.joinToString()}. " +
                "Copy them from /home/puzzleduck/agent-projects/fonts/fonts and rename hyphenated files to Android-safe underscores.",
            missing.isEmpty(),
        )
    }

    @Test
    fun composeFontMapperCoversAllResourcesAndForcesBoldForThinFonts() {
        val mapper = fontMapperSource()
        assertTrue(
            "Expected a Compose font mapper at app/src/modern/java/com/example/androidmixtape/ui/HandwrittenCassetteFonts.kt.",
            mapper != null && mapper.isFile,
        )
        val source = mapper!!.readText()

        val expectedResourceReferences = listOf(
            "R.font.kalam",
            "R.font.patrick_hand",
            "R.font.caveat",
            "R.font.nanum_pen_script",
            "R.font.indie_flower",
            "R.font.gloria_hallelujah",
            "R.font.architects_daughter",
            "R.font.shadows_into_light",
        )
        val missingReferences = expectedResourceReferences.filterNot(source::contains)
        assertTrue(
            "Font mapper must expose every handwritten font resource. Missing references: ${missingReferences.joinToString()}",
            missingReferences.isEmpty(),
        )

        assertTrue(
            "Indie Flower must be handled by the effective cassette weight helper.",
            source.contains("IndieFlower"),
        )
        assertTrue(
            "Shadows Into Light must be handled by the effective cassette weight helper.",
            source.contains("ShadowsIntoLight"),
        )
        assertTrue(
            "Thin candidate fonts must be rendered at least bold for cassette readability.",
            source.contains("FontWeight.Bold"),
        )
    }

    @Test
    fun cassetteSpineFontSizesApplyRequestedPerFontReductions() {
        val source = fontMapperSource()!!.readText()
        val sizeHelper = source.substringAfter("fun MixtapeHandwritingFont.cassetteSpineFontSize()")
            .substringBefore("fun MixtapeHandwritingFont.effectiveCassetteWeight")

        listOf("Kalam", "PatrickHand").forEach { font ->
            assertTrue("$font should use the two-point-reduced 52sp spine title.", sizeHelper.contains("MixtapeHandwritingFont.$font"))
        }
        assertTrue(sizeHelper.contains("-> 52.sp"))
        listOf("IndieFlower", "ArchitectsDaughter").forEach { font ->
            assertTrue("$font should use the four-point-reduced 50sp spine title.", sizeHelper.contains("MixtapeHandwritingFont.$font"))
        }
        assertTrue(sizeHelper.contains("-> 50.sp"))
        listOf("GloriaHallelujah", "ShadowsIntoLight").forEach { font ->
            assertTrue("$font should use the six-point-reduced 48sp spine title.", sizeHelper.contains("MixtapeHandwritingFont.$font"))
        }
        assertTrue(sizeHelper.contains("-> 48.sp"))
        assertTrue("Caveat and Nanum Pen Script should retain the 54sp baseline.", sizeHelper.contains("-> 54.sp"))

        val offsetHelper = source.substringAfter("fun MixtapeHandwritingFont.cassetteSpineVerticalOffset()")
            .substringBefore("fun MixtapeHandwritingFont.effectiveCassetteWeight")
        assertTrue("Gloria Hallelujah should receive the larger upward spine correction.", offsetHelper.contains("GloriaHallelujah -> (-6).dp"))
        assertTrue("Shadows Into Light should retain its upward spine correction.", offsetHelper.contains("ShadowsIntoLight -> (-3).dp"))
    }

    private fun fontResourceDirectory(): File? = listOf(
        File("app/src/modern/res/font"),
        File("src/modern/res/font"),
    ).firstOrNull { it.isDirectory }

    private fun fontMapperSource(): File? = listOf(
        File("app/src/modern/java/com/example/androidmixtape/ui/HandwrittenCassetteFonts.kt"),
        File("src/modern/java/com/example/androidmixtape/ui/HandwrittenCassetteFonts.kt"),
    ).firstOrNull { it.isFile }
}
