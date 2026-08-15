package com.example.androidmixtape.car

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import androidx.car.app.CarContext
import androidx.car.app.media.MediaPlaybackManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.example.androidmixtape.diagnostics.AndroidAutoDiagnostics
import com.example.androidmixtape.playback.MixtapeMediaLibraryService
import com.google.common.util.concurrent.MoreExecutors

class MixtapeCarMediaController(context: Context) {
    private val appContext = context.applicationContext
    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, MixtapeMediaLibraryService::class.java)),
    ).buildAsync()

    private val pendingActions = mutableListOf<(MediaController) -> Unit>()
    private var controller: MediaController? = null
    private var released = false

    init {
        controllerFuture.addListener(
            {
                val connected = runCatching { controllerFuture.get() }
                    .onFailure { error -> AndroidAutoDiagnostics.logError(appContext, "car_media_controller_connect_failed", error) }
                    .getOrNull()
                    ?: return@addListener
                AndroidAutoDiagnostics.log(
                    appContext,
                    "car_media_controller_connected package=${connected.connectedToken?.packageName} " +
                        "commands=${connected.availableCommands}",
                )
                val queued = synchronized(this) {
                    if (released) {
                        connected.release()
                        return@addListener
                    }
                    controller = connected
                    pendingActions.toList().also { pendingActions.clear() }
                }
                queued.forEach { action -> action(connected) }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun playMixtape(mixtape: CarMixtape, startIndex: Int = 0) {
        val trackIndex = startIndex.coerceIn(0, (mixtape.tracks.size - 1).coerceAtLeast(0))
        AndroidAutoDiagnostics.log(
            appContext,
            "car_media_controller_play name=${mixtape.displayName} tracks=${mixtape.tracks.size} requestedIndex=$startIndex resolvedIndex=$trackIndex",
        )
        runWhenConnected { mediaController ->
            val mediaItems = mixtape.tracks.map(::mediaItem)
            if (mediaItems.isEmpty()) return@runWhenConnected
            mediaController.setMediaItems(mediaItems, trackIndex, 0L)
            mediaController.prepare()
            mediaController.play()
        }
    }

    fun registerMediaPlaybackToken(carContext: CarContext) {
        AndroidAutoDiagnostics.log(appContext, "car_media_controller_register_token_requested")
        runWhenConnected { mediaController ->
            val tokenFuture = mediaController.sendCustomCommand(
                MixtapeMediaLibraryService.GET_SESSION_COMPAT_TOKEN_COMMAND,
                Bundle.EMPTY,
            )
            tokenFuture.addListener(
                {
                    val token = runCatching { tokenFuture.get().compatToken() }
                        .onFailure { error -> AndroidAutoDiagnostics.logError(appContext, "car_media_controller_token_command_failed", error) }
                        .getOrNull()
                    if (token == null) {
                        AndroidAutoDiagnostics.log(appContext, "car_media_controller_token_missing")
                        return@addListener
                    }
                    runCatching {
                        carContext.getCarService(MediaPlaybackManager::class.java).registerMediaPlaybackToken(token)
                    }.onSuccess {
                        AndroidAutoDiagnostics.log(appContext, "car_media_controller_token_registered")
                    }.onFailure { error ->
                        AndroidAutoDiagnostics.logError(appContext, "car_media_controller_token_registration_failed", error)
                    }
                },
                MoreExecutors.directExecutor(),
            )
        }
    }

    fun release() {
        AndroidAutoDiagnostics.log(appContext, "car_media_controller_release")
        synchronized(this) {
            released = true
            pendingActions.clear()
        }
        controller?.release() ?: MediaController.releaseFuture(controllerFuture)
        controller = null
    }

    private fun runWhenConnected(action: (MediaController) -> Unit) {
        val connected = synchronized(this) {
            if (released) return
            controller?.takeIf { it.isConnected } ?: run {
                pendingActions += action
                AndroidAutoDiagnostics.log(appContext, "car_media_controller_action_queued pending=${pendingActions.size}")
                return
            }
        }
        action(connected)
    }

    private fun mediaItem(track: CarTrack): MediaItem = MediaItem.Builder()
        .setMediaId(track.id)
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setDisplayTitle(track.title)
                .setSubtitle(track.subtitle)
                .setArtist(track.source.artist)
                .setAlbumTitle(track.source.album)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(track.toMediaExtras())
                .build(),
        )
        .build()

    @Suppress("DEPRECATION")
    private fun SessionResult.compatToken(): MediaSessionCompat.Token? =
        extras.getParcelable(MixtapeMediaLibraryService.EXTRA_SESSION_COMPAT_TOKEN)
}

private const val EXTRA_TRACK_ID = "com.example.androidmixtape.extra.TRACK_ID"
private const val EXTRA_TRACK_TITLE = "com.example.androidmixtape.extra.TRACK_TITLE"
private const val EXTRA_TRACK_ARTIST = "com.example.androidmixtape.extra.TRACK_ARTIST"
private const val EXTRA_TRACK_DURATION_MS = "com.example.androidmixtape.extra.TRACK_DURATION_MS"
private const val EXTRA_TRACK_URI = "com.example.androidmixtape.extra.TRACK_URI"
private const val EXTRA_TRACK_DISPLAY_NAME = "com.example.androidmixtape.extra.TRACK_DISPLAY_NAME"
private const val EXTRA_TRACK_ALBUM = "com.example.androidmixtape.extra.TRACK_ALBUM"
private const val EXTRA_TRACK_MIME_TYPE = "com.example.androidmixtape.extra.TRACK_MIME_TYPE"
private const val EXTRA_TRACK_SIZE_BYTES = "com.example.androidmixtape.extra.TRACK_SIZE_BYTES"
private const val EXTRA_TRACK_DATE_ADDED_SECONDS = "com.example.androidmixtape.extra.TRACK_DATE_ADDED_SECONDS"
private const val EXTRA_TRACK_DATE_MODIFIED_SECONDS = "com.example.androidmixtape.extra.TRACK_DATE_MODIFIED_SECONDS"
private const val EXTRA_TRACK_NUMBER = "com.example.androidmixtape.extra.TRACK_NUMBER"

private fun CarTrack.toMediaExtras(): Bundle = Bundle().apply {
    putLong(EXTRA_TRACK_ID, source.id)
    putString(EXTRA_TRACK_TITLE, source.title)
    putString(EXTRA_TRACK_ARTIST, source.artist)
    putLong(EXTRA_TRACK_DURATION_MS, source.durationMs)
    putString(EXTRA_TRACK_URI, source.uri)
    source.displayName?.let { putString(EXTRA_TRACK_DISPLAY_NAME, it) }
    source.album?.let { putString(EXTRA_TRACK_ALBUM, it) }
    source.mimeType?.let { putString(EXTRA_TRACK_MIME_TYPE, it) }
    source.sizeBytes?.let { putLong(EXTRA_TRACK_SIZE_BYTES, it) }
    source.dateAddedSeconds?.let { putLong(EXTRA_TRACK_DATE_ADDED_SECONDS, it) }
    source.dateModifiedSeconds?.let { putLong(EXTRA_TRACK_DATE_MODIFIED_SECONDS, it) }
    source.trackNumber?.let { putInt(EXTRA_TRACK_NUMBER, it) }
}
