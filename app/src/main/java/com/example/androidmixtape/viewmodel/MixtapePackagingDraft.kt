package com.example.androidmixtape.viewmodel

import kotlin.random.Random

/** Reroll packaging only. Cassette/screws/sticker and the real music queue are not part of it. */
internal fun MixtapeCustomization.randomizedPackaging(
    random: Random, newName: String, fonts: List<MixtapeHandwritingFont>,
    symbols: List<MixtapeEmbellishment>, cases: List<CaseTheme>, sleeves: List<SleeveTheme>,
): MixtapeCustomization {
    fun <T> pick(options: List<T>, current: T): T = options.filterNot { it == current }
        .ifEmpty { options }.takeIf { it.isNotEmpty() }?.random(random) ?: current
    val nameInk = pick(MixtapeSymbolColor.entries, nameColor)
    val symbolInk = pick(MixtapeSymbolColor.entries.filterNot { it == nameInk }, symbolColor)
    val codes = ('A'..'Z').map { it.toString() } + (0..99).map { it.toString() }
    return copy(
        name = newName,
        decorativeId = pick(codes, decorativeId),
        handwritingFont = pick(fonts, handwritingFont),
        jitterStartIndex = ((jitterStartIndex ?: 0) + random.nextInt(1, 5_000)) % 5_000,
        embellishment = pick(symbols, embellishment),
        nameColor = nameInk, symbolColor = symbolInk,
        spineTextAlignment = pick(SpineTextAlignment.entries, spineTextAlignment ?: SpineTextAlignment.Center),
        spineSymbolPlacement = pick(SpineSymbolPlacement.entries, spineSymbolPlacement ?: SpineSymbolPlacement.Right),
        caseTheme = pick(cases, caseTheme), sleeveTheme = pick(sleeves, sleeveTheme),
        sleeveInk = pick(SleeveInk.entries, sleeveInk),
    )
}
