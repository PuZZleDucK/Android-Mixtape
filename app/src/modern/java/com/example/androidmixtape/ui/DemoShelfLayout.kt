package com.example.androidmixtape.ui

/** An absolute, bounded list position, expressed in LazyListState's item/offset coordinates. */
internal data class DemoShelfScrollTarget(val index:Int,val offset:Int)

/**
 * Centre the selected spine where the real content permits it. Near either end, stop at the
 * content boundary instead of manufacturing empty rows/padding. Dimensions include the row's
 * vertical padding and are rounded to actual layout pixels before this calculation.
 */
internal fun demoShelfScrollTarget(
    currentIndex:Int,itemCount:Int,itemHeightPx:Int,viewportHeightPx:Int,gapPx:Int,edgePaddingPx:Int
):DemoShelfScrollTarget {
    if(currentIndex !in 0 until itemCount || itemHeightPx<=0 || viewportHeightPx<=0) return DemoShelfScrollTarget(0,0)
    val gap=gapPx.coerceAtLeast(0).toLong()
    val edge=edgePaddingPx.coerceAtLeast(0).toLong()
    val stride=itemHeightPx+gap
    val contentHeight=2*edge+itemCount.toLong()*itemHeightPx+(itemCount-1)*gap
    val maximumScroll=(contentHeight-viewportHeightPx).coerceAtLeast(0)
    val centeredScroll=edge+currentIndex*stride+(itemHeightPx.toLong()-viewportHeightPx)/2
    val scroll=centeredScroll.coerceIn(0,maximumScroll)
    return DemoShelfScrollTarget((scroll/stride).toInt(),(scroll%stride).toInt())
}
