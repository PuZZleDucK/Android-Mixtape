package com.example.androidmixtape.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Negative origins are intentional: overflowing centred/right-aligned names run off the edge. */
internal fun spineTitleLineLeft(width:Int,inkWidth:Int,alignment:TextAlign):Int = when(alignment) {
    TextAlign.Left,TextAlign.Start -> 0
    TextAlign.Right,TextAlign.End -> width-inkWidth
    else -> (width-inkWidth)/2
}

/** Alignment uses the clear label area; the bitmap itself still spans the entire paper. */
internal fun spineTitlePaperOrigin(width:Int,inkWidth:Int,alignment:TextAlign,left:Int,right:Int):Int {
    val start=left.coerceIn(0,width.coerceAtLeast(0))
    val alignmentWidth=(width-start-right.coerceAtLeast(0)).coerceAtLeast(1)
    return start+spineTitleLineLeft(alignmentWidth,inkWidth,alignment)
}

/** Size a single handwritten line by its ink height only. Never wrap, ellipsize or shrink to
 * fit the width: misjudged space is part of the cassette-label character. A bounded mask clips
 * the overrun at the paper edge, independently of the normal lettering alignment margins.
 */
internal fun rasterizeFilledSpineTitle(
    tokens:List<Pair<String,TextLayoutResult>>,samples:List<HandwritingJitterSample>,em:Float,
    width:Int,height:Int,density:Density,direction:LayoutDirection,alignment:TextAlign,fraction:Float,
    alignmentInsetLeft:Int=0,alignmentInsetRight:Int=0,
):SpineTitleRaster? {
    if(width<=0||height<=2||em<=0||tokens.none { it.first.isNotBlank() })return null
    val target=(height*fraction.coerceIn(.1f,1f)).roundToInt().coerceIn(1,height-2)
    fun sample(index:Int)=samples.getOrNull(index)?.takeIf { handwritingTokenAcceptsPerturbation(tokens[index].first) }
        ?: HandwritingJitterSample(0f,0f,0f,0f)

    // Overlay glyphs in a small probe to measure the entire name's vertical ink extent without
    // allocating a bitmap as wide as an arbitrarily long name. Tracking changes X only.
    val probeWidth=ceil(tokens.maxOf { it.second.size.width }+em*2f).toInt().coerceAtLeast(1)
    val probeHeight=ceil(tokens.maxOf { it.second.size.height }+em*3f).toInt().coerceAtLeast(1)
    val probe=ImageBitmap(probeWidth,probeHeight)
    CanvasDrawScope().draw(density,direction,Canvas(probe),Size(probeWidth.toFloat(),probeHeight.toFloat())) {
        tokens.forEachIndexed { index,(_,layout) ->
            val jitter=sample(index)
            val origin=Offset(em+jitter.dxEm*em,em+jitter.dyEm*em)
            rotate(jitter.rotationDegrees,Offset(origin.x+layout.size.width/2f,origin.y+layout.size.height/2f)) {
                drawText(layout,Color.White,topLeft=origin)
            }
        }
    }
    val bitmap=probe.asAndroidBitmap();val row=IntArray(probeWidth)
    var top=probeHeight;var bottom=0
    for(y in 0 until probeHeight) {
        bitmap.getPixels(row,0,probeWidth,0,y,probeWidth,1)
        if(row.any { it ushr 24!=0 }) {top=minOf(top,y);bottom=y+1}
    }
    bitmap.recycle()
    if(bottom<=top)return null
    val factor=(target-2f).coerceAtLeast(1f)/(bottom-top)
    val advances=tokens.mapIndexed { index,(_,layout) -> layout.size.width+sample(index).trackingEm*em }
    val runWidth=(advances.sum()*factor).roundToInt()
    val originX=spineTitlePaperOrigin(width,runWidth,alignment,alignmentInsetLeft,alignmentInsetRight).toFloat()
    val result=ImageBitmap(width,target)
    CanvasDrawScope().draw(density,direction,Canvas(result),Size(width.toFloat(),target.toFloat())) {
        translate(left=originX,top=1f-top*factor) {
            scale(factor,factor,pivot=Offset.Zero) {
                var cursor=0f
                tokens.forEachIndexed { index,(_,layout) ->
                    val jitter=sample(index)
                    // Skip off-canvas glyphs, but keep their advance for alignment and clipping.
                    val left=originX+(cursor-em)*factor
                    val right=originX+(cursor+layout.size.width+em)*factor
                    if(right>=0&&left<=width) {
                        val origin=Offset(cursor+jitter.dxEm*em,em+jitter.dyEm*em)
                        rotate(jitter.rotationDegrees,Offset(origin.x+layout.size.width/2f,origin.y+layout.size.height/2f)) {
                            drawText(layout,Color.White,topLeft=origin)
                        }
                    }
                    cursor+=advances[index]
                }
            }
        }
    }
    return SpineTitleRaster(result,0,target)
}
