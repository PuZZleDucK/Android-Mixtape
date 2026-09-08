package com.example.androidmixtape.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.androidmixtape.viewmodel.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NowPlayingSettingsPersistenceTest {
    @Test fun visualEditsReloadFromFreshStores() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "card_4834_${System.nanoTime()}_"
        val files = mutableSetOf<String>()
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                files += prefix + name
                return base.getSharedPreferences(prefix + name, mode)
            }
        }
        try {
            val settingsStore = SharedPreferencesMixtapeSettingsStore(context)
            val themeStore = SharedPreferencesMixtapeThemeSettingsStore(context)
            val initialSettings = settingsStore.settings()
            val initialTheme = themeStore.settings()
            val editedSettings = initialSettings.copy(handwritingMessiness =
                HandwritingMessiness.entries.first { it != initialSettings.handwritingMessiness })
            val editedTheme = initialTheme.copy(deckTheme =
                DeckTheme.entries.first { it != initialTheme.deckTheme })
            settingsStore.saveSettings(editedSettings)
            themeStore.saveSettings(editedTheme)
            // A synchronous commit drains pending apply writes before reading new stores.
            files.forEach { assertTrue(base.getSharedPreferences(it, Context.MODE_PRIVATE).edit().commit()) }
            assertEquals(editedSettings, SharedPreferencesMixtapeSettingsStore(context).settings())
            assertEquals(editedTheme, SharedPreferencesMixtapeThemeSettingsStore(context).settings())
        } finally {
            files.forEach { base.deleteSharedPreferences(it) }
        }
    }
}
