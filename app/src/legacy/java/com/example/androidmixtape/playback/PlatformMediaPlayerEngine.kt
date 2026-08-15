package com.example.androidmixtape.playback

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import com.example.androidmixtape.data.Track

class PlatformMediaPlayerEngine(
    private val context: Context,
) : PlayerEngine {
    private var tracks: List<Track> = emptyList()
    private var player: MediaPlayer? = null
    private var currentIndex: Int = -1
    private var currentIndexChangedListener: ((Int) -> Unit)? = null

    override fun loadPlaylist(tracks: List<Track>) {
        this.tracks = tracks
        currentIndex = if (tracks.isEmpty()) -1 else currentIndex.coerceAtLeast(0).coerceAtMost(tracks.lastIndex)
    }

    override fun replacePlaylistPreservingPlayback(tracks: List<Track>, currentIndex: Int) {
        this.tracks = tracks
        this.currentIndex = if (tracks.isEmpty()) -1 else currentIndex.coerceIn(0, tracks.lastIndex)
        if (tracks.isEmpty()) {
            player?.pause()
        }
    }

    override fun playIndex(index: Int) {
        if (index !in tracks.indices) return
        currentIndex = index
        player?.release()
        player = MediaPlayer().apply {
            @Suppress("DEPRECATION")
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            setDataSource(context, Uri.parse(tracks[index].uri))
            setOnCompletionListener {
                val nextIndex = currentIndex + 1
                if (nextIndex in tracks.indices) {
                    playIndex(nextIndex)
                    currentIndexChangedListener?.invoke(nextIndex)
                }
            }
            prepare()
            start()
        }
    }

    override fun play() {
        val existing = player
        if (existing != null) {
            existing.start()
        } else if (currentIndex in tracks.indices) {
            playIndex(currentIndex)
        }
    }

    override fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    override fun setOnCurrentIndexChanged(listener: ((Int) -> Unit)?) {
        currentIndexChangedListener = listener
    }

    override fun setOnExternalPlaybackSnapshotChanged(listener: ((ExternalPlaybackSnapshot) -> Unit)?) {
        // Legacy API 19 playback is phone-local; Android Auto car sessions use the modern flavor.
    }

    override fun release() {
        player?.release()
        player = null
        currentIndexChangedListener = null
    }
}
