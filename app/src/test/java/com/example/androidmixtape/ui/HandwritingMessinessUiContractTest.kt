package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwritingMessinessUiContractTest {
    private val appSource = projectFile("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt").readText()
    private val rendererSource = projectFile("app/src/modern/java/com/example/androidmixtape/ui/JitteredHandwritingTextRenderer.kt").readText()

    @Test
    fun settingsPageOffersHighLowAndOffMessinessLevels() {
        assertTrue(appSource.contains("Text(\"Messiness\""))
        listOf("High", "Low", "Off").forEach { level ->
            assertTrue(appSource.contains("label = \"$level\""))
            assertTrue(appSource.contains("HandwritingMessiness.$level"))
        }
        assertTrue(appSource.contains("onHandwritingMessinessChange"))
    }

    @Test
    fun selectedMessinessControlsEveryHandwrittenRenderer() {
        assertTrue(appSource.contains("LocalHandwritingMessiness provides state.mixtapeSettings.handwritingMessiness"))
        assertTrue(rendererSource.contains("messiness.strengthMultiplier"))
        assertTrue(rendererSource.contains("messiness == HandwritingMessiness.Off"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File(path.removePrefix("app/")))
        return candidates.firstOrNull(File::exists)
            ?: throw AssertionError("Expected $path from ${System.getProperty("user.dir")}")
    }
}
