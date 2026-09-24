package com.example.androidmixtape.viewmodel

import android.content.Context
import com.example.androidmixtape.data.Track
import org.json.JSONArray
import org.json.JSONObject

/** Membership is separate from library order and from the tape's saved name/design. */
data class StoredMixtape(val stableKey: String, val trackIds: List<Long>)

data class MixtapeMembership(
    val tapes: List<StoredMixtape>,
    // Includes removed tracks so a rescan cannot silently add them to another tape.
    val knownTrackIds: Set<Long>,
)

interface MixtapeMembershipStore {
    fun load(): MixtapeMembership?
    fun save(membership: MixtapeMembership)
    fun clear()
}

class InMemoryMixtapeMembershipStore : MixtapeMembershipStore {
    private var membership: MixtapeMembership? = null
    override fun load() = membership
    override fun save(membership: MixtapeMembership) { this.membership = membership }
    override fun clear() { membership = null }
}

class SharedPreferencesMixtapeMembershipStore(context: Context) : MixtapeMembershipStore {
    private val preferences = context.applicationContext.getSharedPreferences("mixtape_membership", Context.MODE_PRIVATE)
    private var cachedJson: String? = null
    private var cachedMembership: MixtapeMembership? = null

    override fun load(): MixtapeMembership? {
        val json = preferences.getString("membership", null) ?: return null
        if (json == cachedJson) return cachedMembership
        val root = JSONObject(json)
        val tapes = root.getJSONArray("tapes")
        return MixtapeMembership(
            tapes = List(tapes.length()) { index ->
                val tape = tapes.getJSONObject(index)
                StoredMixtape(tape.getString("key"), tape.getJSONArray("tracks").longs())
            },
            knownTrackIds = root.getJSONArray("knownTracks").longs().toSet(),
        ).also {
            cachedJson = json
            cachedMembership = it
        }
    }

    override fun save(membership: MixtapeMembership) {
        val tapes = JSONArray()
        membership.tapes.forEach { tape ->
            tapes.put(JSONObject().put("key", tape.stableKey).put("tracks", JSONArray(tape.trackIds)))
        }
        val json = JSONObject().put("tapes", tapes).put("knownTracks", JSONArray(membership.knownTrackIds)).toString()
        preferences.edit().putString("membership", json).apply()
        cachedJson = json
        cachedMembership = membership
    }

    override fun clear() {
        preferences.edit().remove("membership").apply()
        cachedJson = null
        cachedMembership = null
    }

    private fun JSONArray.longs(): List<Long> = List(length()) { getLong(it) }
}

/** Initial keys match the old name/design keys, but never change after a removal. */
fun resolveMixtapeGroups(
    tracks: List<Track>,
    settings: MixtapeSettings,
    store: MixtapeMembershipStore,
): List<MixTapeGroup> {
    val saved = store.load()
    val newTracks = tracks.filterNot { it.id in saved?.knownTrackIds.orEmpty() }
    val membership = if (saved == null || newTracks.isNotEmpty()) {
        val tapes = saved?.tapes.orEmpty().toMutableList()
        val availableIds = tracks.map(Track::id).toSet()
        val freeSlots = tapes.lastOrNull()?.let { tape ->
            (settings.songsPerMixTape - tape.trackIds.count { it in availableIds }).coerceAtLeast(0)
        } ?: 0
        val filling = newTracks.take(freeSlots)
        if (filling.isNotEmpty()) {
            val last = tapes.lastIndex
            tapes[last] = tapes[last].copy(trackIds = tapes[last].trackIds + filling.map(Track::id))
        }
        tapes += buildMixTapeGroups(newTracks.drop(filling.size), settings).map {
            StoredMixtape(it.stableKey, it.tracks.map(Track::id))
        }
        MixtapeMembership(
            tapes = tapes,
            knownTrackIds = saved?.knownTrackIds.orEmpty() + newTracks.map(Track::id),
        ).also(store::save)
    } else saved
    val tracksById = tracks.associateBy(Track::id)
    var startIndex = 0
    return membership.tapes.mapIndexed { index, tape ->
        val availableTracks = tape.trackIds.mapNotNull(tracksById::get)
        MixTapeGroup(
            name = "Mix Tape ${index + 1}",
            tracks = availableTracks,
            startIndex = startIndex,
            stableKey = tape.stableKey,
        ).also { startIndex += availableTracks.size }
    }
}

fun MixtapeMembershipStore.removeTracks(trackIds: Set<Long>, onlyMixtapeKey: String? = null) {
    val membership = load() ?: return
    save(membership.copy(tapes = membership.tapes.map { tape ->
        if (onlyMixtapeKey == null || tape.stableKey == onlyMixtapeKey) {
            tape.copy(trackIds = tape.trackIds.filterNot { it in trackIds })
        } else tape
    }))
}
