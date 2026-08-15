package com.example.androidmixtape.playback

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackJumpCueSourceContractTest {
    @Test
    fun jumpToTrackWithCueStopsCurrentTrackThenWaitsFiveSecondsThenStartsTarget() {
        val viewModelSource = readFirstExisting("app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt")
        val jumpBody = functionBody(viewModelSource, "fun jumpToTrackWithCue(")
        val allSource = playbackSource() + "\n" + viewModelSource

        assertTrue(
            "A shared TransportCueDirection contract should make fast-forward, rewind, and same-row/no-cue behavior explicit.",
            allSource.contains("TransportCueDirection") &&
                Regex("FAST_FORWARD|FastForward|FastForwarding").containsMatchIn(allSource) &&
                Regex("REWIND|Rewind|Rewinding").containsMatchIn(allSource) &&
                Regex("NONE|None|NoCue|SameTrack").containsMatchIn(allSource),
        )
        assertTrue(
            "The jump path should use an injectable TransportCuePlayer (or equivalent shared cue abstraction) instead of coupling shared code to Media3/ExoPlayer.",
            allSource.contains("TransportCuePlayer") && !allSource.contains("androidx.media3") && !allSource.contains("ExoPlayer"),
        )
        assertTrue(
            "Double-click jumps must stop the current track before starting any cue.",
            ordered(jumpBody, listOf("currentIndex", "controller.stop()")) ||
                ordered(jumpBody, listOf("currentIndex", "stop()")) ||
                ordered(jumpBody, listOf("currentIndex", "pause()", "seekTo(0")),
        )
        assertTrue(
            "The cue duration must be exactly 5 seconds before target playback begins.",
            Regex("5_000L|5000L|5_000|5000|seconds\\(5").containsMatchIn(jumpBody),
        )
        assertTrue(
            "Target playback must happen only after the transport cue finishes.",
            orderedAny(
                jumpBody,
                first = listOf("play(TransportCueDirection", "cuePlayer.play", "transportCuePlayer.play", ".playCue", "delay("),
                second = listOf("controller.select(targetIndex)", "select(targetIndex)", "playIndex(targetIndex)"),
            ),
        )
    }

    @Test
    fun jumpDirectionIsBasedOnTargetRelativeToCurrentIndexIncludingSameRowBehavior() {
        val viewModelSource = readFirstExisting("app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt")
        val jumpBody = functionBody(viewModelSource, "fun jumpToTrackWithCue(")

        assertTrue(
            "Jumping to a later row should choose the fast-forward cue from targetIndex > currentIndex.",
            Regex("targetIndex\\s*>\\s*currentIndex[\\s\\S]{0,240}(FAST_FORWARD|FastForward)").containsMatchIn(jumpBody) ||
                Regex("currentIndex\\s*<\\s*targetIndex[\\s\\S]{0,240}(FAST_FORWARD|FastForward)").containsMatchIn(jumpBody),
        )
        assertTrue(
            "Jumping to an earlier row should choose the rewind cue from targetIndex < currentIndex.",
            Regex("targetIndex\\s*<\\s*currentIndex[\\s\\S]{0,240}(REWIND|Rewind)").containsMatchIn(jumpBody) ||
                Regex("currentIndex\\s*>\\s*targetIndex[\\s\\S]{0,240}(REWIND|Rewind)").containsMatchIn(jumpBody),
        )
        assertTrue(
            "Same-row double-click behavior should be deterministic and avoid a fast-forward/rewind cue.",
            Regex("targetIndex\\s*==\\s*currentIndex|currentIndex\\s*==\\s*targetIndex").containsMatchIn(jumpBody) &&
                Regex("NONE|None|NoCue|SameTrack").containsMatchIn(jumpBody),
        )
    }

    @Test
    fun pendingCueJumpIsCancellableFromNewDoubleClickStopEjectAndViewModelClear() {
        val viewModelSource = readFirstExisting("app/src/main/java/com/example/androidmixtape/viewmodel/MixtapeViewModel.kt")
        val jumpBody = functionBody(viewModelSource, "fun jumpToTrackWithCue(")
        val stopBody = functionBody(viewModelSource, "fun stop()")
        val ejectBody = functionBody(viewModelSource, "fun eject()")
        val clearedBody = functionBody(viewModelSource, "override fun onCleared()")

        assertTrue(
            "MixtapeViewModel should keep the pending track jump in a Job so repeated double-clicks cancel stale cues.",
            Regex("private\\s+var\\s+\\w*(Jump|Cue)\\w*Job\\s*:\\s*Job\\?").containsMatchIn(viewModelSource),
        )
        assertTrue(
            "Starting a new double-click jump should cancel any pending jump before launching the replacement cue.",
            jumpBody.contains(".cancel()") && jumpBody.contains("viewModelScope.launch"),
        )
        assertTrue("Stop should cancel any pending cue jump before/while stopping playback.", stopBody.contains(".cancel()"))
        assertTrue("Eject should cancel any pending cue jump so it cannot start a stale target.", ejectBody.contains(".cancel()"))
        assertTrue("onCleared should cancel cue playback/jobs as part of ViewModel cleanup.", clearedBody.contains(".cancel()"))
    }

    private fun ordered(body: String, tokens: List<String>): Boolean {
        var cursor = -1
        return tokens.all { token ->
            val next = body.indexOf(token, startIndex = cursor + 1)
            if (next >= 0) {
                cursor = next
                true
            } else {
                false
            }
        }
    }

    private fun orderedAny(body: String, first: List<String>, second: List<String>): Boolean {
        val firstIndex = first.map { body.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: return false
        val secondIndex = second.map { body.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: return false
        return firstIndex < secondIndex
    }

    private fun functionBody(source: String, startMarker: String): String {
        val start = source.indexOf(startMarker)
        assertTrue("Expected to find $startMarker in source", start >= 0)
        val nextPublicFun = source.indexOf("\n    fun ", start + startMarker.length)
        val nextOverrideFun = source.indexOf("\n    override fun ", start + startMarker.length)
        val nextPrivateFun = source.indexOf("\n    private fun ", start + startMarker.length)
        val candidates = listOf(nextPublicFun, nextOverrideFun, nextPrivateFun).filter { it > start }
        val end = candidates.minOrNull() ?: source.length
        return source.substring(start, end)
    }

    private fun playbackSource(): String {
        val playbackDir = File("app/src/main/java/com/example/androidmixtape/playback")
            .takeIf { it.exists() }
            ?: File("../app/src/main/java/com/example/androidmixtape/playback")
        return playbackDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
    }

    private fun readFirstExisting(vararg paths: String): String {
        val candidates = paths.flatMap { path ->
            val withoutAppPrefix = path.removePrefix("app/")
            listOf(File(path), File(withoutAppPrefix), File("../$path"), File("../$withoutAppPrefix"))
        }
        val file = candidates.firstOrNull { it.exists() }
        assertTrue("Expected one of ${candidates.map { it.path }} from ${System.getProperty("user.dir")}", file != null)
        return file!!.readText()
    }
}
