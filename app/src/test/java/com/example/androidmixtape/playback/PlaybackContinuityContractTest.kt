package com.example.androidmixtape.playback

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackContinuityContractTest {
    private val projectDir = findProjectDir(File(requireNotNull(System.getProperty("user.dir"))))

    @Test
    fun manifestAllowsPartialWakeLockForScreenOffPlayback() {
        val manifest = File(projectDir, "app/src/main/AndroidManifest.xml").readText()

        assertTrue(
            "Screen-off playback needs android.permission.WAKE_LOCK so player wake modes can keep audio running while the display is off.",
            manifest.contains("android.permission.WAKE_LOCK"),
        )
    }

    @Test
    fun modernExoPlayerUsesLocalWakeMode() {
        val source = File(projectDir, "app/src/modern/java/com/example/androidmixtape/playback/ExoPlayerEngine.kt").readText()

        assertTrue(
            "Modern ExoPlayer playback should call setWakeMode(C.WAKE_MODE_LOCAL) or the equivalent local wake mode before playback.",
            source.contains("setWakeMode") && source.contains("WAKE_MODE_LOCAL"),
        )
    }

    @Test
    fun playerEngineExposesCurrentIndexChangeNotification() {
        val source = File(projectDir, "app/src/main/java/com/example/androidmixtape/playback/PlayerEngine.kt").readText()

        assertTrue(
            "PlayerEngine needs a callback/registration API such as onCurrentIndexChanged so automatic playback transitions can update MixtapeController state.",
            source.contains("CurrentIndexChanged") || source.contains("currentIndexChanged"),
        )
    }

    @Test
    fun modernExoPlayerReportsAutomaticMediaItemTransitions() {
        val source = File(projectDir, "app/src/modern/java/com/example/androidmixtape/playback/ExoPlayerEngine.kt").readText()

        assertTrue(
            "Modern ExoPlayerEngine should install a Player.Listener that handles onMediaItemTransition and reports player.currentMediaItemIndex to the current-index callback.",
            source.contains("Player.Listener") &&
                source.contains("onMediaItemTransition") &&
                source.contains("currentMediaItemIndex") &&
                (source.contains("MEDIA_ITEM_TRANSITION") || source.contains("reason")),
        )
    }

    @Test
    fun playerEngineReplacementContractIsNotSilentDefaultNoOp() {
        val source = File(projectDir, "app/src/main/java/com/example/androidmixtape/playback/PlayerEngine.kt").readText()
        val emptyDefaultBody = Regex("""fun\s+replacePlaylistPreservingPlayback\([^)]*\)\s*\{\s*}""").containsMatchIn(source)

        assertTrue(
            "replacePlaylistPreservingPlayback must be a real PlayerEngine contract, not a silent empty default that production engines can inherit.",
            !emptyDefaultBody,
        )
    }

    @Test
    fun productionEnginesOverridePlaylistReplacementPreservation() {
        val engineSources = listOf(
            "ExoPlayerEngine" to "app/src/modern/java/com/example/androidmixtape/playback/ExoPlayerEngine.kt",
            "MediaControllerPlayerEngine" to "app/src/modern/java/com/example/androidmixtape/playback/MediaControllerPlayerEngine.kt",
        )

        engineSources.forEach { (engineName, path) ->
            val source = File(projectDir, path).readText()
            assertTrue(
                "$engineName must override replacePlaylistPreservingPlayback so deleting a non-current device track updates the real engine queue, not only UI/controller state.",
                source.contains("override fun replacePlaylistPreservingPlayback"),
            )
        }
    }

    @Test
    fun modernExoPlayerReplacementPreservesQueuePositionAndPlayingState() {
        val source = File(projectDir, "app/src/modern/java/com/example/androidmixtape/playback/ExoPlayerEngine.kt").readText()
        val body = methodBody(source, "replacePlaylistPreservingPlayback")

        assertTrue(
            "ExoPlayerEngine preservation should replace Media3 media items at the remapped currentIndex while carrying forward currentPosition/playWhenReady instead of restarting at item zero.",
            body.contains("setMediaItems") &&
                body.contains("currentIndex") &&
                body.contains("currentPosition") &&
                body.contains("playWhenReady"),
        )
    }

    @Test
    fun mediaControllerReplacementPreservesQueuePositionAndPlayingState() {
        val source = File(projectDir, "app/src/modern/java/com/example/androidmixtape/playback/MediaControllerPlayerEngine.kt").readText()
        val body = methodBody(source, "replacePlaylistPreservingPlayback")

        assertTrue(
            "MediaControllerPlayerEngine preservation should replace the connected controller playlist at the remapped currentIndex while carrying forward currentPosition/playWhenReady; pending actions must not replay a stale loadPlaylist later.",
            body.contains("setMediaItems") &&
                body.contains("currentIndex") &&
                body.contains("currentPosition") &&
                body.contains("playWhenReady"),
        )
    }

    @Test
    fun playerEngineExposesExternalPlaybackSnapshotForCarInitiatedQueueChanges() {
        val source = File(projectDir, "app/src/main/java/com/example/androidmixtape/playback/PlayerEngine.kt").readText()

        assertTrue(
            "PlayerEngine must expose an external playback snapshot callback richer than currentIndex-only changes so car-started queues can update the phone UI. Expected a snapshot model/listener carrying queue tracks or media IDs, current index/media ID, isPlaying, positionMs, and durationMs.",
            source.contains("ExternalPlaybackSnapshot") &&
                Regex("setOnExternal.*Playback.*(Snapshot|State|Changed|Listener)|setOnPlayback.*Snapshot").containsMatchIn(source) &&
                source.contains("List<Track>") &&
                source.contains("currentIndex") &&
                (source.contains("currentMediaId") || source.contains("mediaId")) &&
                source.contains("isPlaying") &&
                source.contains("positionMs") &&
                source.contains("durationMs"),
        )
    }

    @Test
    fun mediaControllerPlayerEngineObservesExternalTimelineMediaItemAndPlayingEvents() {
        val source = File(projectDir, "app/src/modern/java/com/example/androidmixtape/playback/MediaControllerPlayerEngine.kt").readText()

        assertTrue(
            "MediaControllerPlayerEngine must publish phone-visible external playback snapshots from Media3 onEvents/timeline/media-item/isPlaying/playback-state changes, not only onMediaItemTransition(currentMediaItemIndex).",
            source.contains("onEvents") &&
                source.contains("EVENT_TIMELINE_CHANGED") &&
                source.contains("EVENT_MEDIA_ITEM_TRANSITION") &&
                source.contains("EVENT_IS_PLAYING_CHANGED") &&
                source.contains("EVENT_PLAYBACK_STATE_CHANGED") &&
                (source.contains("mediaItemCount") || source.contains("currentTimeline")) &&
                source.contains("isPlaying") &&
                source.contains("currentPosition") &&
                source.contains("duration"),
        )
    }

    @Test
    fun mixtapeControllerHandlesExternalPlaybackSnapshotByReplacingQueue() {
        val source = File(projectDir, "app/src/main/java/com/example/androidmixtape/playback/MixtapeController.kt").readText()

        assertTrue(
            "MixtapeController must handle external playback snapshots by replacing state.tracks/currentIndex/currentTrack/isPlaying/position/duration together; mapping an external index into stale state.tracks is the Review-blocked bug.",
            source.contains("ExternalPlaybackSnapshot") &&
                Regex("handleExternal.*Playback.*Snapshot|applyExternal.*Playback.*Snapshot").containsMatchIn(source) &&
                source.contains("tracks = snapshot.tracks") &&
                source.contains("currentIndex = snapshot.currentIndex") &&
                source.contains("isPlaying = snapshot.isPlaying") &&
                source.contains("positionMs = snapshot.positionMs") &&
                source.contains("durationMs = snapshot.durationMs"),
        )
    }

    private fun methodBody(source: String, methodName: String): String {
        val methodStart = source.indexOf("override fun $methodName")
        if (methodStart < 0) return ""
        val bodyStart = source.indexOf('{', methodStart)
        if (bodyStart < 0) return ""

        var depth = 0
        for (index in bodyStart until source.length) {
            when (source[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return source.substring(bodyStart + 1, index)
                }
            }
        }
        return source.substring(bodyStart + 1)
    }

    private fun findProjectDir(start: File): File {
        var current: File? = start
        while (current != null) {
            if (File(current, "settings.gradle.kts").isFile) return current
            current = current.parentFile
        }
        error("Unable to locate android-mixtape project root from ${start.absolutePath}")
    }
}
