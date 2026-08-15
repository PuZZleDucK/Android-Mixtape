package com.example.androidmixtape.viewmodel

import com.example.androidmixtape.data.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilenameExclusionMatcherTest {
    @Test
    fun globPatternMatchesDisplayNameCaseInsensitively() {
        assertExcluded(
            patterns = listOf("*.tmp.mp3"),
            track = track(displayName = "Song.TMP.mp3", title = "Song"),
        )
        assertIncluded(
            patterns = listOf("*.tmp.mp3"),
            track = track(displayName = "Song.final.mp3", title = "Song"),
        )
    }

    @Test
    fun questionMarkWildcardMatchesOneFilenameCharacter() {
        assertExcluded(
            patterns = listOf("take-?.wav"),
            track = track(displayName = "take-7.wav", title = "Take Seven"),
        )
        assertIncluded(
            patterns = listOf("take-?.wav"),
            track = track(displayName = "take-12.wav", title = "Take Twelve"),
        )
    }

    @Test
    fun patternWithoutWildcardBehavesLikeContainsMatch() {
        assertExcluded(
            patterns = listOf("voice memo"),
            track = track(displayName = "2026-07-04 Voice Memo.m4a", title = "Recording"),
        )
    }

    @Test
    fun regexMetacharactersAreTreatedAsLiteralFilenameText() {
        assertExcluded(
            patterns = listOf("mix (demo) [raw].mp3"),
            track = track(displayName = "mix (demo) [raw].mp3", title = "Demo"),
        )
        assertIncluded(
            patterns = listOf("mix (demo) [raw].mp3"),
            track = track(displayName = "mix demo raw.mp3", title = "Demo"),
        )
    }

    @Test
    fun titleIsOnlyFallbackWhenDisplayNameIsMissing() {
        assertExcluded(
            patterns = listOf("*draft*"),
            track = track(displayName = null, title = "Draft Song"),
        )
        assertIncluded(
            patterns = listOf("*draft*"),
            track = track(displayName = "released-song.mp3", title = "Draft Song"),
        )
    }

    private fun assertExcluded(patterns: List<String>, track: Track) {
        assertTrue("Expected ${track.displayName ?: track.title} to match $patterns", matches(track, patterns))
    }

    private fun assertIncluded(patterns: List<String>, track: Track) {
        assertFalse("Expected ${track.displayName ?: track.title} not to match $patterns", matches(track, patterns))
    }

    private fun matches(track: Track, patterns: List<String>): Boolean {
        val matcherClass = Class.forName("com.example.androidmixtape.viewmodel.FilenameExclusionMatcher")
        val instance = matcherClass.getField("INSTANCE").get(null)
        val method = matcherClass.methods.singleOrNull { method ->
            method.name == "matches" && method.parameterTypes.toList() == listOf(Track::class.java, List::class.java)
        }
        requireNotNull(method) {
            "FilenameExclusionMatcher must expose matches(track: Track, patterns: List<String>): Boolean"
        }
        return method.invoke(instance, track, patterns) as Boolean
    }

    private fun track(displayName: String?, title: String): Track = Track(
        id = 1L,
        title = title,
        artist = "Artist",
        durationMs = 1_000L,
        uri = "content://track/1",
        displayName = displayName,
    )
}
