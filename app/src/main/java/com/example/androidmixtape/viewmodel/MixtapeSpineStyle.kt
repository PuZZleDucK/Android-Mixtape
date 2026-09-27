package com.example.androidmixtape.viewmodel

/** Persisted per tape: browsing, rotation and playback must never reshuffle its handwriting. */
enum class SpineTextAlignment { Left, Center, Right }
enum class SpineSymbolPlacement(val label:String,val onLeft:Boolean,val verticalBias:Float) {
    UpperLeft("Upper left",true,0f),
    Left("Left",true,.5f),
    LowerLeft("Lower left",true,1f),
    UpperRight("Upper right",false,0f),
    Right("Right",false,.5f),
    LowerRight("Lower right",false,1f),
}

internal data class InitialSpineStyle(
    val alignment:SpineTextAlignment,val symbolPlacement:SpineSymbolPlacement,
    val nameColor:MixtapeSymbolColor,val symbolColor:MixtapeSymbolColor,
)

internal fun initialSpineStyle(seed:Int):InitialSpineStyle {
    val positive=Math.floorMod(seed,5_000)
    val colors=MixtapeSymbolColor.entries
    val nameIndex=(positive*31+17)%colors.size
    val symbolOffset=1+(positive/11)%(colors.size-1)
    return InitialSpineStyle(
        SpineTextAlignment.entries[positive%3],
        SpineSymbolPlacement.entries[(positive/3)%6],
        colors[nameIndex],colors[(nameIndex+symbolOffset)%colors.size],
    )
}

/** Used once for new tapes and pre-variation dev tapes; custom non-default inks are preserved. */
internal fun MixtapeVisualProperties.withInitialSpineStyle():MixtapeVisualProperties {
    val style=initialSpineStyle(jitterStartIndex)
    val defaultInks=nameColor==MixtapeSymbolColor.Navy&&symbolColor==MixtapeSymbolColor.Navy
    return copy(
        spineTextAlignment=style.alignment,spineSymbolPlacement=style.symbolPlacement,
        nameColor=if(defaultInks)style.nameColor else nameColor,
        symbolColor=if(defaultInks)style.symbolColor else symbolColor,
    )
}
