package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Red integration guards for card 4918. Follow with playback and Compose behavior tests. */
class PlayerCounterLiveContractTest {
    private fun source(path: String): String = listOf(File("src/$path"), File("app/src/$path"))
        .first { it.exists() }.readText()

    @Test fun playingPositionMustBeSampledBetweenMedia3Events() {
        val engine = source("modern/java/com/example/androidmixtape/playback/MediaControllerPlayerEngine.kt")
        val ui = source("modern/java/com/example/androidmixtape/ui/MixtapeApp.kt")
        assertTrue(
            "Media3 onEvents does not tick every second. Poll currentPosition while playing, " +
                "publish it through the playback state, and stop polling on pause/release; " +
                "alternatively use a composition-scoped clock with the same guards.",
            (engine.contains("currentPosition") &&
                (engine.contains("delay(") || engine.contains("postDelayed("))) ||
                (ui.contains("positionMs") && ui.contains("delay(") && ui.contains("isPlaying")),
        )
    }

    @Test fun deckThemesMustSelectMechanicalAndSegmentedCounters() {
        val bay = source("modern/java/com/example/androidmixtape/ui/MixtapeApp.kt")
            .substringAfter("private fun DeckCassetteBay(")
            .substringBefore("private fun DeckLevelMeter(")
        val counterUi = source("modern/java/com/example/androidmixtape/ui/CounterWheels.kt")
        assertTrue(
            "Pass the selected deck theme to counter rendering; at least one theme needs " +
                "a mechanical downward roll and another a segmented digital display.",
            bay.contains("deckTheme") && bay.contains("CounterWheels(") &&
                bay.substringAfter("CounterWheels(").substringBefore(")").contains("deckTheme") &&
                counterUi.contains("segment", ignoreCase = true),
        )
    }
}
