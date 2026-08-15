package com.example.androidmixtape.viewmodel

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MixtapeSpineSkinSetBContractTest {
    @Test
    fun visualPropertiesExposeExistingSpineSkinsFollowedByTenReferenceSetBEntries() {
        val skinClass = requiredClass(
            "com.example.androidmixtape.viewmodel.MixtapeSpineSkin",
            "Expected a MixtapeSpineSkin enum for persisted cassette-spine visual variants.",
        )
        assertTrue("MixtapeSpineSkin must be an enum so saved names remain stable.", skinClass.isEnum)

        val skinNames = skinClass.enumConstants.orEmpty().map { (it as Enum<*>).name }
        assertEquals(
            "MixtapeSpineSkin should keep the five existing saved names first, then append exactly ten generic reference-set-B-inspired names before later appended sets.",
            expectedSpineSkinNames,
            skinNames.take(expectedSpineSkinNames.size),
        )

        val propertiesClass = requiredClass(
            "com.example.androidmixtape.viewmodel.MixtapeVisualProperties",
            "Expected MixtapeVisualProperties to carry all persisted visual identity fields.",
        )
        val spineSkinGetter = propertiesClass.methods.firstOrNull { it.name == "getSpineSkin" && it.parameterCount == 0 }
        assertTrue("MixtapeVisualProperties must expose spineSkin for cassette spine rendering.", spineSkinGetter != null)
        assertEquals("MixtapeVisualProperties.spineSkin should return MixtapeSpineSkin.", skinClass, spineSkinGetter!!.returnType)
    }

    @Test
    fun spineSkinSettingsDefaultToAllEntriesAndSafelyRecoverFromMissingOrUnknownSavedNames() {
        assertEquals(
            "Default spine skin settings should include every enum entry so new installs automatically see all previews and assignment choices.",
            MixtapeSpineSkin.entries.toSet(),
            MixtapeSpineSkinSettings().enabledSpineSkins,
        )
        assertEquals(
            "Missing saved settings should migrate to all available spine skins instead of an empty set.",
            MixtapeSpineSkin.entries.toSet(),
            MixtapeSpineSkinSettings.fromNames(null).enabledSpineSkins,
        )
        assertEquals(
            "Unknown saved spine skin names should be ignored and normalize back to all available skins.",
            MixtapeSpineSkin.entries.toSet(),
            MixtapeSpineSkinSettings.fromNames(setOf("DOES_NOT_EXIST", "REFERENCE_TEXT_SHOULD_NOT_EXIST")).enabledSpineSkins,
        )
        assertEquals(
            "An explicit saved subset should stay a subset so old user choices are not corrupted during enum expansion.",
            setOf(MixtapeSpineSkin.CreamRed),
            MixtapeSpineSkinSettings.fromNames(setOf("CreamRed")).enabledSpineSkins,
        )
    }

    @Test
    fun sharedPreferencesStorePersistsSpineSkinNamesWithCreamRedFallbackForUnknownPerMixtapeValues() {
        val source = visualPropertiesSource()

        assertTrue(
            "SharedPreferencesMixtapeVisualPropertiesStore should persist spineSkin with a stable 'spine_skin' string key.",
            source.contains("spineSkin") &&
                source.contains("spine_skin") &&
                source.contains("putString") &&
                source.contains("properties.spineSkin.name"),
        )
        assertTrue(
            "Missing or unknown saved per-mixtape spine skin names must fall back to CreamRed instead of invalidating otherwise valid visual properties.",
            source.contains("MixtapeSpineSkin.CreamRed") &&
                source.contains("MixtapeSpineSkin.entries.firstOrNull"),
        )
    }

    private fun visualPropertiesSource(): String = sourceFile(
        "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt",
        "src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt",
    )

    private fun sourceFile(vararg candidates: String): String {
        val path = candidates.map(::File).firstOrNull { it.exists() }
        assertTrue("Expected to find one of ${candidates.toList()} from ${System.getProperty("user.dir")}", path != null)
        return path!!.readText()
    }

    private fun requiredClass(name: String, message: String): Class<*> = try {
        Class.forName(name)
    } catch (_: ClassNotFoundException) {
        fail(message)
        throw AssertionError(message)
    }

    private companion object {
        val expectedSpineSkinNames = listOf(
            "CreamRed",
            "BlushBlue",
            "SkyGreen",
            "MintAmber",
            "LemonNavy",
            "PinkZineBorder",
            "VioletLibraryStripe",
            "MintCollageTab",
            "AmberIndexBlock",
            "PowderBlueMarker",
            "CoralStickerRail",
            "EmeraldNotebookLines",
            "SepiaNewsprintFrame",
            "BlackPhotoNegative",
            "RainbowCutout",
        )
    }
}
