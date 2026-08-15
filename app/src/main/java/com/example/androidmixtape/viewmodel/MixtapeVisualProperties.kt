package com.example.androidmixtape.viewmodel

import android.content.Context
import android.content.res.Configuration

enum class MixtapeSpineSkin {
    CreamRed,
    BlushBlue,
    SkyGreen,
    MintAmber,
    LemonNavy,
    PinkZineBorder,
    VioletLibraryStripe,
    MintCollageTab,
    AmberIndexBlock,
    PowderBlueMarker,
    CoralStickerRail,
    EmeraldNotebookLines,
    SepiaNewsprintFrame,
    BlackPhotoNegative,
    RainbowCutout,
    SunburstCreamRail,
    AquaLibraryTab,
    CandyStripePink,
    GoldenMemoBlock,
    TomatoBorderLabel,
    TealNotebookRail,
    VioletIndexPanel,
    LimeCutoutStripe,
    PeachGridSticker,
    MidnightRainbowFrame,
}

enum class MixtapeTapeSkin {
    ClassicCreamDots,
    BlackMagentaStripe,
    ChromeGreen,
    CharcoalGold,
    TranslucentViolet,
    SmokedGreenLowNoise,
    TealMeterDeck,
    IvoryRedStripe,
    IvoryBlue120,
    RubyFerroGrid,
    MidnightCreamSuper,
    LimeNavyC30,
}

enum class DeckTheme {
    SilverfaceHiFi, BlackoutPortable, SunsetBoombox, NavyMicro, CrimsonMetal, SafetyYellow, GraphiteSlim,
}

enum class CassetteTheme {
    StudioFerric, MidnightChrome, GhostClear, BubblegumPop, TranslucentSmoke,
    TranslucentRuby, TranslucentCobalt, TranslucentLime, TranslucentViolet,
}

enum class ScrewTheme { Light, Dark }

enum class StickerTheme {
    None, StudioStock, MinimalMono, VintageSunset, AquaOrbit, LowerDeck, HissTachiLoNoise, PrismC60,
}

enum class CaseTheme {
    CrystalClear, SmokeTint, AmberTint, RubyClear, HotPinkClear,
    ElectricBlueClear, AcidGreenClear, VioletClear, CloudyClear,
}

enum class SleeveTheme {
    BlankWhite, RuledNotebook, AlbumPrint, KraftBrown, MidnightGrid,
    CoralAlbum, ForestFleck, BlueprintGrid, GraphPaper,
}

enum class MixtapeEmbellishment {
    Star,
    Heart,
    LightningBolt,
    Sparkles,
    Smiley,
    Flower,
    MusicNote,
    Moon,
    Swirl,
    Crown,
    QuarterNote,
    EighthNote,
    BeamedEighthNotes,
    SixteenthNote,
    WholeNote,
    HalfNote,
    TrebleClef,
    BassClef,
    SharpSign,
    FlatSign,
}

enum class MixtapeSymbolColor {
    Navy,
    Red,
    Green,
    Purple,
    Amber;

    fun next(): MixtapeSymbolColor {
        val colors = entries
        return colors[(ordinal + 1) % colors.size]
    }
}

data class MixtapeVisualProperties(
    val handwritingFont: MixtapeHandwritingFont = MixtapeHandwritingFont.Kalam,
    val jitterStartIndex: Int = 0,
    val embellishment: MixtapeEmbellishment = MixtapeEmbellishment.Star,
    val symbolColor: MixtapeSymbolColor = MixtapeSymbolColor.Navy,
    val nameColor: MixtapeSymbolColor = MixtapeSymbolColor.Navy,
    val tapeSkin: MixtapeTapeSkin = MixtapeTapeSkin.ClassicCreamDots,
    val spineSkin: MixtapeSpineSkin = MixtapeSpineSkin.CreamRed,
    val decorativeId: String = "A",
    val cassetteTheme: CassetteTheme = CassetteTheme.GhostClear,
    val screwTheme: ScrewTheme = ScrewTheme.Light,
    val stickerTheme: StickerTheme = StickerTheme.None,
    val caseTheme: CaseTheme = CaseTheme.CrystalClear,
    val sleeveTheme: SleeveTheme = SleeveTheme.AlbumPrint,
)

data class MixtapeSymbolSettings(
    val enabledEmbellishments: Set<MixtapeEmbellishment> = allEmbellishments(),
) {
    companion object {
        fun allEmbellishments(): Set<MixtapeEmbellishment> = MixtapeEmbellishment.entries.toSet()

        fun normalized(enabledEmbellishments: Set<MixtapeEmbellishment>): MixtapeSymbolSettings =
            MixtapeSymbolSettings(enabledEmbellishments.takeUnless { it.isEmpty() } ?: allEmbellishments())

        fun fromNames(names: Set<String>?): MixtapeSymbolSettings {
            if (names == null) return MixtapeSymbolSettings()
            val parsed = MixtapeEmbellishment.entries.filter { it.name in names }.toSet()
            return normalized(parsed)
        }
    }
}

interface MixtapeSymbolSettingsStore {
    fun settings(): MixtapeSymbolSettings
    fun saveSettings(settings: MixtapeSymbolSettings)
}

class InMemoryMixtapeSymbolSettingsStore(
    initialSettings: MixtapeSymbolSettings = MixtapeSymbolSettings(),
) : MixtapeSymbolSettingsStore {
    constructor(initialEnabledEmbellishments: Set<MixtapeEmbellishment>) : this(
        MixtapeSymbolSettings.normalized(initialEnabledEmbellishments),
    )

    private var currentSettings = MixtapeSymbolSettings.normalized(initialSettings.enabledEmbellishments)

    override fun settings(): MixtapeSymbolSettings = currentSettings

    override fun saveSettings(settings: MixtapeSymbolSettings) {
        currentSettings = MixtapeSymbolSettings.normalized(settings.enabledEmbellishments)
    }
}

class SharedPreferencesMixtapeSymbolSettingsStore(
    context: Context,
) : MixtapeSymbolSettingsStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mixtape_symbol_settings",
        Context.MODE_PRIVATE,
    )

    override fun settings(): MixtapeSymbolSettings =
        MixtapeSymbolSettings.fromNames(preferences.getStringSet(KEY_ENABLED_EMBELLISHMENTS, null))

    override fun saveSettings(settings: MixtapeSymbolSettings) {
        val normalized = MixtapeSymbolSettings.normalized(settings.enabledEmbellishments)
        preferences.edit()
            .putStringSet(KEY_ENABLED_EMBELLISHMENTS, normalized.enabledEmbellishments.map { it.name }.toSet())
            .apply()
    }

    private companion object {
        const val KEY_ENABLED_EMBELLISHMENTS = "enabled_embellishments"
    }
}

data class MixtapeTapeSkinSettings(
    val enabledTapeSkins: Set<MixtapeTapeSkin> = allTapeSkins(),
) {
    companion object {
        fun allTapeSkins(): Set<MixtapeTapeSkin> = MixtapeTapeSkin.entries.toSet()

        fun normalized(enabledTapeSkins: Set<MixtapeTapeSkin>): MixtapeTapeSkinSettings =
            MixtapeTapeSkinSettings(enabledTapeSkins.takeUnless { it.isEmpty() } ?: allTapeSkins())

        fun fromNames(names: Set<String>?): MixtapeTapeSkinSettings {
            if (names == null) return MixtapeTapeSkinSettings()
            val parsed = MixtapeTapeSkin.entries.filter { it.name in names }.toSet()
            return normalized(parsed)
        }
    }
}

interface MixtapeTapeSkinSettingsStore {
    fun settings(): MixtapeTapeSkinSettings
    fun saveSettings(settings: MixtapeTapeSkinSettings)
}

class InMemoryMixtapeTapeSkinSettingsStore(
    initialSettings: MixtapeTapeSkinSettings = MixtapeTapeSkinSettings(),
) : MixtapeTapeSkinSettingsStore {
    constructor(initialEnabledTapeSkins: Set<MixtapeTapeSkin>) : this(
        MixtapeTapeSkinSettings.normalized(initialEnabledTapeSkins),
    )

    private var currentSettings = MixtapeTapeSkinSettings.normalized(initialSettings.enabledTapeSkins)

    override fun settings(): MixtapeTapeSkinSettings = currentSettings

    override fun saveSettings(settings: MixtapeTapeSkinSettings) {
        currentSettings = MixtapeTapeSkinSettings.normalized(settings.enabledTapeSkins)
    }
}

class SharedPreferencesMixtapeTapeSkinSettingsStore(
    context: Context,
) : MixtapeTapeSkinSettingsStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mixtape_tape_skin_settings",
        Context.MODE_PRIVATE,
    )

    override fun settings(): MixtapeTapeSkinSettings =
        MixtapeTapeSkinSettings.fromNames(preferences.getStringSet(KEY_ENABLED_TAPE_SKINS, null))

    override fun saveSettings(settings: MixtapeTapeSkinSettings) {
        val normalized = MixtapeTapeSkinSettings.normalized(settings.enabledTapeSkins)
        preferences.edit()
            .putStringSet(KEY_ENABLED_TAPE_SKINS, normalized.enabledTapeSkins.map { it.name }.toSet())
            .apply()
    }

    private companion object {
        const val KEY_ENABLED_TAPE_SKINS = "enabled_tape_skins"
    }
}

data class MixtapeSpineSkinSettings(
    val enabledSpineSkins: Set<MixtapeSpineSkin> = allSpineSkins(),
) {
    companion object {
        fun allSpineSkins(): Set<MixtapeSpineSkin> = MixtapeSpineSkin.entries.toSet()

        fun normalized(enabledSpineSkins: Set<MixtapeSpineSkin>): MixtapeSpineSkinSettings =
            MixtapeSpineSkinSettings(enabledSpineSkins.takeUnless { it.isEmpty() } ?: allSpineSkins())

        fun fromNames(names: Set<String>?): MixtapeSpineSkinSettings {
            if (names == null) return MixtapeSpineSkinSettings()
            val parsed = MixtapeSpineSkin.entries.filter { it.name in names }.toSet()
            return normalized(parsed)
        }
    }
}

interface MixtapeSpineSkinSettingsStore {
    fun settings(): MixtapeSpineSkinSettings
    fun saveSettings(settings: MixtapeSpineSkinSettings)
}

class InMemoryMixtapeSpineSkinSettingsStore(
    initialSettings: MixtapeSpineSkinSettings = MixtapeSpineSkinSettings(),
) : MixtapeSpineSkinSettingsStore {
    private var currentSettings = MixtapeSpineSkinSettings.normalized(initialSettings.enabledSpineSkins)

    override fun settings(): MixtapeSpineSkinSettings = currentSettings

    override fun saveSettings(settings: MixtapeSpineSkinSettings) {
        currentSettings = MixtapeSpineSkinSettings.normalized(settings.enabledSpineSkins)
    }
}

class SharedPreferencesMixtapeSpineSkinSettingsStore(
    context: Context,
) : MixtapeSpineSkinSettingsStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mixtape_spine_skin_settings",
        Context.MODE_PRIVATE,
    )

    override fun settings(): MixtapeSpineSkinSettings =
        MixtapeSpineSkinSettings.fromNames(preferences.getStringSet(KEY_ENABLED_SPINE_SKINS, null))

    override fun saveSettings(settings: MixtapeSpineSkinSettings) {
        val normalized = MixtapeSpineSkinSettings.normalized(settings.enabledSpineSkins)
        preferences.edit()
            .putStringSet(KEY_ENABLED_SPINE_SKINS, normalized.enabledSpineSkins.map { it.name }.toSet())
            .apply()
    }

    private companion object {
        const val KEY_ENABLED_SPINE_SKINS = "enabled_spine_skins"
    }
}

data class MixtapeHandwritingFontSettings(
    val enabledHandwritingFonts: Set<MixtapeHandwritingFont> = allHandwritingFonts(),
) {
    companion object {
        fun allHandwritingFonts(): Set<MixtapeHandwritingFont> = MixtapeHandwritingFont.entries.toSet()

        fun normalized(enabledHandwritingFonts: Set<MixtapeHandwritingFont>): MixtapeHandwritingFontSettings =
            MixtapeHandwritingFontSettings(enabledHandwritingFonts.takeUnless { it.isEmpty() } ?: allHandwritingFonts())

        fun fromNames(names: Set<String>?): MixtapeHandwritingFontSettings {
            if (names == null) return MixtapeHandwritingFontSettings()
            val parsed = MixtapeHandwritingFont.entries.filter { it.name in names }.toSet()
            return normalized(parsed)
        }
    }
}

interface MixtapeHandwritingFontSettingsStore {
    fun settings(): MixtapeHandwritingFontSettings
    fun saveSettings(settings: MixtapeHandwritingFontSettings)
}

class InMemoryMixtapeHandwritingFontSettingsStore(
    initialSettings: MixtapeHandwritingFontSettings = MixtapeHandwritingFontSettings(),
) : MixtapeHandwritingFontSettingsStore {
    constructor(initialEnabledHandwritingFonts: Set<MixtapeHandwritingFont>) : this(
        MixtapeHandwritingFontSettings.normalized(initialEnabledHandwritingFonts),
    )

    private var currentSettings = MixtapeHandwritingFontSettings.normalized(initialSettings.enabledHandwritingFonts)

    override fun settings(): MixtapeHandwritingFontSettings = currentSettings

    override fun saveSettings(settings: MixtapeHandwritingFontSettings) {
        currentSettings = MixtapeHandwritingFontSettings.normalized(settings.enabledHandwritingFonts)
    }
}

class SharedPreferencesMixtapeHandwritingFontSettingsStore(
    context: Context,
) : MixtapeHandwritingFontSettingsStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mixtape_handwriting_font_settings",
        Context.MODE_PRIVATE,
    )

    override fun settings(): MixtapeHandwritingFontSettings =
        MixtapeHandwritingFontSettings.fromNames(preferences.getStringSet(KEY_ENABLED_HANDWRITING_FONTS, null))

    override fun saveSettings(settings: MixtapeHandwritingFontSettings) {
        val normalized = MixtapeHandwritingFontSettings.normalized(settings.enabledHandwritingFonts)
        preferences.edit()
            .putStringSet(KEY_ENABLED_HANDWRITING_FONTS, normalized.enabledHandwritingFonts.map { it.name }.toSet())
            .apply()
    }

    private companion object {
        const val KEY_ENABLED_HANDWRITING_FONTS = "enabled_handwriting_fonts"
    }
}

data class MixtapeThemeSettings(
    val deckTheme: DeckTheme = DeckTheme.SilverfaceHiFi,
    val enabledCassetteThemes: Set<CassetteTheme> = CassetteTheme.entries.toSet(),
    val enabledScrewThemes: Set<ScrewTheme> = ScrewTheme.entries.toSet(),
    val enabledStickerThemes: Set<StickerTheme> = StickerTheme.entries.toSet(),
    val enabledCaseThemes: Set<CaseTheme> = CaseTheme.entries.toSet(),
    val enabledSleeveThemes: Set<SleeveTheme> = SleeveTheme.entries.toSet(),
) {
    companion object {
        fun normalized(settings: MixtapeThemeSettings): MixtapeThemeSettings = settings.copy(
            enabledCassetteThemes = settings.enabledCassetteThemes.ifEmpty { CassetteTheme.entries.toSet() },
            enabledScrewThemes = settings.enabledScrewThemes.ifEmpty { ScrewTheme.entries.toSet() },
            enabledStickerThemes = settings.enabledStickerThemes.ifEmpty { StickerTheme.entries.toSet() },
            enabledCaseThemes = settings.enabledCaseThemes.ifEmpty { CaseTheme.entries.toSet() },
            enabledSleeveThemes = settings.enabledSleeveThemes.ifEmpty { SleeveTheme.entries.toSet() },
        )
    }
}

interface MixtapeThemeSettingsStore {
    fun settings(): MixtapeThemeSettings
    fun saveSettings(settings: MixtapeThemeSettings)
}

class InMemoryMixtapeThemeSettingsStore(
    initialSettings: MixtapeThemeSettings = MixtapeThemeSettings(),
) : MixtapeThemeSettingsStore {
    private var currentSettings = MixtapeThemeSettings.normalized(initialSettings)
    override fun settings(): MixtapeThemeSettings = currentSettings
    override fun saveSettings(settings: MixtapeThemeSettings) {
        currentSettings = MixtapeThemeSettings.normalized(settings)
    }
}

internal fun initialDeckThemeForUiMode(uiMode: Int): DeckTheme =
    if (uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
        DeckTheme.BlackoutPortable
    } else {
        DeckTheme.SilverfaceHiFi
    }

class SharedPreferencesMixtapeThemeSettingsStore(context: Context) : MixtapeThemeSettingsStore {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.getSharedPreferences("mixtape_component_theme_settings", Context.MODE_PRIVATE)

    override fun settings(): MixtapeThemeSettings = MixtapeThemeSettings.normalized(
        MixtapeThemeSettings(
            deckTheme = savedOrInitialDeckTheme(),
            enabledCassetteThemes = names(KEY_CASSETTES, CassetteTheme.entries),
            enabledScrewThemes = names(KEY_SCREWS, ScrewTheme.entries),
            enabledStickerThemes = names(KEY_STICKERS, StickerTheme.entries),
            enabledCaseThemes = names(KEY_CASES, CaseTheme.entries),
            enabledSleeveThemes = names(KEY_SLEEVES, SleeveTheme.entries),
        ),
    )

    override fun saveSettings(settings: MixtapeThemeSettings) {
        val normalized = MixtapeThemeSettings.normalized(settings)
        preferences.edit()
            .putString(KEY_DECK, normalized.deckTheme.name)
            .putStringSet(KEY_CASSETTES, normalized.enabledCassetteThemes.map { it.name }.toSet())
            .putStringSet(KEY_SCREWS, normalized.enabledScrewThemes.map { it.name }.toSet())
            .putStringSet(KEY_STICKERS, normalized.enabledStickerThemes.map { it.name }.toSet())
            .putStringSet(KEY_CASES, normalized.enabledCaseThemes.map { it.name }.toSet())
            .putStringSet(KEY_SLEEVES, normalized.enabledSleeveThemes.map { it.name }.toSet())
            .apply()
    }

    private fun savedOrInitialDeckTheme(): DeckTheme {
        preferences.getString(KEY_DECK, null)
            ?.let { saved -> DeckTheme.entries.firstOrNull { it.name == saved } }
            ?.let { return it }

        val initialTheme = initialDeckThemeForUiMode(applicationContext.resources.configuration.uiMode)
        preferences.edit().putString(KEY_DECK, initialTheme.name).commit()
        return initialTheme
    }

    private fun <T : Enum<T>> names(key: String, entries: List<T>): Set<T> {
        val saved = preferences.getStringSet(key, null) ?: return entries.toSet()
        return entries.filter { it.name in saved }.toSet().ifEmpty { entries.toSet() }
    }

    private companion object {
        const val KEY_DECK = "deck_theme"
        const val KEY_CASSETTES = "enabled_cassette_themes"
        const val KEY_SCREWS = "enabled_screw_themes"
        const val KEY_STICKERS = "enabled_sticker_themes"
        const val KEY_CASES = "enabled_case_themes"
        const val KEY_SLEEVES = "enabled_sleeve_themes"
    }
}

interface MixtapeVisualPropertiesStore {
    fun propertiesFor(stableMixtapeKey: String): MixtapeVisualProperties?
    fun saveProperties(stableMixtapeKey: String, properties: MixtapeVisualProperties)
    fun retainOnly(stableMixtapeKeys: Set<String>) {}
}

class InMemoryMixtapeVisualPropertiesStore(
    initialProperties: Map<String, MixtapeVisualProperties> = emptyMap(),
) : MixtapeVisualPropertiesStore {
    private val propertiesByKey = initialProperties.toMutableMap()

    override fun propertiesFor(stableMixtapeKey: String): MixtapeVisualProperties? = propertiesByKey[stableMixtapeKey]

    override fun saveProperties(stableMixtapeKey: String, properties: MixtapeVisualProperties) {
        propertiesByKey[stableMixtapeKey] = properties
    }

    override fun retainOnly(stableMixtapeKeys: Set<String>) {
        propertiesByKey.keys.retainAll(stableMixtapeKeys)
    }
}

class SharedPreferencesMixtapeVisualPropertiesStore(
    context: Context,
) : MixtapeVisualPropertiesStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mixtape_visual_properties",
        Context.MODE_PRIVATE,
    )

    override fun propertiesFor(stableMixtapeKey: String): MixtapeVisualProperties? {
        // Schema 2 deliberately ignores all legacy combined tape/spine skins.
        if (preferences.getInt(prefKey(stableMixtapeKey, "theme_schema"), 0) != THEME_SCHEMA) return null
        val fontName = preferences.getString(prefKey(stableMixtapeKey, "font"), null) ?: return null
        val jitterIndexKey = prefKey(stableMixtapeKey, "jitter_start_index")
        val jitterStartIndex = if (preferences.contains(jitterIndexKey)) {
            Math.floorMod(preferences.getInt(jitterIndexKey, 0), 5_000)
        } else {
            val legacyJitterKey = prefKey(stableMixtapeKey, "base_jitter_key")
            val migratedIndex = preferences.getString(legacyJitterKey, null)
                ?.hashCode()
                ?.let { Math.floorMod(it, 5_000) }
                ?: return null
            preferences.edit()
                .putInt(jitterIndexKey, migratedIndex)
                .remove(legacyJitterKey)
                .apply()
            migratedIndex
        }
        val embellishmentName = preferences.getString(prefKey(stableMixtapeKey, "embellishment"), null)
        val symbolColorName = preferences.getString(prefKey(stableMixtapeKey, "symbol_color"), null)
        val nameColorName = preferences.getString(prefKey(stableMixtapeKey, "name_color"), null)
        val tapeSkinName = preferences.getString(prefKey(stableMixtapeKey, "tape_skin"), null)
        val spineSkinName = preferences.getString(prefKey(stableMixtapeKey, "spine_skin"), null)
        val decorativeId = preferences.getString(prefKey(stableMixtapeKey, "decorative_id"), null) ?: return null
        val cassetteTheme = enumValue<CassetteTheme>(stableMixtapeKey, "cassette_theme") ?: return null
        val screwTheme = enumValue<ScrewTheme>(stableMixtapeKey, "screw_theme") ?: return null
        val stickerTheme = enumValue<StickerTheme>(stableMixtapeKey, "sticker_theme") ?: return null
        val caseTheme = enumValue<CaseTheme>(stableMixtapeKey, "case_theme") ?: return null
        val sleeveTheme = enumValue<SleeveTheme>(stableMixtapeKey, "sleeve_theme") ?: return null
        val handwritingFont = MixtapeHandwritingFont.entries.firstOrNull { it.name == fontName } ?: return null
        val embellishment = embellishmentName
            ?.let { savedName -> MixtapeEmbellishment.entries.firstOrNull { it.name == savedName } }
            ?: MixtapeEmbellishment.Star
        val symbolColor = symbolColorName
            ?.let { savedName -> MixtapeSymbolColor.entries.firstOrNull { it.name == savedName } }
            ?: MixtapeSymbolColor.Navy
        val nameColor = nameColorName
            ?.let { savedName -> MixtapeSymbolColor.entries.firstOrNull { it.name == savedName } }
            ?: symbolColor
        val tapeSkin = tapeSkinName
            ?.let { savedName -> MixtapeTapeSkin.entries.firstOrNull { it.name == savedName } }
            ?: MixtapeTapeSkin.ClassicCreamDots
        val spineSkin = spineSkinName
            ?.let { savedName -> MixtapeSpineSkin.entries.firstOrNull { it.name == savedName } }
            ?: MixtapeSpineSkin.CreamRed
        return MixtapeVisualProperties(
            handwritingFont = handwritingFont,
            jitterStartIndex = jitterStartIndex,
            embellishment = embellishment,
            symbolColor = symbolColor,
            nameColor = nameColor,
            tapeSkin = tapeSkin,
            spineSkin = spineSkin,
            decorativeId = decorativeId,
            cassetteTheme = cassetteTheme,
            screwTheme = screwTheme,
            stickerTheme = stickerTheme,
            caseTheme = caseTheme,
            sleeveTheme = sleeveTheme,
        )
    }

    override fun saveProperties(stableMixtapeKey: String, properties: MixtapeVisualProperties) {
        preferences.edit()
            .putInt(prefKey(stableMixtapeKey, "theme_schema"), THEME_SCHEMA)
            .putString(prefKey(stableMixtapeKey, "font"), properties.handwritingFont.name)
            .putInt(prefKey(stableMixtapeKey, "jitter_start_index"), properties.jitterStartIndex)
            .remove(prefKey(stableMixtapeKey, "base_jitter_key"))
            .putString(prefKey(stableMixtapeKey, "embellishment"), properties.embellishment.name)
            .putString(prefKey(stableMixtapeKey, "symbol_color"), properties.symbolColor.name)
            .putString(prefKey(stableMixtapeKey, "name_color"), properties.nameColor.name)
            .putString(prefKey(stableMixtapeKey, "tape_skin"), properties.tapeSkin.name)
            .putString(prefKey(stableMixtapeKey, "spine_skin"), properties.spineSkin.name)
            .putString(prefKey(stableMixtapeKey, "decorative_id"), properties.decorativeId)
            .putString(prefKey(stableMixtapeKey, "cassette_theme"), properties.cassetteTheme.name)
            .putString(prefKey(stableMixtapeKey, "screw_theme"), properties.screwTheme.name)
            .putString(prefKey(stableMixtapeKey, "sticker_theme"), properties.stickerTheme.name)
            .putString(prefKey(stableMixtapeKey, "case_theme"), properties.caseTheme.name)
            .putString(prefKey(stableMixtapeKey, "sleeve_theme"), properties.sleeveTheme.name)
            .apply()
    }

    override fun retainOnly(stableMixtapeKeys: Set<String>) {
        val obsoleteKeys = preferences.all.keys.filter { key -> key.substringAfter(':') !in stableMixtapeKeys }
        if (obsoleteKeys.isEmpty()) return
        preferences.edit().apply {
            obsoleteKeys.forEach { key -> remove(key) }
        }.apply()
    }

    private inline fun <reified T : Enum<T>> enumValue(stableMixtapeKey: String, property: String): T? {
        val saved = preferences.getString(prefKey(stableMixtapeKey, property), null) ?: return null
        return enumValues<T>().firstOrNull { it.name == saved }
    }

    private fun prefKey(stableMixtapeKey: String, property: String): String = "$property:$stableMixtapeKey"

    private companion object {
        const val THEME_SCHEMA = 2
    }
}
