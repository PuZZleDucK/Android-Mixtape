package com.example.androidmixtape.viewmodel

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MixtapeVisualPropertiesContractTest {
    @Test
    fun visualPropertiesModelCombinesHandwritingFontAndStableJitterSeed() {
        val propertiesClass = requiredClass(
            "com.example.androidmixtape.viewmodel.MixtapeVisualProperties",
            "Expected a MixtapeVisualProperties model that is saved per stable mixtape/song identity instead of recalculating visual jitter from playback state.",
        )
        val getters = propertiesClass.methods.filter { it.parameterCount == 0 }

        assertTrue(
            "MixtapeVisualProperties must expose the selected handwriting font as part of the persisted visual identity.",
            getters.any { it.returnType.name == "com.example.androidmixtape.viewmodel.MixtapeHandwritingFont" },
        )
        assertTrue(
            "MixtapeVisualProperties must expose one persisted perturbation-table start index that survives recomposition, navigation, playback changes, and app restart.",
            getters.any { method ->
                method.returnType == Int::class.javaPrimitiveType &&
                    method.name.contains(Regex("Jitter|StartIndex", RegexOption.IGNORE_CASE))
            },
        )
    }

    @Test
    fun visualPropertiesCarryPersistedMixtapeSymbolColor() {
        val propertiesClass = requiredClass(
            "com.example.androidmixtape.viewmodel.MixtapeVisualProperties",
            "Expected a MixtapeVisualProperties model for persisted per-mixtape visual identity.",
        )
        val getters = propertiesClass.methods.filter { it.parameterCount == 0 }

        assertTrue(
            "MixtapeVisualProperties must expose a persisted symbolColor/symbolInk property so tapping the Now Playing symbol changes saved color per tape.",
            getters.any { method ->
                method.name in setOf("getSymbolColor", "getSymbolInk") &&
                    method.returnType.name.startsWith("com.example.androidmixtape.viewmodel.MixtapeSymbol")
            },
        )

        val source = visualPropertiesSource()
        assertTrue(
            "SharedPreferencesMixtapeVisualPropertiesStore must save/load the symbolColor/symbolInk using a stable string key for old-install defaults and restart persistence.",
            (source.contains("symbolColor") || source.contains("symbolInk")) &&
                (source.contains("symbol_color") || source.contains("symbol_ink")) &&
                source.contains("putString"),
        )
    }

    @Test
    fun mixTapeGroupCarriesVisualPropertiesBesideTracksAndName() {
        val groupClass = requiredClass(
            "com.example.androidmixtape.viewmodel.MixTapeGroup",
            "Expected MixTapeGroup to remain the ViewModel-owned source for rendered mixtape metadata.",
        )
        val getterNames = groupClass.methods.filter { it.parameterCount == 0 }.map { it.name }

        assertTrue(
            "MixTapeGroup should carry persisted visual properties, or at least a persisted handwriting jitter key, so cassette spines can render the same text with the same perturbation.",
            getterNames.any { it == "getVisualProperties" || it == "getHandwritingJitterKey" || it == "getJitterSeed" },
        )
    }

    @Test
    fun viewModelAcceptsAVisualPropertiesStoreForRestartPersistence() {
        val viewModelClass = requiredClass(
            "com.example.androidmixtape.viewmodel.MixtapeViewModel",
            "Expected MixtapeViewModel to assign and expose mixtape visual properties.",
        )
        val constructorParameterTypeNames = viewModelClass.constructors.flatMap { constructor ->
            constructor.parameterTypes.map { it.name }
        }

        assertTrue(
            "MixtapeViewModel should accept a MixtapeVisualPropertiesStore (with in-memory and SharedPreferences implementations) so font/jitter properties survive app restart rather than living only in transient maps.",
            constructorParameterTypeNames.any { it.endsWith("MixtapeVisualPropertiesStore") },
        )
    }

    private fun visualPropertiesSource(): String {
        val path = listOf(
            File("app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt"),
            File("src/main/java/com/example/androidmixtape/viewmodel/MixtapeVisualProperties.kt"),
        ).firstOrNull { it.exists() }
        assertTrue("Expected to find MixtapeVisualProperties.kt from ${System.getProperty("user.dir")}", path != null)
        return path!!.readText()
    }

    private fun requiredClass(name: String, message: String): Class<*> = try {
        Class.forName(name)
    } catch (_: ClassNotFoundException) {
        fail(message)
        throw AssertionError(message)
    }
}
