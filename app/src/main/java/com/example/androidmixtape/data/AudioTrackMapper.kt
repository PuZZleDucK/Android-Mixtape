package com.example.androidmixtape.data

data class AudioRow(
    val id: Long?,
    val title: String?,
    val artist: String?,
    val displayName: String?,
    val durationMs: Long?,
    val album: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val dateAddedSeconds: Long? = null,
    val dateModifiedSeconds: Long? = null,
    val trackNumber: Int? = null,
)

object AudioTrackMapper {
    fun mapRows(rows: Sequence<AudioRow>): List<Track> = rows
        .mapNotNull { row -> row.toTrack() }
        .sortedWith(compareBy<Track> { it.title.lowercase() }.thenBy { it.id })
        .toList()

    private fun AudioRow.toTrack(): Track? {
        val stableId = id ?: return null
        val safeDuration = durationMs ?: return null
        if (stableId < 0 || safeDuration <= 0L) return null

        val fallbackTitle = displayName?.trim().orEmpty().ifBlank { "Track $stableId" }
        val safeTitle = title?.trim().orEmpty().ifBlank { fallbackTitle }
        val safeArtist = artist?.trim().orEmpty().ifBlank { "Unknown artist" }
        val uri = "content://media/external/audio/media/$stableId"

        return Track(
            id = stableId,
            title = safeTitle,
            artist = safeArtist,
            durationMs = safeDuration,
            uri = uri,
            displayName = displayName,
            album = album,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            dateAddedSeconds = dateAddedSeconds,
            dateModifiedSeconds = dateModifiedSeconds,
            trackNumber = trackNumber,
        )
    }
}
