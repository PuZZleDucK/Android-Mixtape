package com.example.androidmixtape.viewmodel

import android.content.Context
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.DeleteTrackResult
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.name.InMemoryMixtapeNameStore
import com.example.androidmixtape.name.MixtapeNameSource
import com.example.androidmixtape.name.MixtapeNameStore
import com.example.androidmixtape.playback.MixtapeController
import com.example.androidmixtape.playback.PlayerUiState
import com.example.androidmixtape.playback.SilentTransportCuePlayer
import com.example.androidmixtape.playback.TransportCueDirection
import com.example.androidmixtape.playback.TransportCuePlayer
import com.example.androidmixtape.ui.HANDWRITING_PERTURBATION_COUNT
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class LibraryStatus {
    PermissionRequired,
    Loading,
    Empty,
    Ready,
    Error,
}

enum class MixtapeScreen {
    MixTapes,
    NowPlaying,
    TrackInfo,
    Settings,
    Help,
    MixtapeNames,
    MixtapeSymbolSettings,
    TapeSkinSettings,
    SpineSkinSettings,
    HandwritingFontSettings,
    DeckThemeSettings,
    CassetteThemeSettings,
    ScrewThemeSettings,
    StickerThemeSettings,
    CaseThemeSettings,
    SleeveThemeSettings,
    ExclusionSettings,
}

const val NORMAL_SONGS_PER_MIXTAPE: Int = 24
const val LP_SONGS_PER_MIXTAPE: Int = 56
const val XLP_SONGS_PER_MIXTAPE: Int = 112
const val MEGA_SONGS_PER_MIXTAPE: Int = 256
const val DEFAULT_SONGS_PER_MIXTAPE: Int = LP_SONGS_PER_MIXTAPE
val MIXTAPE_TRACK_COUNT_OPTIONS: List<Int> = listOf(
    NORMAL_SONGS_PER_MIXTAPE,
    LP_SONGS_PER_MIXTAPE,
    XLP_SONGS_PER_MIXTAPE,
    MEGA_SONGS_PER_MIXTAPE,
)
// Keep enum entry names stable: tests and settings UI use these as the shared contract.
enum class ArtistGrouping {
    NoGrouping,
    KeepArtistTogether,
    ArtistTripletsAcrossTapes,
}

enum class HandwritingMessiness(val strengthMultiplier: Float) {
    High(1f),
    Low(0.5f),
    Off(0f),
}

enum class MixtapeHandwritingFont {
    Kalam,
    PatrickHand,
    Caveat,
    NanumPenScript,
    IndieFlower,
    GloriaHallelujah,
    ArchitectsDaughter,
    ShadowsIntoLight,
}

data class MixtapeSettings(
    val songsPerMixTape: Int = DEFAULT_SONGS_PER_MIXTAPE,
    val artistGrouping: ArtistGrouping = ArtistGrouping.ArtistTripletsAcrossTapes,
    val handwritingMessiness: HandwritingMessiness = HandwritingMessiness.Low,
) {
    init {
        require(songsPerMixTape > 0) { "songsPerMixTape must be positive" }
    }
}

interface MixtapeSettingsStore {
    fun settings(): MixtapeSettings
    fun saveSettings(settings: MixtapeSettings)
}

class InMemoryMixtapeSettingsStore(
    private var currentSettings: MixtapeSettings = MixtapeSettings(),
) : MixtapeSettingsStore {
    override fun settings(): MixtapeSettings = currentSettings

    override fun saveSettings(settings: MixtapeSettings) {
        currentSettings = settings
    }
}

class SharedPreferencesMixtapeSettingsStore(context: Context) : MixtapeSettingsStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mixtape_settings",
        Context.MODE_PRIVATE,
    )

    override fun settings(): MixtapeSettings {
        val savedSongsPerMixTape = preferences
            .getInt(KEY_SONGS_PER_MIXTAPE, DEFAULT_SONGS_PER_MIXTAPE)
            .coerceAtLeast(1)
        val songsPerMixTape = savedSongsPerMixTape.takeIf { it in MIXTAPE_TRACK_COUNT_OPTIONS }
            ?: DEFAULT_SONGS_PER_MIXTAPE
        val artistGrouping = preferences.getString(KEY_ARTIST_GROUPING, null)
            ?.let { saved -> ArtistGrouping.entries.firstOrNull { it.name == saved } }
            ?: ArtistGrouping.ArtistTripletsAcrossTapes
        val handwritingMessiness = preferences.getString(KEY_HANDWRITING_MESSINESS, null)
            ?.let { saved -> HandwritingMessiness.entries.firstOrNull { it.name == saved } }
            ?: HandwritingMessiness.Low
        return MixtapeSettings(
            songsPerMixTape = songsPerMixTape,
            artistGrouping = artistGrouping,
            handwritingMessiness = handwritingMessiness,
        )
    }

    override fun saveSettings(settings: MixtapeSettings) {
        preferences.edit()
            .putInt(KEY_SONGS_PER_MIXTAPE, settings.songsPerMixTape)
            .putString(KEY_ARTIST_GROUPING, settings.artistGrouping.name)
            .putString(KEY_HANDWRITING_MESSINESS, settings.handwritingMessiness.name)
            .apply()
    }

    private companion object {
        private const val KEY_SONGS_PER_MIXTAPE = "songs_per_mixtape"
        private const val KEY_ARTIST_GROUPING = "artist_grouping"
        private const val KEY_HANDWRITING_MESSINESS = "handwriting_messiness"
    }
}

data class MixTapeGroup(
    val name: String,
    val tracks: List<Track>,
    val startIndex: Int,
    val visualProperties: MixtapeVisualProperties = MixtapeVisualProperties(),
) {
    val handwritingFont: MixtapeHandwritingFont get() = visualProperties.handwritingFont
    val handwritingJitterStartIndex: Int get() = visualProperties.jitterStartIndex
}

fun buildMixTapeGroups(tracks: List<Track>, groupSize: Int = DEFAULT_SONGS_PER_MIXTAPE): List<MixTapeGroup> =
    buildMixTapeGroups(tracks, MixtapeSettings(songsPerMixTape = groupSize))

fun buildMixTapeGroups(tracks: List<Track>, settings: MixtapeSettings): List<MixTapeGroup> {
    val groupSize = settings.songsPerMixTape
    require(groupSize > 0) { "groupSize must be positive" }
    val groupedTracks = when (settings.artistGrouping) {
        ArtistGrouping.NoGrouping -> tracks.chunked(groupSize)
        ArtistGrouping.KeepArtistTogether -> tracks.packArtistsTogether(groupSize)
        ArtistGrouping.ArtistTripletsAcrossTapes -> tracks.packArtistTripletsAcrossTapes(groupSize)
    }
    var startIndex = 0
    return groupedTracks.mapIndexed { index, groupTracks ->
        MixTapeGroup(
            name = "Mix Tape ${index + 1}",
            tracks = groupTracks,
            startIndex = startIndex,
        ).also { startIndex += groupTracks.size }
    }
}

private fun List<Track>.packArtistsTogether(groupSize: Int): List<List<Track>> {
    if (isEmpty()) return emptyList()
    val tapes = mutableListOf<MutableList<Track>>()

    groupBy { it.artist }.values.forEach { artistTracks ->
        artistTracks.chunked(groupSize).forEach { block ->
            val tape = if (block.size == groupSize) {
                null
            } else {
                tapes.firstOrNull { it.size + block.size <= groupSize }
            }

            if (tape == null) {
                tapes += block.toMutableList()
            } else {
                tape.addAll(block)
            }
        }
    }

    return tapes
}

private fun List<Track>.packArtistTripletsAcrossTapes(groupSize: Int): List<List<Track>> {
    if (isEmpty()) return emptyList()
    val chunkSize = minOf(3, groupSize)
    val artistChunkQueues = groupBy { it.artist }
        .values
        .map { artistTracks -> artistTracks.chunked(chunkSize).toMutableList() }
        .toMutableList()
    val orderedTracks = mutableListOf<Track>()

    while (artistChunkQueues.any { it.isNotEmpty() }) {
        artistChunkQueues.forEach { queue ->
            if (queue.isNotEmpty()) {
                orderedTracks += queue.removeAt(0)
            }
        }
    }

    return orderedTracks.chunked(groupSize)
}

data class MixtapeCustomization(
    val name: String,
    val decorativeId: String,
    val handwritingFont: MixtapeHandwritingFont,
    val embellishment: MixtapeEmbellishment,
    val symbolColor: MixtapeSymbolColor,
    val nameColor: MixtapeSymbolColor,
    val cassetteTheme: CassetteTheme,
    val screwTheme: ScrewTheme,
    val stickerTheme: StickerTheme,
    val caseTheme: CaseTheme,
    val sleeveTheme: SleeveTheme,
)

data class MixtapeNameInfo(
    val stableKey: String,
    val name: String,
)

data class MixtapeUiState(
    val status: LibraryStatus = LibraryStatus.PermissionRequired,
    val screen: MixtapeScreen = MixtapeScreen.MixTapes,
    val tracks: List<Track> = emptyList(),
    val mixTapeGroups: List<MixTapeGroup> = emptyList(),
    val queueTracks: List<Track> = emptyList(),
    val currentTrack: Track? = null,
    val selectedTrackInfo: Track? = null,
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val mixtapeSettings: MixtapeSettings = MixtapeSettings(),
    val currentMixtapeStableKey: String? = null,
    val currentMixtapeIndex: Int = -1,
    val currentMixtapeName: String? = null,
    val currentMixtapeVisualProperties: MixtapeVisualProperties = MixtapeVisualProperties(),
    val mixtapeSymbolSettings: MixtapeSymbolSettings = MixtapeSymbolSettings(),
    val mixtapeTapeSkinSettings: MixtapeTapeSkinSettings = MixtapeTapeSkinSettings(),
    val mixtapeSpineSkinSettings: MixtapeSpineSkinSettings = MixtapeSpineSkinSettings(),
    val mixtapeHandwritingFontSettings: MixtapeHandwritingFontSettings = MixtapeHandwritingFontSettings(),
    val mixtapeThemeSettings: MixtapeThemeSettings = MixtapeThemeSettings(),
    val mixtapeExclusionSettings: MixtapeExclusionSettings = MixtapeExclusionSettings(),
    val mixtapeNameInfos: List<MixtapeNameInfo> = emptyList(),
    val message: String = "Allow audio access to build your mixtape.",
) {
    val enabledMixtapeEmbellishments: Set<MixtapeEmbellishment> get() = mixtapeSymbolSettings.enabledEmbellishments
    val enabledMixtapeTapeSkins: Set<MixtapeTapeSkin> get() = mixtapeTapeSkinSettings.enabledTapeSkins
    val enabledMixtapeSpineSkins: Set<MixtapeSpineSkin> get() = mixtapeSpineSkinSettings.enabledSpineSkins
    val enabledMixtapeHandwritingFonts: Set<MixtapeHandwritingFont> get() = mixtapeHandwritingFontSettings.enabledHandwritingFonts
    val currentMixtapeHandwritingFont: MixtapeHandwritingFont get() = currentMixtapeVisualProperties.handwritingFont
    val currentMixtapeJitterStartIndex: Int get() = currentMixtapeVisualProperties.jitterStartIndex
    val isPermissionRequired: Boolean = status == LibraryStatus.PermissionRequired
    val isLoading: Boolean = status == LibraryStatus.Loading
}

private data class PendingDelete(
    val track: Track,
    val intentSender: IntentSender,
)

class MixtapeViewModel(
    private val repository: AudioRepository,
    private val controller: MixtapeController,
    private val mixtapeRandom: Random = Random.Default,
    private val nameSource: MixtapeNameSource = MixtapeNameSource.Empty,
    private val nameStore: MixtapeNameStore = InMemoryMixtapeNameStore(),
    private val visualPropertiesStore: MixtapeVisualPropertiesStore = InMemoryMixtapeVisualPropertiesStore(),
    private val settingsStore: MixtapeSettingsStore = InMemoryMixtapeSettingsStore(),
    private val symbolSettingsStore: MixtapeSymbolSettingsStore = InMemoryMixtapeSymbolSettingsStore(),
    private val tapeSkinSettingsStore: MixtapeTapeSkinSettingsStore = InMemoryMixtapeTapeSkinSettingsStore(),
    private val spineSkinSettingsStore: MixtapeSpineSkinSettingsStore = InMemoryMixtapeSpineSkinSettingsStore(),
    private val handwritingFontSettingsStore: MixtapeHandwritingFontSettingsStore = InMemoryMixtapeHandwritingFontSettingsStore(),
    private val themeSettingsStore: MixtapeThemeSettingsStore = InMemoryMixtapeThemeSettingsStore(),
    private val exclusionSettingsStore: MixtapeExclusionSettingsStore = InMemoryMixtapeExclusionSettingsStore(),
    private val transportCuePlayer: TransportCuePlayer = SilentTransportCuePlayer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MixtapeUiState())
    val uiState: StateFlow<MixtapeUiState> = _uiState.asStateFlow()
    var onDeleteTrackUserActionRequired: ((IntentSender) -> Unit)? = null
    private var pendingDelete: PendingDelete? = null
    private var rawLibraryTracks: List<Track> = emptyList()
    private var libraryTracks: List<Track> = emptyList()
    private var mixtapeTracks: List<Track> = emptyList()
    private var explicitMixtapeGroups: List<List<Track>>? = null
    private var mixtapeSettings: MixtapeSettings = settingsStore.settings()
    private var mixtapeSymbolSettings: MixtapeSymbolSettings = symbolSettingsStore.settings()
    private var mixtapeTapeSkinSettings: MixtapeTapeSkinSettings = tapeSkinSettingsStore.settings()
    private var mixtapeSpineSkinSettings: MixtapeSpineSkinSettings = spineSkinSettingsStore.settings()
    private var mixtapeHandwritingFontSettings: MixtapeHandwritingFontSettings = handwritingFontSettingsStore.settings()
    private var mixtapeThemeSettings: MixtapeThemeSettings = themeSettingsStore.settings()
    private var mixtapeExclusionSettings: MixtapeExclusionSettings = exclusionSettingsStore.settings()
    private var currentMixtapeStableKey: String? = null
    private var currentMixtapeVisualProperties: MixtapeVisualProperties = MixtapeVisualProperties()
    private var trackJumpCueJob: Job? = null
    private val assignedMixtapeNames = mutableMapOf<String, String>()

    init {
        _uiState.value = _uiState.value.copy(
            mixtapeSettings = mixtapeSettings,
            mixtapeSymbolSettings = mixtapeSymbolSettings,
            mixtapeSpineSkinSettings = mixtapeSpineSkinSettings,
            mixtapeTapeSkinSettings = mixtapeTapeSkinSettings,
            mixtapeHandwritingFontSettings = mixtapeHandwritingFontSettings,
            mixtapeThemeSettings = mixtapeThemeSettings,
            mixtapeExclusionSettings = mixtapeExclusionSettings,
        )
        controller.setOnPlaybackStateChanged { refreshCurrentUiState() }
    }

    fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.value = MixtapeUiState(
                status = LibraryStatus.PermissionRequired,
                mixtapeSymbolSettings = mixtapeSymbolSettings,
                mixtapeSpineSkinSettings = mixtapeSpineSkinSettings,
                mixtapeTapeSkinSettings = mixtapeTapeSkinSettings,
                mixtapeHandwritingFontSettings = mixtapeHandwritingFontSettings,
                mixtapeExclusionSettings = mixtapeExclusionSettings,
                message = "Mixtape needs audio permission to find songs on this device.",
            )
            return
        }

        if (_uiState.value.status in setOf(LibraryStatus.Loading, LibraryStatus.Empty, LibraryStatus.Ready)) return

        refresh()
    }

    fun refresh() {
        assignedMixtapeNames.clear()
        _uiState.value = _uiState.value.copy(status = LibraryStatus.Loading, message = "Scanning device audio…")
        viewModelScope.launch {
            runCatching { repository.loadTracks() }
                .onSuccess { tracks ->
                    rawLibraryTracks = tracks
                    explicitMixtapeGroups = null
                    rebuildFilteredTracks(preserveMixtapeOrder = false)
                    controller.load(libraryTracks)
                    if (tracks.isEmpty()) {
                        _uiState.value = MixtapeUiState(
                            status = LibraryStatus.Empty,
                            mixtapeSymbolSettings = mixtapeSymbolSettings,
                            mixtapeTapeSkinSettings = mixtapeTapeSkinSettings,
                            mixtapeSpineSkinSettings = mixtapeSpineSkinSettings,
                            mixtapeHandwritingFontSettings = mixtapeHandwritingFontSettings,
                            mixtapeExclusionSettings = mixtapeExclusionSettings,
                            message = "No audio files found. Add music to the device, then refresh.",
                        )
                    } else if (libraryTracks.isEmpty()) {
                        _uiState.value = controller.toUiState(
                            status = LibraryStatus.Ready,
                            screen = MixtapeScreen.MixTapes,
                            message = "All audio files are hidden by exclusion settings.",
                        )
                    } else {
                        val mixTapeCount = buildBaseMixTapeGroups().size
                        _uiState.value = controller.toUiState(
                            status = LibraryStatus.Ready,
                            screen = MixtapeScreen.MixTapes,
                            message = "$mixTapeCount mix tapes ready",
                        )
                        assignNamesForCurrentGroups()
                    }
                }
                .onFailure { error ->
                    _uiState.value = MixtapeUiState(
                        status = LibraryStatus.Error,
                        mixtapeSymbolSettings = mixtapeSymbolSettings,
                        mixtapeTapeSkinSettings = mixtapeTapeSkinSettings,
                        mixtapeSpineSkinSettings = mixtapeSpineSkinSettings,
                        mixtapeHandwritingFontSettings = mixtapeHandwritingFontSettings,
                        mixtapeExclusionSettings = mixtapeExclusionSettings,
                        message = error.message ?: "Could not scan audio. Check permission and try refresh.",
                    )
                }
        }
    }

    fun showMixTapes() {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.MixTapes,
            message = "${buildBaseMixTapeGroups().size} mix tapes ready",
        )
    }

    fun showSettings() {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.Settings,
            message = "Manage your mixtape collection",
        )
    }

    fun showHelp() {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.Help,
            message = "Android Auto setup help",
        )
    }

    fun showMixtapeNames() {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.MixtapeNames,
            message = "Manage mixtape names",
        )
    }

    fun showMixtapeSymbolSettings() {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.MixtapeSymbolSettings,
            message = "Choose mixtape symbols",
        )
    }

    fun showTapeSkinSettings() {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.TapeSkinSettings,
            message = "Choose tape skins",
        )
    }

    fun showSpineSkinSettings() {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.SpineSkinSettings,
            message = "Choose spine skins",
        )
    }

    fun showHandwritingFontSettings() {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.HandwritingFontSettings,
            message = "Choose handwriting fonts",
        )
    }

    fun showDeckThemeSettings() = showThemeSettings(MixtapeScreen.DeckThemeSettings, "Choose deck theme")
    fun showCassetteThemeSettings() = showThemeSettings(MixtapeScreen.CassetteThemeSettings, "Choose cassette themes")
    fun showScrewThemeSettings() = showThemeSettings(MixtapeScreen.ScrewThemeSettings, "Choose screw themes")
    fun showStickerThemeSettings() = showThemeSettings(MixtapeScreen.StickerThemeSettings, "Choose sticker themes")
    fun showCaseThemeSettings() = showThemeSettings(MixtapeScreen.CaseThemeSettings, "Choose case themes")
    fun showSleeveThemeSettings() = showThemeSettings(MixtapeScreen.SleeveThemeSettings, "Choose sleeve themes")

    private fun showThemeSettings(screen: MixtapeScreen, message: String) {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = screen,
            message = message,
        )
    }

    fun showExclusionSettings() {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.ExclusionSettings,
            message = "Manage filename exclusions",
        )
    }

    fun addFilenameExclusionPattern(pattern: String) {
        val updatedSettings = MixtapeExclusionSettings.normalized(
            mixtapeExclusionSettings.filenamePatterns + pattern,
        )
        if (updatedSettings == mixtapeExclusionSettings) return
        mixtapeExclusionSettings = updatedSettings
        exclusionSettingsStore.saveSettings(mixtapeExclusionSettings)
        applyExclusionSettingsAndRefresh("Updated filename exclusions")
    }

    fun removeFilenameExclusionPattern(pattern: String) {
        val trimmedPattern = pattern.trim()
        val updatedSettings = MixtapeExclusionSettings.normalized(
            mixtapeExclusionSettings.filenamePatterns.filterNot { it == trimmedPattern },
        )
        if (updatedSettings == mixtapeExclusionSettings) return
        mixtapeExclusionSettings = updatedSettings
        exclusionSettingsStore.saveSettings(mixtapeExclusionSettings)
        applyExclusionSettingsAndRefresh("Updated filename exclusions")
    }

    fun updateMixtapeEmbellishmentEnabled(embellishment: MixtapeEmbellishment, enabled: Boolean) {
        val currentlyEnabled = mixtapeSymbolSettings.enabledEmbellishments
        val updated = when {
            enabled -> currentlyEnabled + embellishment
            embellishment !in currentlyEnabled -> currentlyEnabled
            currentlyEnabled.size <= 1 -> currentlyEnabled
            else -> currentlyEnabled - embellishment
        }
        if (updated == currentlyEnabled) return

        mixtapeSymbolSettings = MixtapeSymbolSettings.normalized(updated)
        symbolSettingsStore.saveSettings(mixtapeSymbolSettings)
        refreshCurrentUiState()
    }

    fun updateMixtapeTapeSkinEnabled(tapeSkin: MixtapeTapeSkin, enabled: Boolean) {
        val enabledTapeSkins = mixtapeTapeSkinSettings.enabledTapeSkins
        val updated = when {
            enabled -> enabledTapeSkins + tapeSkin
            tapeSkin !in enabledTapeSkins -> enabledTapeSkins
            enabledTapeSkins.size > 1 -> enabledTapeSkins - tapeSkin
            else -> enabledTapeSkins
        }
        if (updated == enabledTapeSkins) return

        mixtapeTapeSkinSettings = MixtapeTapeSkinSettings.normalized(updated)
        tapeSkinSettingsStore.saveSettings(mixtapeTapeSkinSettings)
        refreshCurrentUiState()
    }

    fun updateMixtapeSpineSkinEnabled(spineSkin: MixtapeSpineSkin, enabled: Boolean) {
        val enabledSpineSkins = mixtapeSpineSkinSettings.enabledSpineSkins
        val updated = when {
            enabled -> enabledSpineSkins + spineSkin
            spineSkin !in enabledSpineSkins -> enabledSpineSkins
            enabledSpineSkins.size > 1 -> enabledSpineSkins - spineSkin
            else -> enabledSpineSkins
        }
        if (updated == enabledSpineSkins) return

        mixtapeSpineSkinSettings = MixtapeSpineSkinSettings.normalized(updated)
        spineSkinSettingsStore.saveSettings(mixtapeSpineSkinSettings)
        refreshCurrentUiState()
    }

    fun updateMixtapeHandwritingFontEnabled(handwritingFont: MixtapeHandwritingFont, enabled: Boolean) {
        val enabledHandwritingFonts = mixtapeHandwritingFontSettings.enabledHandwritingFonts
        val updated = when {
            enabled -> enabledHandwritingFonts + handwritingFont
            handwritingFont !in enabledHandwritingFonts -> enabledHandwritingFonts
            enabledHandwritingFonts.size > 1 -> enabledHandwritingFonts - handwritingFont
            else -> enabledHandwritingFonts
        }
        if (updated == enabledHandwritingFonts) return

        mixtapeHandwritingFontSettings = MixtapeHandwritingFontSettings.normalized(updated)
        handwritingFontSettingsStore.saveSettings(mixtapeHandwritingFontSettings)
        refreshCurrentUiState()
    }

    fun updateDeckTheme(theme: DeckTheme) {
        mixtapeThemeSettings = mixtapeThemeSettings.copy(deckTheme = theme)
        themeSettingsStore.saveSettings(mixtapeThemeSettings)
        refreshCurrentUiState()
    }

    fun updateCassetteThemeEnabled(theme: CassetteTheme, enabled: Boolean) {
        mixtapeThemeSettings = mixtapeThemeSettings.copy(
            enabledCassetteThemes = toggleTheme(mixtapeThemeSettings.enabledCassetteThemes, theme, enabled),
        )
        saveThemeSettings()
    }

    fun updateScrewThemeEnabled(theme: ScrewTheme, enabled: Boolean) {
        mixtapeThemeSettings = mixtapeThemeSettings.copy(
            enabledScrewThemes = toggleTheme(mixtapeThemeSettings.enabledScrewThemes, theme, enabled),
        )
        saveThemeSettings()
    }

    fun updateStickerThemeEnabled(theme: StickerTheme, enabled: Boolean) {
        mixtapeThemeSettings = mixtapeThemeSettings.copy(
            enabledStickerThemes = toggleTheme(mixtapeThemeSettings.enabledStickerThemes, theme, enabled),
        )
        saveThemeSettings()
    }

    fun updateCaseThemeEnabled(theme: CaseTheme, enabled: Boolean) {
        mixtapeThemeSettings = mixtapeThemeSettings.copy(
            enabledCaseThemes = toggleTheme(mixtapeThemeSettings.enabledCaseThemes, theme, enabled),
        )
        saveThemeSettings()
    }

    fun updateSleeveThemeEnabled(theme: SleeveTheme, enabled: Boolean) {
        mixtapeThemeSettings = mixtapeThemeSettings.copy(
            enabledSleeveThemes = toggleTheme(mixtapeThemeSettings.enabledSleeveThemes, theme, enabled),
        )
        saveThemeSettings()
    }

    private fun <T> toggleTheme(current: Set<T>, theme: T, enabled: Boolean): Set<T> = when {
        enabled -> current + theme
        theme !in current -> current
        current.size <= 1 -> current
        else -> current - theme
    }

    private fun saveThemeSettings() {
        mixtapeThemeSettings = MixtapeThemeSettings.normalized(mixtapeThemeSettings)
        themeSettingsStore.saveSettings(mixtapeThemeSettings)
        refreshCurrentUiState()
    }

    fun updateCurrentMixtapeCustomization(customization: MixtapeCustomization) {
        val stableKey = currentMixtapeStableKey ?: return
        val updated = currentMixtapeVisualProperties.copy(
            decorativeId = customization.decorativeId.trim().ifBlank { randomDecorativeId() }.take(2),
            handwritingFont = customization.handwritingFont,
            embellishment = customization.embellishment,
            symbolColor = customization.symbolColor,
            nameColor = customization.nameColor,
            cassetteTheme = customization.cassetteTheme,
            screwTheme = customization.screwTheme,
            stickerTheme = customization.stickerTheme,
            caseTheme = customization.caseTheme,
            sleeveTheme = customization.sleeveTheme,
        )
        currentMixtapeVisualProperties = updated
        visualPropertiesStore.saveProperties(stableKey, updated)
        editMixtapeName(stableKey, customization.name)
        refreshCurrentUiState()
    }

    fun cycleCurrentMixtapeSymbolColor() {
        val stableKey = currentMixtapeStableKey ?: return
        if (!controller.state.isPlaying) return

        val updatedProperties = currentMixtapeVisualProperties.copy(
            symbolColor = currentMixtapeVisualProperties.symbolColor.next(),
        )
        currentMixtapeVisualProperties = updatedProperties
        visualPropertiesStore.saveProperties(stableKey, updatedProperties)
        refreshCurrentUiState()
    }

    fun selectCurrentMixtapeSymbolColor(symbolColor: MixtapeSymbolColor) {
        val stableKey = currentMixtapeStableKey ?: return

        val updatedProperties = currentMixtapeVisualProperties.copy(symbolColor = symbolColor)
        currentMixtapeVisualProperties = updatedProperties
        visualPropertiesStore.saveProperties(stableKey, updatedProperties)
        refreshCurrentUiState()
    }

    fun cycleCurrentMixtapeNameColor() {
        val stableKey = currentMixtapeStableKey ?: return
        if (!controller.state.isPlaying) return

        val updatedProperties = currentMixtapeVisualProperties.copy(
            nameColor = currentMixtapeVisualProperties.nameColor.next(),
        )
        currentMixtapeVisualProperties = updatedProperties
        visualPropertiesStore.saveProperties(stableKey, updatedProperties)
        refreshCurrentUiState()
    }

    fun selectCurrentMixtapeEmbellishment(embellishment: MixtapeEmbellishment) {
        val stableKey = currentMixtapeStableKey ?: return
        if (embellishment !in mixtapeSymbolSettings.enabledEmbellishments) return

        val updatedProperties = currentMixtapeVisualProperties.copy(embellishment = embellishment)
        currentMixtapeVisualProperties = updatedProperties
        visualPropertiesStore.saveProperties(stableKey, updatedProperties)
        refreshCurrentUiState()
    }

    fun resetAllMixTapes() {
        mixtapeTracks = libraryTracks.shuffled(mixtapeRandom)
        explicitMixtapeGroups = null
        assignedMixtapeNames.clear()
        currentMixtapeVisualProperties = MixtapeVisualProperties()
        controller.stop()
        controller.load(emptyList())
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.MixTapes,
            message = "${buildBaseMixTapeGroups().size} mix tapes reset",
        )
        assignNamesForCurrentGroups(force = true)
    }

    fun backToMixTapes() {
        showMixTapes()
    }

    fun eject() {
        trackJumpCueJob?.cancel()
        transportCuePlayer.cancel()
        controller.stop()
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.MixTapes,
            message = "${buildBaseMixTapeGroups().size} mix tapes ready",
        )
    }

    fun selectMixTapeGroup(index: Int) {
        val group = buildAssignedMixTapeGroups().getOrNull(index) ?: return
        currentMixtapeStableKey = group.stableMixtapeKey()
        currentMixtapeVisualProperties = group.visualProperties
        controller.load(group.tracks)
        controller.select(0)
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.NowPlaying,
            message = "Playing ${group.name}",
        )
    }

    fun showTrackInfo(trackId: Long) {
        val track = controller.state.tracks.firstOrNull { it.id == trackId }
            ?: mixtapeTracks.firstOrNull { it.id == trackId }
            ?: libraryTracks.firstOrNull { it.id == trackId }
            ?: return
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = MixtapeScreen.TrackInfo,
            message = "Track info for ${track.title}",
            selectedTrackInfo = track,
        )
    }

    fun backFromTrackInfo() {
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = if (controller.state.tracks.isEmpty()) MixtapeScreen.MixTapes else MixtapeScreen.NowPlaying,
            message = _uiState.value.message,
        )
    }

    fun removeTrackFromCurrentMixtape(trackId: Long) {
        val currentGroups = buildBaseMixTapeGroups()
        val currentTapeIndex = currentMixtapeStableKey
            ?.let { stableKey -> currentGroups.indexOfFirst { it.stableMixtapeKey() == stableKey } }
            ?.takeIf { it >= 0 }
            ?: currentGroups.indexOfFirst { group -> group.tracks.any { it.id == trackId } }
        val currentGroup = currentGroups.getOrNull(currentTapeIndex) ?: return
        val removeIndex = currentGroup.tracks.indexOfFirst { it.id == trackId }
        if (removeIndex < 0) return

        val otherTapeCandidates = currentGroups.mapIndexedNotNull { index, group ->
            if (index != currentTapeIndex && group.tracks.isNotEmpty()) index to group else null
        }
        if (otherTapeCandidates.isEmpty()) {
            _uiState.value = controller.toUiState(
                status = LibraryStatus.Ready,
                screen = _uiState.value.screen,
                message = "No other mix tape available to receive ${currentGroup.tracks[removeIndex].title}",
            )
            return
        }

        val (otherTapeIndex, otherTape) = otherTapeCandidates.random(mixtapeRandom)
        val otherTrackIndex = otherTape.tracks.indices.random(mixtapeRandom)
        val updatedGroups = currentGroups.map { it.tracks.toMutableList() }.toMutableList()
        val removedTrack = updatedGroups[currentTapeIndex][removeIndex]
        val swapTrack = updatedGroups[otherTapeIndex][otherTrackIndex]
        updatedGroups[currentTapeIndex][removeIndex] = swapTrack
        updatedGroups[otherTapeIndex][otherTrackIndex] = removedTrack
        explicitMixtapeGroups = updatedGroups.map { it.toList() }

        val assignedGroups = buildAssignedMixTapeGroups()
        val openGroup = assignedGroups.getOrNull(currentTapeIndex)
        currentMixtapeStableKey = openGroup?.stableMixtapeKey()
        openGroup?.let { currentMixtapeVisualProperties = it.visualProperties }
        val queueTracks = openGroup?.tracks.orEmpty()
        controller.load(queueTracks)
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = if (queueTracks.isEmpty()) MixtapeScreen.MixTapes else MixtapeScreen.NowPlaying,
            message = "Removed ${removedTrack.title} from this mix tape",
            selectedTrackInfo = _uiState.value.selectedTrackInfo?.takeUnless { it.id == trackId },
        )
    }

    fun deleteTrackFromDevice(trackId: Long) {
        val track = libraryTracks.firstOrNull { it.id == trackId }
            ?: rawLibraryTracks.firstOrNull { it.id == trackId }
            ?: mixtapeTracks.firstOrNull { it.id == trackId }
            ?: controller.state.tracks.firstOrNull { it.id == trackId }
            ?: return
        viewModelScope.launch {
            when (val result = repository.deleteTrack(track)) {
                DeleteTrackResult.Success -> {
                    pendingDelete = null
                    rawLibraryTracks = rawLibraryTracks.filterNot { it.matchesDeleteTarget(track) }
                    libraryTracks = libraryTracks.filterNot { it.matchesDeleteTarget(track) }
                    mixtapeTracks = mixtapeTracks.filterNot { it.matchesDeleteTarget(track) }
                    removeDeletedTrackFromAppState(track, "Deleted ${track.title} from device")
                }
                is DeleteTrackResult.RequiresUserAction -> {
                    pendingDelete = PendingDelete(track, result.intentSender)
                    _uiState.value = controller.toUiState(
                        status = LibraryStatus.Ready,
                        screen = _uiState.value.screen,
                        message = "Confirm Android's delete prompt to remove ${track.title} from this device",
                    )
                    onDeleteTrackUserActionRequired?.invoke(result.intentSender)
                    return@launch
                }
                is DeleteTrackResult.Failure -> {
                    pendingDelete = null
                    _uiState.value = controller.toUiState(
                        status = LibraryStatus.Ready,
                        screen = _uiState.value.screen,
                        message = "Could not delete ${track.title}: ${result.message}",
                    )
                }
            }
        }
    }

    fun confirmTrackDeletedFromDevice() {
        val confirmedDelete = pendingDelete ?: return
        pendingDelete = null
        viewModelScope.launch {
            runCatching { repository.loadTracks() }
                .onSuccess { refreshedTracks ->
                    rawLibraryTracks = refreshedTracks.filterNot { it.matchesDeleteTarget(confirmedDelete.track) }
                    libraryTracks = libraryTracks.filterNot { it.matchesDeleteTarget(confirmedDelete.track) }
                    mixtapeTracks = mixtapeTracks.filterNot { it.matchesDeleteTarget(confirmedDelete.track) }
                }
            removeDeletedTrackFromAppState(confirmedDelete.track, "Deleted ${confirmedDelete.track.title} from device")
        }
    }

    fun cancelPendingTrackDeleteFromDevice() {
        val cancelledDelete = pendingDelete ?: return
        pendingDelete = null
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = _uiState.value.screen,
            message = "Did not delete ${cancelledDelete.track.title} from device",
        )
    }

    fun failPendingTrackDeleteFromDevice(message: String) {
        val failedDelete = pendingDelete ?: return
        pendingDelete = null
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = _uiState.value.screen,
            message = "Could not delete ${failedDelete.track.title}: $message",
        )
    }

    fun updateSongsPerMixTape(songsPerMixTape: Int) {
        if (songsPerMixTape !in MIXTAPE_TRACK_COUNT_OPTIONS || songsPerMixTape == mixtapeSettings.songsPerMixTape) return
        mixtapeSettings = mixtapeSettings.copy(songsPerMixTape = songsPerMixTape)
        explicitMixtapeGroups = null
        settingsStore.saveSettings(mixtapeSettings)
        assignedMixtapeNames.clear()
        refreshMixtapeSettingsState("${buildBaseMixTapeGroups().size} mix tapes ready")
        assignNamesForCurrentGroups()
    }

    fun updateArtistGrouping(artistGrouping: ArtistGrouping) {
        if (artistGrouping == mixtapeSettings.artistGrouping) return
        mixtapeSettings = mixtapeSettings.copy(artistGrouping = artistGrouping)
        explicitMixtapeGroups = null
        settingsStore.saveSettings(mixtapeSettings)
        assignedMixtapeNames.clear()
        refreshMixtapeSettingsState("${buildBaseMixTapeGroups().size} mix tapes ready")
        assignNamesForCurrentGroups()
    }

    fun updateHandwritingMessiness(handwritingMessiness: HandwritingMessiness) {
        if (handwritingMessiness == mixtapeSettings.handwritingMessiness) return
        mixtapeSettings = mixtapeSettings.copy(handwritingMessiness = handwritingMessiness)
        settingsStore.saveSettings(mixtapeSettings)
        refreshMixtapeSettingsState(_uiState.value.message)
    }

    fun togglePlayPause() {
        controller.togglePlayPause()
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = _uiState.value.screen,
            message = _uiState.value.message,
        )
    }

    fun next() {
        controller.next()
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = _uiState.value.screen,
            message = _uiState.value.message,
        )
    }

    fun previous() {
        controller.previous()
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = _uiState.value.screen,
            message = _uiState.value.message,
        )
    }

    fun seekTo(positionMs: Long) {
        controller.seekTo(positionMs)
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = _uiState.value.screen,
            message = _uiState.value.message,
        )
    }

    fun stop() {
        trackJumpCueJob?.cancel()
        transportCuePlayer.cancel()
        controller.stop()
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = _uiState.value.screen,
            message = _uiState.value.message,
        )
    }

    fun jumpToTrackWithCue(targetIndex: Int, track: Track) {
        val currentIndex = controller.state.currentIndex
        val targetTrack = controller.state.tracks.getOrNull(targetIndex)?.takeIf { it.matchesDeleteTarget(track) } ?: return
        val direction = when {
            targetIndex > currentIndex -> TransportCueDirection.FAST_FORWARD
            targetIndex < currentIndex -> TransportCueDirection.REWIND
            targetIndex == currentIndex -> TransportCueDirection.NONE
            else -> TransportCueDirection.NONE
        }
        trackJumpCueJob?.cancel()
        transportCuePlayer.cancel()
        controller.stop()
        _uiState.value = controller.toUiState(
            status = LibraryStatus.Ready,
            screen = _uiState.value.screen,
            message = transportCueMessage(direction, targetTrack),
        )
        trackJumpCueJob = viewModelScope.launch {
            try {
                val cueDurationMs = if (direction == TransportCueDirection.NONE) 0L else 5_000L
                transportCuePlayer.play(direction, cueDurationMs)
                if (controller.state.tracks.getOrNull(targetIndex)?.matchesDeleteTarget(targetTrack) == true) {
                    controller.select(targetIndex)
                    _uiState.value = controller.toUiState(
                        status = LibraryStatus.Ready,
                        screen = _uiState.value.screen,
                        message = "Playing ${targetTrack.title}",
                    )
                } else {
                    _uiState.value = controller.toUiState(
                        status = LibraryStatus.Ready,
                        screen = _uiState.value.screen,
                        message = "${targetTrack.title} is no longer in this mix tape",
                    )
                }
            } catch (error: CancellationException) {
                transportCuePlayer.cancel()
                throw error
            }
        }
    }

    override fun onCleared() {
        trackJumpCueJob?.cancel()
        transportCuePlayer.cancel()
        transportCuePlayer.release()
        onDeleteTrackUserActionRequired = null
        controller.setOnPlaybackStateChanged(null)
        controller.release()
    }

    private fun transportCueMessage(direction: TransportCueDirection, track: Track): String = when (direction) {
        TransportCueDirection.FAST_FORWARD -> "Fast-forwarding to ${track.title}…"
        TransportCueDirection.REWIND -> "Rewinding to ${track.title}…"
        TransportCueDirection.NONE -> "Restarting ${track.title}…"
    }

    private fun removeDeletedTrackFromAppState(track: Track, message: String) {
        val previousState = _uiState.value
        val oldPlaybackState = controller.state
        val removedCurrentTrack = oldPlaybackState.currentTrack?.matchesDeleteTarget(track) == true
        val oldStableKey = currentMixtapeStableKey
        val oldVisualProperties = currentMixtapeVisualProperties

        rawLibraryTracks = rawLibraryTracks.filterNot { it.matchesDeleteTarget(track) }
        libraryTracks = libraryTracks.filterNot { it.matchesDeleteTarget(track) }
        mixtapeTracks = mixtapeTracks.filterNot { it.matchesDeleteTarget(track) }
        explicitMixtapeGroups = explicitMixtapeGroups?.map { group -> group.filterNot { it.matchesDeleteTarget(track) } }

        val assignedGroups = buildAssignedMixTapeGroups()
        val openGroup = assignedGroups.getOrNull(previousState.currentMixtapeIndex)
            ?: oldPlaybackState.currentTrack
                ?.takeUnless { it.matchesDeleteTarget(track) }
                ?.let { current -> assignedGroups.firstOrNull { group -> group.tracks.any { it.matchesDeleteTarget(current) } } }
            ?: assignedGroups.firstOrNull()
        val nextStableKey = openGroup?.stableMixtapeKey()
        if (oldStableKey != null && nextStableKey != null && oldStableKey != nextStableKey) {
            carryMixtapeIdentity(oldStableKey, nextStableKey, oldVisualProperties)
        }
        currentMixtapeStableKey = nextStableKey
        openGroup?.let { currentMixtapeVisualProperties = it.visualProperties }

        val queueTracks = openGroup?.tracks.orEmpty()
        if (removedCurrentTrack) {
            controller.replaceQueueAfterCurrentRemoval(
                tracks = queueTracks,
                nextIndex = oldPlaybackState.currentIndex.coerceAtMost(queueTracks.lastIndex),
                playNext = false,
            )
        } else {
            controller.replaceQueuePreservingCurrentTrack(queueTracks)
        }
        _uiState.value = controller.toUiState(
            status = if (libraryTracks.isEmpty()) LibraryStatus.Empty else LibraryStatus.Ready,
            screen = if (queueTracks.isEmpty()) MixtapeScreen.MixTapes else previousState.screen,
            message = message,
            selectedTrackInfo = previousState.selectedTrackInfo?.takeUnless { it.matchesDeleteTarget(track) },
        )
    }

    private fun removeTrackFromCurrentMixtapeOnly(trackId: Long, message: String, deletedTrack: Track? = null) {
        val currentGroups = buildBaseMixTapeGroups()
        val currentTapeIndex = currentMixtapeStableKey
            ?.let { stableKey -> currentGroups.indexOfFirst { it.stableMixtapeKey() == stableKey } }
            ?.takeIf { it >= 0 }
            ?: currentGroups.indexOfFirst { group -> group.tracks.any { it.id == trackId } }
        if (currentTapeIndex !in currentGroups.indices) return

        val currentGroup = currentGroups[currentTapeIndex]
        val removeTarget = deletedTrack ?: currentGroup.tracks.firstOrNull { it.id == trackId } ?: return
        if (currentGroup.tracks.none { it.matchesDeleteTarget(removeTarget) }) return

        val oldStableKey = currentGroup.stableMixtapeKey()
        val oldVisualProperties = currentMixtapeVisualProperties
        val oldPlaybackState = controller.state
        val updatedGroupTracks = currentGroup.tracks.filterNot { it.matchesDeleteTarget(removeTarget) }
        val updatedGroups = currentGroups.mapIndexed { index, group ->
            if (index == currentTapeIndex) updatedGroupTracks else group.tracks.filterNot { it.matchesDeleteTarget(removeTarget) }
        }
        explicitMixtapeGroups = updatedGroups

        val assignedGroups = buildAssignedMixTapeGroups()
        val openGroup = assignedGroups.getOrNull(currentTapeIndex)
        val nextStableKey = openGroup?.stableMixtapeKey()
        if (nextStableKey != null && nextStableKey != oldStableKey) {
            carryMixtapeIdentity(oldStableKey, nextStableKey, oldVisualProperties)
        }
        currentMixtapeStableKey = nextStableKey
        openGroup?.let { currentMixtapeVisualProperties = it.visualProperties }

        val queueTracks = openGroup?.tracks.orEmpty()
        replaceQueueAfterRemoval(queueTracks, removeTarget, oldPlaybackState)
        _uiState.value = controller.toUiState(
            status = if (libraryTracks.isEmpty()) LibraryStatus.Empty else LibraryStatus.Ready,
            screen = if (queueTracks.isEmpty()) MixtapeScreen.MixTapes else MixtapeScreen.NowPlaying,
            message = message,
            selectedTrackInfo = _uiState.value.selectedTrackInfo?.takeUnless { it.matchesDeleteTarget(removeTarget) },
        )
    }

    private fun applyExclusionSettingsAndRefresh(message: String) {
        assignedMixtapeNames.clear()
        val previousScreen = _uiState.value.screen
        val previousQueue = controller.state.tracks
        rebuildFilteredTracks(preserveMixtapeOrder = true)
        val assignedGroups = buildAssignedMixTapeGroups()
        val openGroup = currentMixtapeStableKey
            ?.let { stableKey -> assignedGroups.firstOrNull { it.stableMixtapeKey() == stableKey } }
            ?: assignedGroups.firstOrNull { group -> previousQueue.any { queued -> group.tracks.any { it.id == queued.id } } }
        currentMixtapeStableKey = openGroup?.stableMixtapeKey()
        openGroup?.let { currentMixtapeVisualProperties = it.visualProperties }
        val queueTracks = openGroup?.tracks.orEmpty()
        controller.load(if (previousScreen == MixtapeScreen.NowPlaying) queueTracks else emptyList())
        val nextScreen = when {
            previousScreen == MixtapeScreen.NowPlaying && queueTracks.isNotEmpty() -> MixtapeScreen.NowPlaying
            previousScreen == MixtapeScreen.ExclusionSettings -> MixtapeScreen.ExclusionSettings
            previousScreen == MixtapeScreen.Settings -> MixtapeScreen.Settings
            else -> MixtapeScreen.MixTapes
        }
        _uiState.value = controller.toUiState(
            status = if (rawLibraryTracks.isEmpty()) LibraryStatus.Empty else LibraryStatus.Ready,
            screen = nextScreen,
            message = message,
            selectedTrackInfo = _uiState.value.selectedTrackInfo?.takeIf { track -> libraryTracks.any { it.id == track.id } },
        )
        if (libraryTracks.isNotEmpty()) assignNamesForCurrentGroups()
    }

    private fun rebuildFilteredTracks(preserveMixtapeOrder: Boolean) {
        libraryTracks = rawLibraryTracks.filterNot { track ->
            FilenameExclusionMatcher.matches(track, mixtapeExclusionSettings.filenamePatterns)
        }
        if (!preserveMixtapeOrder || mixtapeTracks.isEmpty()) {
            mixtapeTracks = libraryTracks
            return
        }

        val eligibleIds = libraryTracks.map { it.id }.toSet()
        val orderedExistingTracks = mixtapeTracks.filter { it.id in eligibleIds }
        val orderedExistingIds = orderedExistingTracks.map { it.id }.toSet()
        val restoredTracks = libraryTracks.filterNot { it.id in orderedExistingIds }
        mixtapeTracks = orderedExistingTracks + restoredTracks
    }

    private fun Track.matchesDeleteTarget(deleteTarget: Track): Boolean =
        id == deleteTarget.id || uri == deleteTarget.uri

    private fun MixtapeController.toUiState(
        status: LibraryStatus,
        screen: MixtapeScreen,
        message: String,
        selectedTrackInfo: Track? = _uiState.value.selectedTrackInfo.takeIf { screen == MixtapeScreen.TrackInfo },
    ): MixtapeUiState {
        val assignedMixTapeGroups = buildAssignedMixTapeGroups()
        val selectedStableKey = currentMixtapeStableKey
        val selectedGroupIndex = selectedStableKey
            ?.let { stableKey -> assignedMixTapeGroups.indexOfFirst { it.stableMixtapeKey() == stableKey } }
            ?: -1
        val selectedGroup = assignedMixTapeGroups.getOrNull(selectedGroupIndex)
        val currentMixtapeName = selectedGroup?.name
        selectedGroup?.let { currentMixtapeVisualProperties = it.visualProperties }
        return MixtapeUiState(
            status = status,
            screen = screen,
            tracks = libraryTracks,
            mixTapeGroups = assignedMixTapeGroups,
            queueTracks = state.tracks,
            currentTrack = state.currentTrack,
            selectedTrackInfo = selectedTrackInfo,
            currentIndex = state.currentIndex,
            isPlaying = state.isPlaying,
            positionMs = state.positionMs,
            durationMs = state.currentTrack?.durationMs ?: state.durationMs,
            canGoPrevious = state.canGoPrevious,
            canGoNext = state.canGoNext,
            mixtapeSettings = mixtapeSettings,
            currentMixtapeStableKey = selectedStableKey,
            currentMixtapeIndex = selectedGroupIndex,
            currentMixtapeName = currentMixtapeName,
            currentMixtapeVisualProperties = currentMixtapeVisualProperties,
            mixtapeSymbolSettings = mixtapeSymbolSettings,
            mixtapeSpineSkinSettings = mixtapeSpineSkinSettings,
            mixtapeTapeSkinSettings = mixtapeTapeSkinSettings,
            mixtapeHandwritingFontSettings = mixtapeHandwritingFontSettings,
            mixtapeThemeSettings = mixtapeThemeSettings,
            mixtapeExclusionSettings = mixtapeExclusionSettings,
            mixtapeNameInfos = assignedMixTapeGroups.map { it.toMixtapeNameInfo() },
            message = message,
        )
    }

    private fun buildBaseMixTapeGroups(): List<MixTapeGroup> {
        explicitMixtapeGroups?.let { groups ->
            var startIndex = 0
            return groups.mapIndexed { index, tracks ->
                MixTapeGroup(
                    name = "Mix Tape ${index + 1}",
                    tracks = tracks,
                    startIndex = startIndex,
                ).also { startIndex += tracks.size }
            }
        }
        return buildMixTapeGroups(mixtapeTracks, mixtapeSettings)
    }

    private fun buildAssignedMixTapeGroups(): List<MixTapeGroup> =
        buildBaseMixTapeGroups().map { group ->
            val stableKey = group.stableMixtapeKey()
            group.copy(
                name = assignedMixtapeNames[stableKey]
                    ?: nameStore.nameFor(stableKey)
                    ?: group.name,
                visualProperties = visualPropertiesFor(stableKey),
            )
        }

    private fun carryMixtapeIdentity(oldStableKey: String, newStableKey: String, visualProperties: MixtapeVisualProperties) {
        val name = assignedMixtapeNames[oldStableKey] ?: nameStore.nameFor(oldStableKey)
        if (name != null) {
            assignedMixtapeNames[newStableKey] = name
            nameStore.saveName(newStableKey, name)
        }
        visualPropertiesStore.saveProperties(newStableKey, visualProperties)
    }

    private fun replaceQueueAfterRemoval(queueTracks: List<Track>, removedTrack: Track, oldPlaybackState: PlayerUiState) {
        val removedCurrentTrack = oldPlaybackState.currentTrack?.matchesDeleteTarget(removedTrack) == true
        if (!removedCurrentTrack) {
            controller.replaceQueuePreservingCurrentTrack(queueTracks)
            return
        }

        val nextIndex = oldPlaybackState.currentIndex
        if (nextIndex < queueTracks.size) {
            controller.replaceQueueAfterCurrentRemoval(queueTracks, nextIndex, playNext = oldPlaybackState.isPlaying)
        } else {
            controller.replaceQueueAfterCurrentRemoval(queueTracks, queueTracks.lastIndex, playNext = false)
            controller.stop()
        }
    }

    private fun visualPropertiesFor(stableMixtapeKey: String): MixtapeVisualProperties {
        val enabledEmbellishments = enabledEmbellishments()
        val enabledTapeSkins = enabledTapeSkins()
        val enabledSpineSkins = enabledSpineSkins()
        val enabledHandwritingFonts = enabledHandwritingFonts()
        visualPropertiesStore.propertiesFor(stableMixtapeKey)?.let { properties ->
            val migratedProperties = properties.copy(
                embellishment = properties.embellishment.takeIf { it in enabledEmbellishments } ?: enabledEmbellishments.first(),
                tapeSkin = properties.tapeSkin.takeIf { it in enabledTapeSkins } ?: enabledTapeSkins.first(),
                spineSkin = properties.spineSkin.takeIf { it in enabledSpineSkins } ?: enabledSpineSkins.first(),
                handwritingFont = properties.handwritingFont.takeIf { it in enabledHandwritingFonts } ?: enabledHandwritingFonts.first(),
            )
            if (migratedProperties != properties) {
                visualPropertiesStore.saveProperties(stableMixtapeKey, migratedProperties)
            }
            return migratedProperties
        }
        val properties = MixtapeVisualProperties(
            handwritingFont = enabledHandwritingFonts.random(mixtapeRandom),
            jitterStartIndex = mixtapeRandom.nextInt(HANDWRITING_PERTURBATION_COUNT),
            embellishment = enabledEmbellishments.random(mixtapeRandom),
            tapeSkin = enabledTapeSkins.random(mixtapeRandom),
            spineSkin = enabledSpineSkins.random(mixtapeRandom),
            decorativeId = randomDecorativeId(),
            cassetteTheme = mixtapeThemeSettings.enabledCassetteThemes.random(mixtapeRandom),
            screwTheme = mixtapeThemeSettings.enabledScrewThemes.random(mixtapeRandom),
            stickerTheme = mixtapeThemeSettings.enabledStickerThemes.random(mixtapeRandom),
            caseTheme = mixtapeThemeSettings.enabledCaseThemes.random(mixtapeRandom),
            sleeveTheme = mixtapeThemeSettings.enabledSleeveThemes.random(mixtapeRandom),
        )
        visualPropertiesStore.saveProperties(stableMixtapeKey, properties)
        return properties
    }

    private fun randomDecorativeId(): String {
        val value = mixtapeRandom.nextInt(126)
        return if (value < 26) ('A' + value).toString() else (value - 26).toString()
    }

    private fun enabledEmbellishments(): List<MixtapeEmbellishment> =
        MixtapeEmbellishment.entries.filter { it in mixtapeSymbolSettings.enabledEmbellishments }
            .ifEmpty { MixtapeEmbellishment.entries }

    private fun enabledTapeSkins(): List<MixtapeTapeSkin> =
        MixtapeTapeSkin.entries.filter { it in mixtapeTapeSkinSettings.enabledTapeSkins }
            .ifEmpty { MixtapeTapeSkin.entries }

    private fun enabledSpineSkins(): List<MixtapeSpineSkin> =
        MixtapeSpineSkin.entries.filter { it in mixtapeSpineSkinSettings.enabledSpineSkins }
            .ifEmpty { MixtapeSpineSkin.entries }

    private fun enabledHandwritingFonts(): List<MixtapeHandwritingFont> =
        MixtapeHandwritingFont.entries.filter { it in mixtapeHandwritingFontSettings.enabledHandwritingFonts }
            .ifEmpty { MixtapeHandwritingFont.entries }

    private fun MixTapeGroup.stableMixtapeKey(): String = buildString {
        append(startIndex)
        append('|')
        tracks.joinTo(this, separator = ",") { it.id.toString() }
    }

    private fun MixTapeGroup.toMixtapeNameInfo(): MixtapeNameInfo = MixtapeNameInfo(
        stableKey = stableMixtapeKey(),
        name = name,
    )
    
    fun editMixtapeName(stableKey: String, name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        assignedMixtapeNames[stableKey] = trimmedName
        nameStore.saveName(stableKey, trimmedName)
        refreshCurrentUiState()
    }
    
    fun regenerateMixtapeName(stableKey: String) {
        val group = buildBaseMixTapeGroups().firstOrNull { it.stableMixtapeKey() == stableKey } ?: return
        val names = nameSource.names()
        if (names.isEmpty()) return
        val currentName = assignedMixtapeNames[stableKey] ?: nameStore.nameFor(stableKey) ?: group.name
        val namesUsedByOtherTapes = buildBaseMixTapeGroups()
            .asSequence()
            .map { it.stableMixtapeKey() }
            .filterNot { it == stableKey }
            .mapNotNull { key -> assignedMixtapeNames[key] ?: nameStore.nameFor(key) }
            .toSet()
        val availableNames = names.filterNot { it == currentName || it in namesUsedByOtherTapes }
            .ifEmpty { names.filterNot { it == currentName } }
            .ifEmpty { names }
        val randomName = availableNames.random(mixtapeRandom)
        assignedMixtapeNames[stableKey] = randomName
        nameStore.saveName(stableKey, randomName)
        refreshCurrentUiState()
    }
    
    private fun assignNamesForCurrentGroups(force: Boolean = false) {
        val groups = buildBaseMixTapeGroups()
        visualPropertiesStore.retainOnly(groups.map { it.stableMixtapeKey() }.toSet())
        val names = nameSource.names()
        if (groups.isEmpty() || names.isEmpty()) {
            refreshCurrentUiState()
            return
        }
    
        val usedNames = mutableSetOf<String>()
        groups.forEach { group ->
            val stableKey = group.stableMixtapeKey()
            val existingName = if (force) null else assignedMixtapeNames[stableKey] ?: nameStore.nameFor(stableKey)
            if (existingName != null) {
                assignedMixtapeNames[stableKey] = existingName
                usedNames += existingName
                return@forEach
            }
    
            val availableNames = names.filterNot { it in usedNames }.ifEmpty { names }
            val randomName = availableNames.random(mixtapeRandom)
            assignedMixtapeNames[stableKey] = randomName
            nameStore.saveName(stableKey, randomName)
            usedNames += randomName
        }
        refreshCurrentUiState()
    }
    
    private fun refreshCurrentUiState() {
        val currentState = _uiState.value
        matchingMixtapeStableKeyFor(controller.state.tracks)?.let { currentMixtapeStableKey = it }
        _uiState.value = controller.toUiState(
            status = currentState.status,
            screen = currentState.screen,
            message = currentState.message,
        )
    }

    private fun matchingMixtapeStableKeyFor(queueTracks: List<Track>): String? {
        if (queueTracks.isEmpty()) return null
        return buildAssignedMixTapeGroups()
            .firstOrNull { group -> group.tracks.sameOrderedTracksAs(queueTracks) }
            ?.stableMixtapeKey()
    }

    private fun List<Track>.sameOrderedTracksAs(other: List<Track>): Boolean =
        size == other.size && zip(other).all { (left, right) -> left.id == right.id || left.uri == right.uri }

    private fun refreshMixtapeSettingsState(message: String) {
        _uiState.value = controller.toUiState(
            status = _uiState.value.status,
            screen = _uiState.value.screen,
            message = message,
        )
    }

    class Factory(
        private val repository: AudioRepository,
        private val controller: MixtapeController,
        private val mixtapeRandom: Random = Random.Default,
        private val nameSource: MixtapeNameSource = MixtapeNameSource.Empty,
        private val nameStore: MixtapeNameStore = InMemoryMixtapeNameStore(),
        private val visualPropertiesStore: MixtapeVisualPropertiesStore = InMemoryMixtapeVisualPropertiesStore(),
        private val settingsStore: MixtapeSettingsStore = InMemoryMixtapeSettingsStore(),
        private val symbolSettingsStore: MixtapeSymbolSettingsStore = InMemoryMixtapeSymbolSettingsStore(),
        private val tapeSkinSettingsStore: MixtapeTapeSkinSettingsStore = InMemoryMixtapeTapeSkinSettingsStore(),
        private val spineSkinSettingsStore: MixtapeSpineSkinSettingsStore = InMemoryMixtapeSpineSkinSettingsStore(),
        private val handwritingFontSettingsStore: MixtapeHandwritingFontSettingsStore = InMemoryMixtapeHandwritingFontSettingsStore(),
        private val themeSettingsStore: MixtapeThemeSettingsStore = InMemoryMixtapeThemeSettingsStore(),
        private val exclusionSettingsStore: MixtapeExclusionSettingsStore = InMemoryMixtapeExclusionSettingsStore(),
        private val transportCuePlayer: TransportCuePlayer = SilentTransportCuePlayer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(MixtapeViewModel::class.java)) {
                return MixtapeViewModel(
                    repository,
                    controller,
                    mixtapeRandom,
                    nameSource,
                    nameStore,
                    visualPropertiesStore,
                    settingsStore,
                    symbolSettingsStore,
                    tapeSkinSettingsStore,
                    spineSkinSettingsStore,
                    handwritingFontSettingsStore,
                    themeSettingsStore,
                    exclusionSettingsStore,
                    transportCuePlayer,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
