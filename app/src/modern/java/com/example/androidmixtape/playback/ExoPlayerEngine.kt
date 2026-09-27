package com.example.androidmixtape.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.androidmixtape.data.Track

class ExoPlayerEngine(context: Context) : PlayerEngine {
    private var currentIndexChangedListener: ((Int) -> Unit)? = null

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        setWakeMode(C.WAKE_MODE_LOCAL)
        addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    currentIndexChangedListener?.invoke(currentMediaItemIndex)
                }
            }
        })
    }

    override fun loadPlaylist(tracks: List<Track>) {
        player.setMediaItems(tracks.map(::trackMediaItem))
        player.prepare()
    }

    override fun replacePlaylistPreservingPlayback(tracks: List<Track>, currentIndex: Int) {
        if (tracks.isEmpty()) {
            player.clearMediaItems()
            return
        }
        val currentPosition = player.currentPosition.coerceAtLeast(0L)
        val playWhenReady = player.playWhenReady
        player.setMediaItems(
            tracks.map(::trackMediaItem),
            currentIndex.coerceIn(0, tracks.lastIndex),
            currentPosition,
        )
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    override fun playIndex(index: Int) {
        player.seekTo(index, 0L)
        player.playWhenReady = true
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    override fun setOnCurrentIndexChanged(listener: ((Int) -> Unit)?) {
        currentIndexChangedListener = listener
    }

    override fun setOnExternalPlaybackSnapshotChanged(listener: ((ExternalPlaybackSnapshot) -> Unit)?) {
        // This engine is owned by the phone UI and has no external MediaController session.
    }

    override fun release() {
        player.release()
    }

    private fun trackMediaItem(track: Track): MediaItem = MediaItem.Builder()
        .setMediaId("track:${track.id}")
        .setUri(track.uri)
        .build()
}
