package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

/** Integration guards, supplemented by CasePlasticCompositingTest on the emulator. */
class CasePlasticMaterialContractTest {
    private fun source(): String = listOf(
        File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
    ).first { it.exists() }.readText()

    @Test fun allFiveLivePathsUseTheSharedMaterialWithoutLocalTintMultipliers() {
        val native = listOf(File("src/modern/java/com/example/androidmixtape/ui/DemoPackaging.kt"), File("app/src/modern/java/com/example/androidmixtape/ui/DemoPackaging.kt")).first { it.exists() }.readText()
        assertTrue(native.contains("internal fun DemoCaseSurface("))
        assertTrue(native.contains("demoCaseTransmission(tint,opacity)"))
        assertTrue(native.contains("ColorFilter.colorMatrix(matrix)"))
        assertTrue(native.contains("drawContext.canvas.saveLayer") && native.contains("drawContent();drawContext.canvas.restore()"))
        assertEquals(2, Regex("DemoCaseSurface\\(themes.case\\(properties.caseTheme\\)").findAll(native).count())
        val previews = source().substringAfter("private fun CaseComponentPreview(").substringBefore("private fun HelpScreen(")
        assertTrue(previews.contains("DemoCaseSurface(style") && previews.contains("DemoCaseSurface(rememberDemoThemes().case"))
    }

    @Test fun clearCasePreviewMustNotHaveAnOpaqueDarkPlasticBacking() {
        val preview = source().substringAfter("private fun CaseComponentPreview(")
            .substringBefore("private fun SleeveComponentPreview(")
        assertFalse("Leave the area outside the inset paper transparent to the parent.",
            preview.contains("drawRoundRect(Color(0xFF171A20), size = size"))
    }

    @Test fun casePlasticMustNotUseHeavyIndependentCardBorders() {
        assertFalse("Use the shared material rim, not an extra 2 or 3 dp Card outline.",
            Regex("""BorderStroke\([23]\.dp, casePlastic\.edge\)""").containsMatchIn(source()))
    }

    @Test fun previewMustNotBypassRuntimeTintStrength() {
        val preview = source().substringAfter("private fun CaseComponentPreview(")
            .substringBefore("private fun SleeveComponentPreview(")
        assertFalse("Preview and runtime must use the same material compositing policy.",
            preview.contains("drawRoundRect(plastic.tint, size = size"))
    }
}
