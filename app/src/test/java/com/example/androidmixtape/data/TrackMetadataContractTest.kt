package com.example.androidmixtape.data

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMetadataContractTest {
    @Test
    fun trackAndAudioRowCarryOptionalMetadataForInfoPage() {
        val trackSource = readFirstExisting("app/src/main/java/com/example/androidmixtape/data/Track.kt")
        val mapperSource = readFirstExisting("app/src/main/java/com/example/androidmixtape/data/AudioTrackMapper.kt")
        val trackSignature = trackSource.substringAfter("data class Track(").substringBefore(") {")
        val audioRowSignature = mapperSource.substringAfter("data class AudioRow(").substringBefore(")")

        val requiredFields = mapOf(
            "displayName" to "String?",
            "album" to "String?",
            "mimeType" to "String?",
            "sizeBytes" to "Long?",
            "dateAddedSeconds" to "Long?",
            "dateModifiedSeconds" to "Long?",
            "trackNumber" to "Int?",
        )

        requiredFields.forEach { (name, type) ->
            assertTrue(
                "Track should include optional metadata field $name: $type for the Track info page.",
                Regex("val\\s+$name\\s*:\\s*${Regex.escape(type)}").containsMatchIn(trackSignature),
            )
            assertTrue(
                "AudioRow should include optional metadata field $name: $type so the mapper can preserve MediaStore data.",
                Regex("val\\s+$name\\s*:\\s*${Regex.escape(type)}").containsMatchIn(audioRowSignature),
            )
        }
    }

    @Test
    fun mapperPreservesMetadataWhileStillAcceptingNullOptionalColumns() {
        val mapperSource = readFirstExisting("app/src/main/java/com/example/androidmixtape/data/AudioTrackMapper.kt")
        val toTrackBody = mapperSource.substringAfter("private fun AudioRow.toTrack(): Track?").substringBefore("\n    }")

        listOf("displayName", "album", "mimeType", "sizeBytes", "dateAddedSeconds", "dateModifiedSeconds", "trackNumber").forEach { field ->
            assertTrue(
                "AudioTrackMapper should pass nullable $field through to Track instead of dropping it.",
                Regex("$field\\s*=\\s*$field").containsMatchIn(toTrackBody),
            )
        }
        assertTrue(
            "Only id and duration should remain required; optional metadata nulls must not cause row rejection.",
            !toTrackBody.contains("album ?: return null") &&
                !toTrackBody.contains("displayName ?: return null") &&
                !toTrackBody.contains("mimeType ?: return null"),
        )
    }

    @Test
    fun mediaStoreRepositoryQueriesMetadataNeededByTrackInfo() {
        val repositorySource = readFirstExisting("app/src/main/java/com/example/androidmixtape/data/AudioRepository.kt")

        listOf(
            "MediaStore.Audio.Media.ALBUM",
            "MediaStore.Audio.Media.MIME_TYPE",
            "MediaStore.Audio.Media.SIZE",
            "MediaStore.Audio.Media.DATE_ADDED",
            "MediaStore.Audio.Media.DATE_MODIFIED",
            "MediaStore.Audio.Media.TRACK",
        ).forEach { column ->
            assertTrue(
                "MediaStoreAudioRepository projection should include $column for the Track info page.",
                repositorySource.contains(column),
            )
        }
        listOf("album", "mimeType", "sizeBytes", "dateAddedSeconds", "dateModifiedSeconds", "trackNumber").forEach { field ->
            assertTrue(
                "MediaStoreAudioRepository should read and populate AudioRow.$field.",
                repositorySource.contains("$field ="),
            )
        }
    }

    private fun readFirstExisting(vararg paths: String): String {
        val candidates = paths.flatMap { path ->
            val withoutAppPrefix = path.removePrefix("app/")
            listOf(File(path), File(withoutAppPrefix), File("../$path"), File("../$withoutAppPrefix"))
        }
        val file = candidates.firstOrNull { it.exists() }
        assertTrue("Expected one of ${candidates.map { it.path }} from ${System.getProperty("user.dir")}", file != null)
        return file!!.readText()
    }
}
