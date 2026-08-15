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
    fun legacyMediaPlayerUsesPartialWakeLockMode() {
        val source = File(projectDir, "app/src/legacy/java/com/example/androidmixtape/playback/PlatformMediaPlayerEngine.kt").readText()

        assertTrue(
            "Legacy MediaPlayer playback should call setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK) before prepare/start.",
            source.contains("setWakeMode") && source.contains("PARTIAL_WAKE_LOCK"),
        )
    }

    @Test
    fun legacyActivityDoesNotReleasePlaybackDuringConfigurationChange() {
        val source = File(projectDir, "app/src/legacy/java/com/example/androidmixtape/MainActivity.kt").readText()
        val onDestroyBody = source.substringAfter("override fun onDestroy()").substringBefore("\n    }", missingDelimiterValue = source)

        assertTrue(
            "Legacy orientation changes recreate MainActivity; onDestroy must guard controller.release() with !isChangingConfigurations or retain playback outside the Activity.",
            source.contains("isChangingConfigurations") && (!onDestroyBody.contains("controller.release()") || onDestroyBody.contains("!isChangingConfigurations")),
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
            "PlatformMediaPlayerEngine" to "app/src/legacy/java/com/example/androidmixtape/playback/PlatformMediaPlayerEngine.kt",
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
    fun legacyReplacementUpdatesBackingQueueWithoutReleasingCurrentPlayer() {
        val source = File(projectDir, "app/src/legacy/java/com/example/androidmixtape/playback/PlatformMediaPlayerEngine.kt").readText()
        val body = methodBody(source, "replacePlaylistPreservingPlayback")

        assertTrue(
            "PlatformMediaPlayerEngine preservation should update its backing tracks list and remapped currentIndex without releasing/recreating the active MediaPlayer when the current track survives deletion.",
            body.contains("tracks = tracks") &&
                body.contains("currentIndex") &&
                !body.contains("player?.release()"),
        )
    }

    @Test
    fun legacyMediaPlayerCompletionAdvancesAndReportsNextTrack() {
        val source = File(projectDir, "app/src/legacy/java/com/example/androidmixtape/playback/PlatformMediaPlayerEngine.kt").readText()

        assertTrue(
            "Legacy MediaPlayerEngine should set an OnCompletionListener that advances to currentIndex + 1 when available and reports that automatic index change.",
            source.contains("setOnCompletionListener") &&
                source.contains("currentIndex + 1") &&
                (source.contains("CurrentIndexChanged") || source.contains("currentIndexChanged")),
        )
    }

    @Test
    fun legacyActivityRefreshesUiWhenControllerPlaybackStateChangesAutomatically() {
        val source = File(projectDir, "app/src/legacy/java/com/example/androidmixtape/MainActivity.kt").readText()
        val registersPlaybackChangeCallback = listOf(
            Regex("""(?:MixtapeController\(|controller\.)[\s\S]{0,400}(?:on|set|add|observe)\w*(?:State|Playback|CurrentIndex|Change|Changed|Listener)"""),
            Regex("""controller\.\w*(?:on|state|playback|currentIndex)\w*(?:Changed|Listener)\s*="""),
        ).any { it.containsMatchIn(source) }
        val refreshesStatusOnUiThread = Regex(
            """runOnUiThread\s*(?:\(\s*::updatePlaybackStatus\s*\)|\{[\s\S]{0,300}updatePlaybackStatus\(\))""",
        ).containsMatchIn(source)

        assertTrue(
            "Legacy MainActivity must observe controller/player automatic track changes and call runOnUiThread { updatePlaybackStatus() } so MediaPlayer completion refreshes the status text and buttons without a user tapping Next.",
            registersPlaybackChangeCallback && refreshesStatusOnUiThread,
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
