package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingHandwritingStabilityContractTest {
    @Test
    fun nowPlayingStartIndexComesFromPersistedVisualProperties() {
        val functionSource = extractFunction(mixtapeAppSource(), "nowPlayingJitterStartIndex")

        assertTrue(functionSource.contains("currentMixtapeJitterStartIndex"))
        listOf("positionMs", "durationMs", "isPlaying", "canGoPrevious", "canGoNext").forEach { mutableState ->
            assertFalse(
                "The base perturbation index must not depend on mutable playback state '$mutableState'.",
                Regex("\\b$mutableState\\b").containsMatchIn(functionSource),
            )
        }
    }

    @Test
    fun uiStateCarriesOneIntegerStartIndexPerMixtape() {
        val source = viewModelSource()

        assertTrue(
            "MixtapeUiState should expose the persisted perturbation-table start index.",
            Regex("val\\s+currentMixtapeJitterStartIndex:\\s*Int\\b").containsMatchIn(source),
        )
    }

    @Test
    fun rendererCallSitesUseIndicesInsteadOfPerTextSeedMaterial() {
        val source = mixtapeAppSource()

        assertFalse(source.contains("seedMaterial ="))
        assertTrue(source.contains("startIndex = jitterStartIndex"))
        assertTrue(source.contains("startIndex = mixtapeJitterStartIndex"))
        assertTrue(source.contains("handwritingTrackStartIndex(mixtapeJitterStartIndex, index)"))
    }

    private fun extractFunction(source: String, functionName: String): String {
        val start = source.indexOf("fun $functionName")
        assertTrue("Expected to find function $functionName in MixtapeApp.kt", start >= 0)
        val openBrace = source.indexOf('{', start)
        assertTrue("Expected $functionName to have a body.", openBrace >= 0)
        var depth = 0
        for (index in openBrace until source.length) {
            when (source[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return source.substring(start, index + 1)
                }
            }
        }
        throw AssertionError("Could not extract function $functionName.")
    }

    private fun mixtapeAppSource(): String = requiredFile(
        "app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt",
        "src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt",
    ).readText()

    private fun viewModelSource(): String = requiredFile(
        "app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt",
        "src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt",
    ).readText()

    private fun requiredFile(vararg paths: String): File {
        val file = paths.map(::File).firstOrNull { it.exists() }
        assertTrue("Expected one of ${paths.joinToString()} from ${System.getProperty("user.dir")}", file != null)
        return file!!
    }
}
