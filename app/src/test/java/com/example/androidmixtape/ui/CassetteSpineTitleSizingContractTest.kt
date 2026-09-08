package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

/** Card 4831 red guards. Pixel/metric tests in the plan remain required. */
class CassetteSpineTitleSizingContractTest {
    private fun spine(): String {
        val file = listOf(
            File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
            File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        ).first { it.exists() }
        return file.readText().substringAfter("private fun CassetteSpineRow(")
            .substringBefore("private fun LegacyCassetteSpineRow(")
    }

    @Test fun titleMustNotUseUnconstrainedFontSizeInsideRatioSizedSpine() {
        assertFalse(
            "Fit the selected font to the available label height before drawing it.",
            Regex("""fontSize\s*=\s*handwritingFont\.cassetteSpineFontSize\(\)\s*,""")
                .containsMatchIn(spine()),
        )
    }

    @Test fun titleMustNotUseFontSpecificDpOffsetInsteadOfMeasuredCenter() {
        assertFalse(
            "Center transformed visible glyph bounds, not a font-specific dp nudge.",
            spine().contains(".offset(y = handwritingFont.cassetteSpineVerticalOffset())"),
        )
    }

    @Test fun labelInsetsMustNotConsumeFixed24DpAtEverySpineHeight() {
        assertFalse(
            "Use the proportional paper bounds and a scale-aware safety inset.",
            Regex("""padding\(horizontal\s*=\s*18.dp,\s*vertical\s*=\s*12.dp\)""")
                .containsMatchIn(spine()),
        )
    }
}
