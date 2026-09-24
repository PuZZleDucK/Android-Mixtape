package com.example.androidmixtape.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.androidmixtape.data.Track
import com.google.common.util.concurrent.MoreExecutors

class MediaControllerPlayerEngine(context: Context) : PlayerEngine {
    private val appContext = context.applicationContext
    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, MixtapeMediaLibraryService::class.java)),
    ).buildAsync()
    private data class PendingControllerAction(
        val queueSensitive: Boolean,
        val action: (MediaController) -> Unit,
    )

    private val pendingActions = mutableListOf<PendingControllerAction>()
    private var controller: MediaController? = null
    private var currentIndexChangedListener: ((Int) -> Unit)? = null
    private var externalPlaybackSnapshotListener: ((ExternalPlaybackSnapshot) -> Unit)? = null
    private var released = false
    private val positionHandler = Handler(Looper.getMainLooper())
    private var lastPublishedPositionMs: Long? = null
    private val positionSample = object : Runnable {
        override fun run() {
            val player = controller?.takeIf { !released && it.isConnected && it.isPlaying && it.mediaItemCount > 0 }
                ?: return
            val positionMs = player.currentPosition.coerceAtLeast(0L)
            if (shouldPublishPositionSample(true, player.mediaItemCount, positionMs, lastPublishedPositionMs)) {
                publishExternalPlaybackSnapshot(player)
            }
            positionHandler.postDelayed(this, 350L)
        }
    }

    private fun schedulePositionSamples(player: Player) {
        positionHandler.removeCallbacks(positionSample)
        if (!released && player.isPlaying && player.mediaItemCount > 0) {
            positionHandler.postDelayed(positionSample, 350L)
        }
    }

    init {
        controllerFuture.addListener(
            {
                val connectedController = runCatching { controllerFuture.get() }.getOrNull() ?: return@addListener
                val actions = synchronized(this) {
                    if (released) {
                        connectedController.release()
                        return@addListener
                    }
                    controller = connectedController
                    connectedController.addListener(
                        object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                currentIndexChangedListener?.invoke(connectedController.currentMediaItemIndex)
                            }

                            override fun onEvents(player: Player, events: Player.Events) {
                                if (
                                    events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                                    events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                                    events.contains(Player.EVENT_IS_PLAYING_CHANGED) ||
                                    events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                                    events.contains(Player.EVENT_POSITION_DISCONTINUITY)
                                ) {
                                    publishExternalPlaybackSnapshot(
                                        player,
                                        positionDiscontinuity = events.contains(Player.EVENT_POSITION_DISCONTINUITY),
                                    )
                                    schedulePositionSamples(player)
                                }
                            }
                        },
                    )
                    publishExternalPlaybackSnapshot(connectedController)
                    schedulePositionSamples(connectedController)
                    pendingActions.toList().also { pendingActions.clear() }
                }
                actions.forEach { pendingAction -> pendingAction.action(connectedController) }
            },
            MoreExecutors.directExecutor(),
        )
    }

    override fun loadPlaylist(tracks: List<Track>) {
        val mediaItems = tracks.map(::trackMediaItem)
        runWhenConnected(queueSensitive = true) { mediaController ->
            mediaController.setMediaItems(mediaItems)
            mediaController.prepare()
        }
    }

    override fun initializePlaylistIfEmpty(tracks: List<Track>) {
        val mediaItems = tracks.map(::trackMediaItem)
        runWhenConnected(queueSensitive = true) { mediaController ->
            if (mediaController.mediaItemCount == 0) {
                mediaController.setMediaItems(mediaItems)
                mediaController.prepare()
            }
        }
    }

    override fun replacePlaylistPreservingPlayback(tracks: List<Track>, currentIndex: Int) {
        val mediaItems = tracks.map(::trackMediaItem)
        runWhenConnected(queueSensitive = true, replaceQueuedPlaylistActions = true) { mediaController ->
            if (mediaItems.isEmpty()) {
                mediaController.clearMediaItems()
                return@runWhenConnected
            }
            val currentPosition = mediaController.currentPosition.coerceAtLeast(0L)
            val playWhenReady = mediaController.playWhenReady
            mediaController.setMediaItems(
                mediaItems,
                currentIndex.coerceIn(0, mediaItems.lastIndex),
                currentPosition,
            )
            mediaController.prepare()
            mediaController.playWhenReady = playWhenReady
        }
    }

    override fun playIndex(index: Int) {
        runWhenConnected(queueSensitive = true) { mediaController ->
            mediaController.seekTo(index, 0L)
            mediaController.play()
        }
    }

    override fun play() {
        runWhenConnected { it.play() }
    }

    override fun pause() {
        runWhenConnected { it.pause() }
    }

    override fun seekTo(positionMs: Long) {
        runWhenConnected { it.seekTo(positionMs) }
    }

    override fun setOnCurrentIndexChanged(listener: ((Int) -> Unit)?) {
        currentIndexChangedListener = listener
    }

    override fun setOnExternalPlaybackSnapshotChanged(listener: ((ExternalPlaybackSnapshot) -> Unit)?) {
        externalPlaybackSnapshotListener = listener
        controller?.takeIf { it.isConnected }?.let { publishExternalPlaybackSnapshot(it) }
    }

    override fun release() {
        synchronized(this) {
            released = true
            pendingActions.clear()
        }
        positionHandler.removeCallbacks(positionSample)
        controller?.release() ?: MediaController.releaseFuture(controllerFuture)
        controller = null
        currentIndexChangedListener = null
        externalPlaybackSnapshotListener = null
    }

    private fun runWhenConnected(
        queueSensitive: Boolean = false,
        replaceQueuedPlaylistActions: Boolean = false,
        action: (MediaController) -> Unit,
    ) {
        val connectedController = synchronized(this) {
            if (released) return
            controller?.takeIf { it.isConnected } ?: run {
                if (replaceQueuedPlaylistActions) {
                    pendingActions.removeAll { it.queueSensitive }
                }
                pendingActions += PendingControllerAction(queueSensitive, action)
                return
            }
        }
        action(connectedController)
    }

    private fun publishExternalPlaybackSnapshot(player: Player, positionDiscontinuity: Boolean = false) {
        val tracks = (0 until player.mediaItemCount).map { index ->
            player.getMediaItemAt(index).toTrack(index)
        }
        val currentIndex = when {
            tracks.isEmpty() -> -1
            player.currentMediaItemIndex in tracks.indices -> player.currentMediaItemIndex
            else -> 0
        }
        val currentMediaId = player.currentMediaItem?.mediaId ?: tracks.getOrNull(currentIndex)?.mediaId
        val duration = player.duration.takeIf { it > 0L }
            ?: tracks.getOrNull(currentIndex)?.durationMs
            ?: 0L
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        lastPublishedPositionMs = positionMs
        externalPlaybackSnapshotListener?.invoke(
            ExternalPlaybackSnapshot(
                tracks = tracks,
                currentIndex = currentIndex,
                currentMediaId = currentMediaId,
                isPlaying = player.isPlaying,
                positionMs = positionMs,
                durationMs = duration,
                positionDiscontinuity = positionDiscontinuity,
            ),
        )
    }

    private fun trackMediaItem(track: Track): MediaItem = MediaItem.Builder()
        .setMediaId(track.mediaId)
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setDisplayTitle(track.title)
                .setSubtitle(track.displaySubtitle)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setExtras(track.toMediaExtras())
                .build(),
        )
        .build()
}

internal fun shouldPublishPositionSample(
    playing: Boolean, itemCount: Int, positionMs: Long, lastPublishedPositionMs: Long?,
): Boolean = playing && itemCount > 0 && positionMs.coerceAtLeast(0L) != lastPublishedPositionMs

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

private val Track.mediaId: String
    get() = "track:$id"

private fun Track.toMediaExtras(): Bundle = Bundle().apply {
    putLong(EXTRA_TRACK_ID, id)
    putString(EXTRA_TRACK_TITLE, title)
    putString(EXTRA_TRACK_ARTIST, artist)
    putLong(EXTRA_TRACK_DURATION_MS, durationMs)
    putString(EXTRA_TRACK_URI, uri)
    displayName?.let { putString(EXTRA_TRACK_DISPLAY_NAME, it) }
    album?.let { putString(EXTRA_TRACK_ALBUM, it) }
    mimeType?.let { putString(EXTRA_TRACK_MIME_TYPE, it) }
    sizeBytes?.let { putLong(EXTRA_TRACK_SIZE_BYTES, it) }
    dateAddedSeconds?.let { putLong(EXTRA_TRACK_DATE_ADDED_SECONDS, it) }
    dateModifiedSeconds?.let { putLong(EXTRA_TRACK_DATE_MODIFIED_SECONDS, it) }
    trackNumber?.let { putInt(EXTRA_TRACK_NUMBER, it) }
}

private fun MediaItem.toTrack(fallbackIndex: Int): Track {
    val extras = mediaMetadata.extras
    val parsedId = mediaId.removePrefix("track:").toLongOrNull()
    val id = extras?.getLong(EXTRA_TRACK_ID, Long.MIN_VALUE)
        ?.takeIf { it != Long.MIN_VALUE }
        ?: parsedId
        ?: mediaId.hashCode().toLong()
    val title = extras?.getString(EXTRA_TRACK_TITLE)
        ?: mediaMetadata.displayTitle?.toString()
        ?: mediaMetadata.title?.toString()
        ?: "Track ${fallbackIndex + 1}"
    val artist = extras?.getString(EXTRA_TRACK_ARTIST)
        ?: mediaMetadata.artist?.toString()
        ?: mediaMetadata.subtitle?.toString()
        ?: "Unknown artist"
    val durationMs = extras?.getLong(EXTRA_TRACK_DURATION_MS, 0L)?.takeIf { it > 0L } ?: 0L
    val uri = extras?.getString(EXTRA_TRACK_URI)
        ?: localConfiguration?.uri?.toString()
        ?: ""
    return Track(
        id = id,
        title = title,
        artist = artist,
        durationMs = durationMs,
        uri = uri,
        displayName = extras?.getString(EXTRA_TRACK_DISPLAY_NAME),
        album = extras?.getString(EXTRA_TRACK_ALBUM) ?: mediaMetadata.albumTitle?.toString(),
        mimeType = extras?.getString(EXTRA_TRACK_MIME_TYPE),
        sizeBytes = extras?.getLong(EXTRA_TRACK_SIZE_BYTES, Long.MIN_VALUE)?.takeIf { it != Long.MIN_VALUE },
        dateAddedSeconds = extras?.getLong(EXTRA_TRACK_DATE_ADDED_SECONDS, Long.MIN_VALUE)?.takeIf { it != Long.MIN_VALUE },
        dateModifiedSeconds = extras?.getLong(EXTRA_TRACK_DATE_MODIFIED_SECONDS, Long.MIN_VALUE)?.takeIf { it != Long.MIN_VALUE },
        trackNumber = extras?.getInt(EXTRA_TRACK_NUMBER, Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE },
    )
}
