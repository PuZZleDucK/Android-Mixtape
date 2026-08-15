package com.example.androidmixtape.name

import android.content.Context

interface MixtapeNameStore {
    fun nameFor(stableMixtapeKey: String): String?
    fun saveName(stableMixtapeKey: String, name: String)
}

class InMemoryMixtapeNameStore(
    initialNames: Map<String, String> = emptyMap(),
) : MixtapeNameStore {
    private val names = initialNames.toMutableMap()

    override fun nameFor(stableMixtapeKey: String): String? = names[stableMixtapeKey]

    override fun saveName(stableMixtapeKey: String, name: String) {
        names[stableMixtapeKey] = name
    }
}

class SharedPreferencesMixtapeNameStore(context: Context) : MixtapeNameStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mixtape_names",
        Context.MODE_PRIVATE,
    )

    override fun nameFor(stableMixtapeKey: String): String? =
        preferences.getString(prefKey(stableMixtapeKey), null)

    override fun saveName(stableMixtapeKey: String, name: String) {
        preferences.edit().putString(prefKey(stableMixtapeKey), name).apply()
    }

    private fun prefKey(stableMixtapeKey: String): String = "name:$stableMixtapeKey"
}
