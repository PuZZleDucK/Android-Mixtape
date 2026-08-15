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
        val source = source("app/src/modern/java/com/example/androidmixtape/ui/MixtapeApp.kt")
        val deck = source.substringAfter("private fun DeckCassetteBay(").substringBefore("@Composable\nprivate fun DemoDeckButton(")

        assertTrue(source.contains("AudioLevelMonitor.levels.collectAsState()"))
        assertTrue(deck.contains("leftAudioLevel: Float") && deck.contains("rightAudioLevel: Float"))
        assertTrue(deck.contains("DeckLevelMeter(\"L\", leftAudioLevel"))
        assertTrue(deck.contains("DeckLevelMeter(\"R\", rightAudioLevel"))
        assertTrue(deck.contains("fillMaxHeight(displayedLevel)"))
        assertFalse("Remove the old fixed decorative meter heights.", deck.contains("0.56f") || deck.contains("0.72f") || deck.contains("0.08f"))
    }

    private fun source(path: String): String {
        val candidates = listOf(File(path), File("..", path), File(path.removePrefix("app/")))
        return requireNotNull(candidates.firstOrNull { it.exists() }) { "Source not found: $path" }.readText()
    }
}
