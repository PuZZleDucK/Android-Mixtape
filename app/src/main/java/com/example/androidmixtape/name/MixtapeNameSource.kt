package com.example.androidmixtape.name

import android.content.Context

interface MixtapeNameSource {
    fun names(): List<String>

    object Empty : MixtapeNameSource {
        override fun names(): List<String> = emptyList()
    }
}

class AssetMixtapeNameSource(context: Context) : MixtapeNameSource {
    private val applicationContext = context.applicationContext
    private val loadedNames: List<String> by lazy {
        applicationContext.assets.open(ASSET_FILE_NAME).bufferedReader().useLines { lines ->
            lines.map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .toList()
        }
    }

    override fun names(): List<String> = loadedNames

    private companion object {
        private const val ASSET_FILE_NAME = "mixtape_names.txt"
    }
}
