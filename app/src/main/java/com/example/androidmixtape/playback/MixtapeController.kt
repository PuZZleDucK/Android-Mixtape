package com.example.androidmixtape.playback

import com.example.androidmixtape.data.Track
import kotlin.math.max
import kotlin.math.min

data class PlayerUiState(
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isReleased: Boolean = false,
) {
    val currentTrack: Track? = tracks.getOrNull(currentIndex)
    val canGoPrevious: Boolean = currentIndex > 0
    val canGoNext: Boolean = currentIndex >= 0 && currentIndex < tracks.lastIndex
}

class MixtapeController(
    private val player: PlayerEngine,
) {
    var state: PlayerUiState = PlayerUiState()
        private set
    private var playbackStateChangedListener: ((PlayerUiState) -> Unit)? = null

    init {
        player.setOnCurrentIndexChanged(::handleCurrentIndexChanged)
        player.setOnExternalPlaybackSnapshotChanged(::handleExternalPlaybackSnapshot)
    }

    fun setOnPlaybackStateChanged(listener: ((PlayerUiState) -> Unit)?) {
        playbackStateChangedListener = listener
    }

    fun load(tracks: List<Track>) {
        state = PlayerUiState(tracks = tracks, currentIndex = if (tracks.isEmpty()) -1 else 0)
        if (tracks.isNotEmpty()) {
            player.loadPlaylist(tracks)
        }
    }

    fun replaceQueueAfterCurrentRemoval(tracks: List<Track>, nextIndex: Int, playNext: Boolean) {
        if (tracks.isEmpty()) {
            if (state.isPlaying) player.pause()
            player.replacePlaylistPreservingPlayback(emptyList(), -1)
            state = PlayerUiState()
            return
        }

        val clampedIndex = nextIndex.coerceIn(0, tracks.lastIndex)
        if (playNext) {
            player.loadPlaylist(tracks)
            state = PlayerUiState(tracks = tracks, currentIndex = clampedIndex, durationMs = tracks[clampedIndex].durationMs)
            select(clampedIndex)
        } else {
            if (state.isPlaying) player.pause()
            player.replacePlaylistPreservingPlayback(tracks, clampedIndex)
            state = PlayerUiState(tracks = tracks, currentIndex = clampedIndex, durationMs = tracks[clampedIndex].durationMs)
        }
    }

    fun replaceQueuePreservingCurrentTrack(tracks: List<Track>) {
        val previousCurrentTrack = state.currentTrack
        val preservedCurrentIndex = previousCurrentTrack?.let { currentTrack ->
            tracks.indexOfFirst { it.sameTrackAs(currentTrack) }
        } ?: -1

        if (tracks.isEmpty()) {
            if (state.isPlaying) player.pause()
            player.replacePlaylistPreservingPlayback(emptyList(), -1)
            state = PlayerUiState()
            return
        }

        if (preservedCurrentIndex >= 0) {
            player.replacePlaylistPreservingPlayback(tracks, preservedCurrentIndex)
            state = state.copy(
                tracks = tracks,
                currentIndex = preservedCurrentIndex,
                durationMs = tracks[preservedCurrentIndex].durationMs,
            )
            return
        }

        if (state.isPlaying) player.pause()
        player.loadPlaylist(tracks)
        val nextIndex = state.currentIndex.coerceIn(0, tracks.lastIndex)
        state = PlayerUiState(
            tracks = tracks,
            currentIndex = nextIndex,
            durationMs = tracks[nextIndex].durationMs,
        )
    }

    fun select(index: Int) {
        if (index !in state.tracks.indices) return
        val track = state.tracks[index]
        player.playIndex(index)
        state = state.copy(
            currentIndex = index,
            isPlaying = true,
            positionMs = 0L,
            durationMs = track.durationMs,
        )
    }

    fun togglePlayPause() {
        if (state.currentTrack == null) return
        if (state.isPlaying) {
            player.pause()
            state = state.copy(isPlaying = false)
        } else {
            player.play()
            state = state.copy(isPlaying = true)
        }
    }

    fun next() {
        if (state.canGoNext) select(state.currentIndex + 1)
    }

    fun previous() {
        if (state.canGoPrevious) select(state.currentIndex - 1)
    }

    fun seekTo(positionMs: Long) {
        val duration = state.currentTrack?.durationMs ?: return
        val clamped = min(max(positionMs, 0L), duration)
        player.seekTo(clamped)
        state = state.copy(positionMs = clamped, durationMs = duration)
    }

    fun stop() {
        val duration = state.currentTrack?.durationMs ?: return
        player.pause()
        player.seekTo(0L)
        state = state.copy(isPlaying = false, positionMs = 0L, durationMs = duration)
    }

    private fun handleCurrentIndexChanged(index: Int) {
        val track = state.tracks.getOrNull(index) ?: return
        state = state.copy(
            currentIndex = index,
            isPlaying = true,
            positionMs = 0L,
            durationMs = track.durationMs,
        )
        playbackStateChangedListener?.invoke(state)
    }

    private fun handleExternalPlaybackSnapshot(snapshot: ExternalPlaybackSnapshot) {
        if (snapshot.tracks.isEmpty()) {
            state = PlayerUiState(
                isPlaying = snapshot.isPlaying,
                positionMs = snapshot.positionMs,
                durationMs = snapshot.durationMs,
            )
            playbackStateChangedListener?.invoke(state)
            return
        }

        val snapshotIndex = when {
            snapshot.currentIndex in snapshot.tracks.indices -> snapshot.currentIndex
            snapshot.currentMediaId != null -> snapshot.tracks.indexOfFirst { it.mediaId == snapshot.currentMediaId }
            else -> -1
        }.takeIf { it >= 0 } ?: 0
        val normalizedSnapshot = snapshot.copy(currentIndex = snapshotIndex)
        state = PlayerUiState(
            tracks = snapshot.tracks,
            currentIndex = snapshot.currentIndex,
            isPlaying = snapshot.isPlaying,
            positionMs = snapshot.positionMs,
            durationMs = snapshot.durationMs,
        ).let { nextState ->
            if (snapshot.currentIndex == normalizedSnapshot.currentIndex) {
                nextState
            } else {
                nextState.copy(currentIndex = normalizedSnapshot.currentIndex)
            }
        }
        playbackStateChangedListener?.invoke(state)
    }

    fun release() {
        if (!state.isReleased) {
            player.release()
            playbackStateChangedListener = null
            state = state.copy(isReleased = true, isPlaying = false)
        }
    }
}

private fun Track.sameTrackAs(other: Track): Boolean = id == other.id || uri == other.uri

private val Track.mediaId: String
    get() = "track:$id"
