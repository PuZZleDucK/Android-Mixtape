package com.example.androidmixtape.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckAudioLevelMeterContractTest {
    @Test
    fun playbackServiceMetersDecodedStereoPcmWithoutMicrophoneCapture() {
        val monitor = source("app/src/modern/java/com/example/androidmixtape/playback/AudioLevelMonitor.kt")
        val service = source("app/src/modern/java/com/example/androidmixtape/playback/MixtapeMediaLibraryService.kt")

        assertTrue(monitor.contains("class MeteringAudioProcessor : BaseAudioProcessor"))
        assertTrue(monitor.contains("leftSquares") && monitor.contains("rightSquares"))
        assertTrue(monitor.contains("channelCount > 1"))
        assertTrue(monitor.contains("DefaultAudioSink.Builder(context)") && monitor.contains("setAudioProcessors"))
        assertTrue(service.contains("MeteringRenderersFactory") && service.contains("MeteringAudioProcessor"))
        assertFalse("Audio meters should inspect decoded playback PCM, not request microphone capture.", monitor.contains("Visualizer(") || monitor.contains("AudioRecord("))
    }

    @Test
    fun deckMetersConsumeIndependentLiveLeftAndRightLevels() {
        val ui = source("app/src/modern/java/com/example/androidmixtape/ui/DemoPlayerScreen.kt")
        val deck = source("app/src/modern/java/com/example/androidmixtape/ui/DemoDeck.kt")
        assertTrue(ui.contains("AudioLevelMonitor.levels.collectAsState()"))
        assertTrue(ui.contains("leftLevel=audio.left,rightLevel=audio.right"))
        assertTrue(deck.contains("if(playing)leftLevel else 0f") && deck.contains("if(playing)rightLevel else 0f"))
        assertTrue(deck.contains("0xFF62BF70") && deck.contains("0xFFEAAF48") && deck.contains("0xFFE45B48"))
        assertTrue(deck.contains("extent.height*(9-i)/10"))
        assertFalse(deck.contains("Random") || deck.contains("sin("))
    }

    private fun source(path: String): String {
        val candidates = listOf(File(path), File("..", path), File(path.removePrefix("app/")))
        return requireNotNull(candidates.firstOrNull { it.exists() }) { "Source not found: $path" }.readText()
    }
}
