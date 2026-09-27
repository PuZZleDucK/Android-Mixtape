package com.example.androidmixtape.car

import android.content.Context
import com.example.androidmixtape.data.AudioRepository
import com.example.androidmixtape.data.MediaStoreAudioRepository
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.name.AssetMixtapeNameSource
import com.example.androidmixtape.name.MixtapeNameSource
import com.example.androidmixtape.name.MixtapeNameStore
import com.example.androidmixtape.name.SharedPreferencesMixtapeNameStore
import com.example.androidmixtape.viewmodel.MixtapeSettings
import com.example.androidmixtape.viewmodel.MixtapeSettingsStore
import com.example.androidmixtape.viewmodel.MixtapeEmbellishment
import com.example.androidmixtape.viewmodel.MixtapeSpineSkin
import com.example.androidmixtape.viewmodel.MixtapeSymbolColor
import com.example.androidmixtape.viewmodel.MixtapeTapeSkin
import com.example.androidmixtape.viewmodel.MixtapeVisualPropertiesStore
import com.example.androidmixtape.viewmodel.MixtapeVisualProperties
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeVisualPropertiesStore
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeSettingsStore
import com.example.androidmixtape.viewmodel.InMemoryMixtapeMembershipStore
import com.example.androidmixtape.viewmodel.MixtapeMembershipStore
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeMembershipStore
import com.example.androidmixtape.viewmodel.FilenameExclusionMatcher
import com.example.androidmixtape.viewmodel.InMemoryMixtapeExclusionSettingsStore
import com.example.androidmixtape.viewmodel.MixtapeExclusionSettingsStore
import com.example.androidmixtape.viewmodel.SharedPreferencesMixtapeExclusionSettingsStore
import com.example.androidmixtape.viewmodel.resolveMixtapeGroups
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

/** Driver-safe catalog for the Cars App Library UI. */
data class CarMixtape(
    val id: String,
    val stableKey: String,
    val displayName: String,
    val trackCount: Int,
    val startIndex: Int,
    val visualProperties: MixtapeVisualProperties,
    val tracks: List<CarTrack>,
) {
    val needsPermission: Boolean = false
}

data class CarTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val uri: String,
    val source: Track,
)

data class CarMixtapeCatalogState(
    val mixtapes: List<CarMixtape>,
    val needsPermission: Boolean = false,
    val message: String? = null,
)

class CarMixtapeCatalog(
    private val repository: AudioRepository,
    private val settingsStore: MixtapeSettingsStore,
    private val nameSource: MixtapeNameSource,
    private val nameStore: MixtapeNameStore,
    private val visualPropertiesStore: MixtapeVisualPropertiesStore,
    private val random: Random = Random.Default,
    private val membershipStore: MixtapeMembershipStore = InMemoryMixtapeMembershipStore(),
    private val exclusionSettingsStore: MixtapeExclusionSettingsStore = InMemoryMixtapeExclusionSettingsStore(),
) {
    constructor(context: Context) : this(
        repository = MediaStoreAudioRepository(context.applicationContext),
        settingsStore = SharedPreferencesMixtapeSettingsStore(context.applicationContext),
        nameSource = AssetMixtapeNameSource(context.applicationContext),
        nameStore = SharedPreferencesMixtapeNameStore(context.applicationContext),
        visualPropertiesStore = SharedPreferencesMixtapeVisualPropertiesStore(context.applicationContext),
        membershipStore = SharedPreferencesMixtapeMembershipStore(context.applicationContext),
        exclusionSettingsStore = SharedPreferencesMixtapeExclusionSettingsStore(context.applicationContext),
    )

    fun load(): CarMixtapeCatalogState {
        return try {
            val tracks = runBlocking(Dispatchers.IO) { repository.loadTracks() }
            if (tracks.isEmpty()) {
                return CarMixtapeCatalogState(
                    mixtapes = emptyList(),
                    message = "Open Mixtape on your phone and grant audio permission to build car mixtapes.",
                )
            }
            val settings = settingsStore.settings()
        val usedNames = mutableSetOf<String>()
        val availableNames = nameSource.names()
        val patterns = exclusionSettingsStore.settings().filenamePatterns
        val eligibleTracks = tracks.filterNot { FilenameExclusionMatcher.matches(it, patterns) }
        val mixtapes = resolveMixtapeGroups(eligibleTracks, settings, membershipStore).mapIndexed { index, group ->
            val stableKey = group.stableKey
            val savedName = nameStore.nameFor(stableKey)
            val displayName = savedName ?: availableNames
                .filterNot { it in usedNames }
                .ifEmpty { availableNames }
                .takeIf { it.isNotEmpty() }
                ?.random(random)
                ?.also { nameStore.saveName(stableKey, it) }
                ?: group.name
            usedNames += displayName
            CarMixtape(
                id = "mixtape:$stableKey",
                stableKey = stableKey,
                displayName = displayName,
                trackCount = group.tracks.size,
                startIndex = group.startIndex,
                visualProperties = visualPropertiesStore.propertiesFor(stableKey)
                    ?: fallbackVisualProperties(stableKey, index),
                tracks = group.tracks.map { track ->
                    CarTrack(
                        id = "track:${track.id}",
                        title = track.title,
                        subtitle = track.displaySubtitle,
                        uri = track.uri,
                        source = track,
                    )
                },
            )
        }
            CarMixtapeCatalogState(mixtapes = mixtapes)
        } catch (_: SecurityException) {
            CarMixtapeCatalogState(
                mixtapes = emptyList(),
                needsPermission = true,
                message = "Permission required: open Mixtape on your phone and allow audio access.",
            )
        } catch (error: IllegalArgumentException) {
            CarMixtapeCatalogState(
                mixtapes = emptyList(),
                message = error.message ?: "Mixtape car catalog is unavailable.",
            )
        }
    }

    private fun fallbackVisualProperties(stableKey: String, index: Int): MixtapeVisualProperties {
        val hash = stableKey.hashCode()
        return MixtapeVisualProperties(
            jitterStartIndex = Math.floorMod(hash, 5_000),
            embellishment = MixtapeEmbellishment.entries[Math.floorMod(hash / 7, MixtapeEmbellishment.entries.size)],
            symbolColor = MixtapeSymbolColor.entries[Math.floorMod(hash / 11, MixtapeSymbolColor.entries.size)],
            nameColor = MixtapeSymbolColor.entries[Math.floorMod(hash / 13, MixtapeSymbolColor.entries.size)],
            tapeSkin = MixtapeTapeSkin.entries[Math.floorMod(hash / 17, MixtapeTapeSkin.entries.size)],
            spineSkin = MixtapeSpineSkin.entries[index % MixtapeSpineSkin.entries.size],
        )
    }
}
