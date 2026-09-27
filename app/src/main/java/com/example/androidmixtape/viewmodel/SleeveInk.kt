package com.example.androidmixtape.viewmodel

/** A saved palette slot, so changing sleeves always gives the ink a matching colour. */
enum class SleeveInk {
    Original, AlternateOne, AlternateTwo;

    companion object {
        fun fromJitter(jitterStartIndex: Int): SleeveInk = entries[Math.floorMod(jitterStartIndex, entries.size)]
    }
}

data class SleeveInkColor(val label: String, val argb: Long)

fun SleeveTheme.inkColors(): List<SleeveInkColor> = when (this) {
    SleeveTheme.BlankWhite -> listOf(
        SleeveInkColor("Charcoal", 0xFF23272E), SleeveInkColor("Midnight blue", 0xFF1F416D), SleeveInkColor("Forest green", 0xFF25533E),
    )
    SleeveTheme.RuledNotebook -> listOf(
        SleeveInkColor("Blue black", 0xFF202634), SleeveInkColor("Burgundy", 0xFF6A3045), SleeveInkColor("Bottle green", 0xFF235244),
    )
    SleeveTheme.AlbumPrint -> listOf(
        SleeveInkColor("Ivory", 0xFFF1EFE8), SleeveInkColor("Ice blue", 0xFFA9D8F3), SleeveInkColor("Rose", 0xFFF1B4CE),
    )
    SleeveTheme.KraftBrown -> listOf(
        SleeveInkColor("Dark brown", 0xFF33261A), SleeveInkColor("Navy", 0xFF203B56), SleeveInkColor("Deep green", 0xFF204530),
    )
    SleeveTheme.MidnightGrid -> listOf(
        SleeveInkColor("Frost", 0xFFEDF3FF), SleeveInkColor("Lavender", 0xFFCEBBF3), SleeveInkColor("Warm cream", 0xFFF1D3A1),
    )
    SleeveTheme.CoralAlbum -> listOf(
        SleeveInkColor("Plum black", 0xFF38252A), SleeveInkColor("Indigo", 0xFF34415F), SleeveInkColor("Pine", 0xFF2B514C),
    )
    SleeveTheme.ForestFleck -> listOf(
        SleeveInkColor("Pine", 0xFF263426), SleeveInkColor("Plum", 0xFF53354D), SleeveInkColor("Navy", 0xFF2D4460),
    )
    SleeveTheme.BlueprintGrid -> listOf(
        SleeveInkColor("White", 0xFFF2F8FC), SleeveInkColor("Pale mint", 0xFFBEEFD9), SleeveInkColor("Blush", 0xFFFFD5D8),
    )
    SleeveTheme.GraphPaper -> listOf(
        SleeveInkColor("Slate", 0xFF27313B), SleeveInkColor("Plum", 0xFF60365D), SleeveInkColor("Forest", 0xFF2F513B),
    )
    SleeveTheme.StudioIndex -> listOf(SleeveInkColor("Original", 0xFF293A37), SleeveInkColor("Accent", 0xFF9B4525), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.NotebookPage -> listOf(SleeveInkColor("Original", 0xFF46564B), SleeveInkColor("Accent", 0xFF994D3D), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.AfterDark -> listOf(SleeveInkColor("Original", 0xFFE7E1F1), SleeveInkColor("Accent", 0xFFAD95D1), SleeveInkColor("Blue", 0xFF869CB0))
    SleeveTheme.CottonPaper -> listOf(SleeveInkColor("Original", 0xFF385246), SleeveInkColor("Accent", 0xFF55715B), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinCreamRed -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFFAE5664), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinBlushBlue -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFFA2526B), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinSkyGreen -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF4F7782), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinMintAmber -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF936D33), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinLemonNavy -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF7B6824), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinPinkZineBorder -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFFA65B76), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinVioletLibraryStripe -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF6966A9), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinMintCollageTab -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFFAB5952), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinAmberIndexBlock -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF7E6538), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinPowderBlueMarker -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF476E77), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinCoralStickerRail -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF8C584B), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinEmeraldNotebookLines -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF5B7C41), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinSepiaNewsprintFrame -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF76644C), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinBlackPhotoNegative -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFFA03972), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinRainbowCutout -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFFBA4874), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinSunburstCreamRail -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF8C6218), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinAquaLibraryTab -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF995334), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinCandyStripePink -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF1C7D8A), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinGoldenMemoBlock -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF407655), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinTomatoBorderLabel -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF80682E), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinTealNotebookRail -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF995F29), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinVioletIndexPanel -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFFAE4E67), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinLimeCutoutStripe -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF117A86), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinPeachGridSticker -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFF2E7475), SleeveInkColor("Blue", 0xFF365A7B))
    SleeveTheme.SkinMidnightRainbowFrame -> listOf(SleeveInkColor("Original", 0xFF263864), SleeveInkColor("Accent", 0xFFB23767), SleeveInkColor("Blue", 0xFF365A7B))
}

fun SleeveTheme.inkColor(ink: SleeveInk): SleeveInkColor = inkColors()[ink.ordinal]
