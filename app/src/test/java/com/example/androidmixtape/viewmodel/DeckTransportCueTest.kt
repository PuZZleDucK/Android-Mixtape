package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.playback.FakePlayerEngine
import com.example.androidmixtape.playback.MixtapeController
import com.example.androidmixtape.playback.TransportCueDirection
import com.example.androidmixtape.playback.TransportCuePlayer
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeckTransportCueTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test fun forwardButtonPlaysCueBeforeStartingNextTrack() = runTest {
        val (vm, engine, cue) = fixture()
        vm.nextWithCue()
        runCurrent()
        assertEquals(listOf(TransportCueDirection.FAST_FORWARD to 5_000L), cue.calls)
        assertEquals(TransportCueDirection.FAST_FORWARD, vm.uiState.value.transportCueDirection)
        assertFalse(vm.uiState.value.isPlaying)
        assertEquals(0, engine.playedIndex)
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(0, engine.playedIndex)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, engine.playedIndex)
        assertTrue(vm.uiState.value.isPlaying)
    }

    @Test fun rewindButtonPlaysCueBeforeStartingPreviousTrack() = runTest {
        val (vm, engine, cue) = fixture(1)
        vm.previousWithCue()
        runCurrent()
        assertEquals(listOf(TransportCueDirection.REWIND to 5_000L), cue.calls)
        assertEquals(TransportCueDirection.REWIND, vm.uiState.value.transportCueDirection)
        assertFalse(vm.uiState.value.isPlaying)
        assertEquals(1, engine.playedIndex)
        advanceUntilIdle()
        assertEquals(0, engine.playedIndex)
        assertTrue(vm.uiState.value.isPlaying)
    }

    @Test fun boundariesAndEmptyQueueDoNotPlayEffectsOrRestartTracks() = runTest {
        val (vm, engine, cue) = fixture()
        vm.previousWithCue()
        vm.next()
        vm.next()
        vm.nextWithCue()
        advanceUntilIdle()
        assertTrue(cue.calls.isEmpty())
        assertEquals(2, engine.playedIndex)
        val (empty, _, emptyCue) = fixture(count = 0)
        empty.nextWithCue()
        empty.previousWithCue()
        advanceUntilIdle()
        assertTrue(emptyCue.calls.isEmpty())
    }

    @Test fun stopAndEjectCancelButtonCuesWithoutStartingTheirTargets() = runTest {
        for (eject in listOf(false, true)) {
            val (vm, engine, cue) = fixture()
            vm.nextWithCue()
            runCurrent()
            if (eject) vm.eject() else vm.stop()
            advanceUntilIdle()
            assertNull(cue.active)
            assertEquals(TransportCueDirection.NONE, vm.uiState.value.transportCueDirection)
            assertEquals(0, engine.playedIndex)
            assertFalse(vm.uiState.value.isPlaying)
        }
    }

    @Test fun replacingForwardWithRewindKeepsNewCueActiveAndCancelsOldTarget() = runTest {
        val (vm, engine, cue) = fixture(1)
        vm.nextWithCue()
        runCurrent()
        advanceTimeBy(1_000)
        vm.previousWithCue()
        runCurrent()
        assertEquals(TransportCueDirection.REWIND, cue.active)
        advanceUntilIdle()
        assertEquals(0, engine.playedIndex)
        assertEquals(listOf(TransportCueDirection.FAST_FORWARD, TransportCueDirection.REWIND), cue.calls.map { it.first })
    }

    @Test fun playAndSelectingAnotherTapeCancelWindingAndItsPendingTarget() = runTest {
        for (selectTape in listOf(false, true)) {
            val (vm, engine, _) = fixture()
            vm.nextWithCue(); runCurrent()
            assertEquals(TransportCueDirection.FAST_FORWARD, vm.uiState.value.transportCueDirection)
            if (selectTape) vm.selectMixTapeGroup(0) else vm.togglePlayPause()
            assertEquals(TransportCueDirection.NONE, vm.uiState.value.transportCueDirection)
            advanceUntilIdle()
            assertEquals(0, engine.playedIndex)
            assertTrue(vm.uiState.value.isPlaying)
        }
    }

    @Test fun completedCueRestoresNormalPlayMotion() = runTest {
        val (vm, _, _) = fixture()
        vm.nextWithCue(); runCurrent(); advanceUntilIdle()
        assertEquals(TransportCueDirection.NONE, vm.uiState.value.transportCueDirection)
        assertTrue(vm.uiState.value.isPlaying)
    }

    @Test fun automaticTrackTransitionsRemainImmediateAndSilent() = runTest {
        val (vm, engine, cue) = fixture()
        engine.simulateAutomaticTransitionTo(1)
        assertEquals(1, vm.uiState.value.currentIndex)
        assertTrue(vm.uiState.value.isPlaying)
        assertTrue(cue.calls.isEmpty())
    }

    @Test fun activityWiresBothDeckButtonsToCueAwareActions() {
        val path = "src/modern/java/com/example/androidmixtape/MainActivity.kt"
        val source = listOf(File("app/$path"), File(path)).first { it.exists() }.readText()
        assertTrue(source.contains("onPrevious = viewModel::previousWithCue"))
        assertTrue(source.contains("onNext = viewModel::nextWithCue"))
    }

    private fun TestScope.fixture(index: Int = 0, count: Int = 3): Triple<MixtapeViewModel, FakePlayerEngine, RecordingCue> {
        val tracks = (1..count).map { Track(it.toLong(), "Song $it", "Artist", 60_000, "content://song/$it") }
        val engine = FakePlayerEngine()
        val cue = RecordingCue()
        val vm = MixtapeViewModel(
            repository = object : AudioRepository { override suspend fun loadTracks() = tracks },
            controller = MixtapeController(engine),
            transportCuePlayer = cue,
        )
        vm.onPermissionResult(true)
        runCurrent()
        if (count > 0) vm.selectMixTapeGroup(0)
        repeat(index) { vm.next() }
        return Triple(vm, engine, cue)
    }

    private class RecordingCue : TransportCuePlayer {
        val calls = mutableListOf<Pair<TransportCueDirection, Long>>()
        var active: TransportCueDirection? = null
        override suspend fun play(direction: TransportCueDirection, durationMs: Long) {
            calls += direction to durationMs
            active = direction
            delay(durationMs)
            active = null
        }
        override fun cancel() { active = null }
        override fun release() = cancel()
    }
}
