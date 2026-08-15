package com.example.androidmixtape.data

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: String,
    val displayName: String? = null,
    val album: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val dateAddedSeconds: Long? = null,
    val dateModifiedSeconds: Long? = null,
    val trackNumber: Int? = null,
) {
    val displaySubtitle: String
        get() = artist.ifBlank { "Unknown artist" }
}
