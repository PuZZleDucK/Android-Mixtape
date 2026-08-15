package com.example.androidmixtape.viewmodel

import android.content.Context
import com.example.androidmixtape.data.Track

/** Ordered filename exclusion settings used to keep matching files out of mixtapes. */
data class MixtapeExclusionSettings(
    val filenamePatterns: List<String> = emptyList(),
) {
    companion object {
        fun normalized(patterns: List<String>): MixtapeExclusionSettings {
            val seen = mutableSetOf<String>()
            val normalizedPatterns = patterns.map { it.trim() }
                .filter { it.isNotEmpty() }
                .filter { pattern -> seen.add(pattern.lowercase()) }
            return MixtapeExclusionSettings(normalizedPatterns)
        }
    }
}

object FilenameExclusionMatcher {
    fun matches(track: Track, patterns: List<String>): Boolean {
        val filename = track.displayName?.takeIf { it.isNotBlank() } ?: track.title
        return patterns.any { pattern -> matchesPattern(filename, pattern) }
    }

    private fun matchesPattern(filename: String, rawPattern: String): Boolean {
        val trimmedPattern = rawPattern.trim()
        if (trimmedPattern.isEmpty()) return false
        val effectivePattern = if (trimmedPattern.any { it == '*' || it == '?' }) {
            trimmedPattern
        } else {
            "*$trimmedPattern*"
        }
        return Regex(globToRegex(effectivePattern), RegexOption.IGNORE_CASE).matches(filename)
    }

    private fun globToRegex(pattern: String): String = buildString {
        append('^')
        pattern.forEach { character ->
            when (character) {
                '*' -> append(".*")
                '?' -> append('.')
                else -> append(Regex.escape(character.toString()))
            }
        }
        append('$')
    }
}

interface MixtapeExclusionSettingsStore {
    fun settings(): MixtapeExclusionSettings
    fun saveSettings(settings: MixtapeExclusionSettings)
}

class InMemoryMixtapeExclusionSettingsStore(
    initialSettings: MixtapeExclusionSettings = MixtapeExclusionSettings(),
) : MixtapeExclusionSettingsStore {
    constructor(initialPatterns: List<String>) : this(MixtapeExclusionSettings.normalized(initialPatterns))

    private var currentSettings = MixtapeExclusionSettings.normalized(initialSettings.filenamePatterns)

    override fun settings(): MixtapeExclusionSettings = currentSettings

    override fun saveSettings(settings: MixtapeExclusionSettings) {
        currentSettings = MixtapeExclusionSettings.normalized(settings.filenamePatterns)
    }
}

class SharedPreferencesMixtapeExclusionSettingsStore(
    context: Context,
) : MixtapeExclusionSettingsStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mixtape_exclusion_settings",
        Context.MODE_PRIVATE,
    )

    override fun settings(): MixtapeExclusionSettings {
        val savedPatterns = preferences.getString(KEY_FILENAME_PATTERNS, null)
            ?.lineSequence()
            ?.toList()
            .orEmpty()
        return MixtapeExclusionSettings.normalized(savedPatterns)
    }

    override fun saveSettings(settings: MixtapeExclusionSettings) {
        val normalized = MixtapeExclusionSettings.normalized(settings.filenamePatterns)
        preferences.edit()
            .putString(KEY_FILENAME_PATTERNS, normalized.filenamePatterns.joinToString("\n"))
            .apply()
    }

    private companion object {
        const val KEY_FILENAME_PATTERNS = "filename_patterns"
    }
}
