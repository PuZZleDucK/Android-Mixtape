package com.example.androidmixtape.data

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class DeleteTrackResult {
    data object Success : DeleteTrackResult()
    data class RequiresUserAction(
        val intentSender: IntentSender,
        val retryAfterApproval: Boolean = false,
    ) : DeleteTrackResult()
    data class Failure(val message: String) : DeleteTrackResult()
}

interface AudioRepository {
    suspend fun loadTracks(): List<Track>
    suspend fun deleteTrack(track: Track): DeleteTrackResult = DeleteTrackResult.Failure("Delete from device is not available for this audio source")
}

class MediaStoreAudioRepository(
    context: Context,
) : AudioRepository {
    private val resolver: ContentResolver = context.contentResolver

    override suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.TRACK,
        )
        val cursor = resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null,
        ) ?: return@withContext emptyList()

        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val displayNameIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val mimeTypeIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateAddedIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val trackNumberIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            val rows = sequence {
                while (it.moveToNext()) {
                    yield(
                        AudioRow(
                            id = it.getLongOrNull(idIndex),
                            title = it.getStringOrNull(titleIndex),
                            artist = it.getStringOrNull(artistIndex),
                            displayName = it.getStringOrNull(displayNameIndex),
                            durationMs = it.getLongOrNull(durationIndex),
                            album = it.getStringOrNull(albumIndex),
                            mimeType = it.getStringOrNull(mimeTypeIndex),
                            sizeBytes = it.getLongOrNull(sizeIndex),
                            dateAddedSeconds = it.getLongOrNull(dateAddedIndex),
                            dateModifiedSeconds = it.getLongOrNull(dateModifiedIndex),
                            trackNumber = it.getIntOrNull(trackNumberIndex),
                        ),
                    )
                }
            }
            AudioTrackMapper.mapRows(rows)
        }
    }

    override suspend fun deleteTrack(track: Track): DeleteTrackResult = withContext(Dispatchers.IO) {
        try {
            val trackUri = Uri.parse(track.uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return@withContext DeleteTrackResult.RequiresUserAction(
                    MediaStore.createDeleteRequest(resolver, listOf(trackUri)).intentSender,
                )
            }

            val deletedRows = resolver.delete(trackUri, null, null)
            if (deletedRows > 0) {
                DeleteTrackResult.Success
            } else {
                DeleteTrackResult.Failure("Track was not deleted by the device media provider")
            }
        } catch (error: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && error is RecoverableSecurityException) {
                DeleteTrackResult.RequiresUserAction(
                    error.userAction.actionIntent.intentSender,
                    retryAfterApproval = true,
                )
            } else {
                DeleteTrackResult.Failure(error.message ?: "Device permission is required to delete this track")
            }
        } catch (error: IllegalArgumentException) {
            DeleteTrackResult.Failure(error.message ?: "Track URI is invalid")
        }
    }
}

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

private fun android.database.Cursor.getIntOrNull(index: Int): Int? =
    if (isNull(index)) null else getInt(index)
