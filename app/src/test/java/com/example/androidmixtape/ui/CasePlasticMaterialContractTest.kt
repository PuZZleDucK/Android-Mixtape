package com.example.androidmixtape.ui

import java.io.File
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
        assertEquals(5, Regex("drawCasePlastic\\(").findAll(source()).count())
        assertFalse(source().contains("CASE_PLASTIC_TINT_STRENGTH"))
        val spine = source().substringAfter("private fun CassetteSpineRow(")
            .substringBefore("private fun LegacyCassetteSpineRow(")
        assertFalse(spine.contains("drawRoundRect(Color(0xFF15191F)"))
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
