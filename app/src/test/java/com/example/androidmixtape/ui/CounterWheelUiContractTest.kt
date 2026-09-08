package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

/** Card 4835 red guards, not a substitute for the deterministic frame tests in its plan. */
class CounterWheelUiContractTest {
    private val source = listOf(
        File("src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
        File("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt"),
    ).first { it.exists() }.readText()

    @Test fun counterMustNotReplaceTheEntireNumeralStringAbruptly() {
        val bay = source.substringAfter("private fun DeckCassetteBay(")
            .substringBefore("private fun DeckLevelMeter(")
        assertFalse(
            "Replace the static counter Text with fixed clipped downward digit wheels; retain the value source.",
            bay.contains("text = counterValue.coerceIn(0, 999).toString().padStart(3, '0')"),
        )
    }

    @Test fun toggleMustReleaseSpaceForTheSpine() {
        val toggle = source.substringAfter("private fun DeckViewToggle(")
            .substringBefore("private fun CurrentTrackPreview(")
        assertFalse(
            "Shrink the 64dp toggle allocation to 48dp and retain an accessible click target.",
            toggle.contains(".size(64.dp)"),
        )
    }
}
