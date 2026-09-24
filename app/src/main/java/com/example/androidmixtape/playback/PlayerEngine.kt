package com.example.androidmixtape.playback

import com.example.androidmixtape.data.Track

data class ExternalPlaybackSnapshot(
    val tracks: List<Track>,
    val currentIndex: Int,
    val currentMediaId: String? = null,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    /** True only for an engine-reported position discontinuity, not a progress sample. */
    val positionDiscontinuity: Boolean = false,
)

interface PlayerEngine {
    fun loadPlaylist(tracks: List<Track>)
    /** Startup only: never replace a queue restored by a late media-session connection. */
    fun initializePlaylistIfEmpty(tracks: List<Track>) = loadPlaylist(tracks)
    fun replacePlaylistPreservingPlayback(tracks: List<Track>, currentIndex: Int)
    fun playIndex(index: Int)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setOnCurrentIndexChanged(listener: ((Int) -> Unit)?)
    fun setOnExternalPlaybackSnapshotChanged(listener: ((ExternalPlaybackSnapshot) -> Unit)?)
    fun release()
}
