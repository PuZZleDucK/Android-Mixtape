package com.example.androidmixtape.playback

import com.example.androidmixtape.data.Track

data class ExternalPlaybackSnapshot(
    val tracks: List<Track>,
    val currentIndex: Int,
    val currentMediaId: String? = null,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
)

interface PlayerEngine {
    fun loadPlaylist(tracks: List<Track>)
    fun replacePlaylistPreservingPlayback(tracks: List<Track>, currentIndex: Int)
    fun playIndex(index: Int)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setOnCurrentIndexChanged(listener: ((Int) -> Unit)?)
    fun setOnExternalPlaybackSnapshotChanged(listener: ((ExternalPlaybackSnapshot) -> Unit)?)
    fun release()
}
