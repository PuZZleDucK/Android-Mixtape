package com.example.androidmixtape.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidmixtape.data.Track
import com.example.androidmixtape.viewmodel.*
import kotlin.math.roundToInt

/** Paper artwork is shared by the back and the flat spine. */
internal fun DrawScope.demoPaperTexture(theme:DemoStyle,w:Float,h:Float) {
    val rule=theme.color("rule",theme.color("ink"))
    when(theme.text("style")) {
        "grid" -> {
            var x=0f;while(x<w){drawLine(rule.copy(alpha=.32f),Offset(x,0f),Offset(x,h),.7f);x+=18f}
            var y=0f;while(y<h){drawLine(rule.copy(alpha=.32f),Offset(0f,y),Offset(w,y),.7f);y+=18f}
        }
        "ruled" -> {var y=11f;while(y<h){drawLine(rule.copy(alpha=if(theme.has("rule")) .5f else .17f),Offset(0f,y),Offset(w,y),.7f);y+=24f}}
    }
    if(theme.has("margin"))drawLine(theme.color("margin").copy(alpha=.7f),Offset(20f,0f),Offset(20f,h),1f)
    if(theme.has("speckle")) {
        val color=theme.color("speckle").copy(alpha=.4f)
        for(y in 0 until h.toInt() step 29)for(x in 0 until w.toInt() step 31){
            drawLine(color,Offset(x+3f,y+8f),Offset(x+5f,y+9f),.8f)
            drawLine(color,Offset(x+19f,y+12f),Offset(x+22f,y+11f),.8f)
        }
    } else {
        for(y in 0 until h.toInt() step 13)for(x in 0 until w.toInt() step 17)
            drawCircle(Color(0x0B172414),.5f,Offset(x+(y%7).toFloat(),y.toFloat()))
    }
}

internal fun DrawScope.paperArt(theme:DemoStyle,spine:Boolean) {
    val w=size.width;val h=size.height
    val paper=theme.color("paper");val inner=theme.color("innerPaper",paper);val accent=theme.color("accent")
    drawRect(if(spine)paper else inner)
    val textureScale = if (spine) w / 420f else density
    scale(textureScale, textureScale, Offset.Zero) { demoPaperTexture(theme,w/textureScale,h/textureScale) }
    if(spine&&theme.text("style")=="album") {
        val path=Path().apply{
            moveTo(w*.52f,-10f);cubicTo(w*.38f,h*.3f,w*1.04f,h*.38f,w*.83f,h*.63f);cubicTo(w*.64f,h*.85f,w*.7f,h,w*.9f,h*1.1f)
        }
        repeat(3){translate(it*w*.045f,0f){drawPath(path,accent.copy(alpha=.28f),style=Stroke(1.2f))}}
        drawCircle(accent.copy(alpha=.10f),w*.15f,Offset(w*.9f,h*.14f))
    }
    if(spine&&theme.text("style")=="studio") {
        drawRect(accent,Offset.Zero,Size(if(spine)w*.024f else w*.01f,h))
        if(spine)drawPath(Path().apply{moveTo(w*.80f,0f);lineTo(w*.816f,0f);lineTo(w*.868f,h);lineTo(w*.852f,h);close()},accent.copy(alpha=.6f))
    }
    if(spine&&theme.has("motif")) {
        val strip=theme.color("strip",accent);val rule=theme.color("rule",accent)
        drawRect(theme.color("border",accent).copy(alpha=.55f),style=Stroke(if(spine)h*.08f else 2f))
        drawRect(inner,Offset(w*.055f,h*.13f),Size(w*.88f,h*.73f))
        drawRoundRect(strip,Offset(w*.04f,h*.14f),Size(w*.01f,h*.72f),CornerRadius(1f))
        when(theme.text("motif")) {
            "plain","double-rail","notebook-rules" -> {
                val lines=if(theme.text("motif")=="notebook-rules")4 else 2
                repeat(lines){val y=h*(.25f+it*if(lines==4).145f else .36f);drawLine(rule.copy(alpha=.25f),Offset(w*.067f,y),Offset(w*.93f,y),.7f)}
                if(theme.text("motif")=="double-rail")drawRect(accent,Offset(w*.95f,h*.18f),Size(w*.007f,h*.64f))
            }
            "sticker-tab" -> drawRoundRect(accent.copy(alpha=.74f),Offset(w*.88f,h*.25f),Size(w*.045f,h*.48f),CornerRadius(2f))
            "collage-blocks" -> listOf(accent,rule,strip,accent).forEachIndexed{i,c->drawRect(c.copy(alpha=.6f),Offset(w*listOf(.07f,.129f,.836f,.89f)[i],h*(.23f+(i%2)*.48f)),Size(w*.032f,h*.1f))}
            "dark-frame" -> drawRect(theme.color("border",accent).copy(alpha=.35f),Offset(w*.064f,h*.2f),Size(w*.87f,h*.59f),style=Stroke(1.4f))
        }
        for(x in listOf(0f,w*.978f))drawRect(theme.color("endCap",paper),Offset(x,0f),Size(w*.022f,h))
    }
}

/** Only a handful of artist themes opt into a small, factory-printed catalogue box. */
internal fun DrawScope.demoSpineCatalogueMark(theme:DemoStyle,code:String) {
    if(!theme.has("catalogueBox"))return
    val unit=size.width/420f
    val side=18f*unit
    val left=if(theme.text("catalogueBox")=="left")7f*unit else size.width-25f*unit
    val top=(size.height-side)/2f
    val ink=theme.color("ink").copy(alpha=.82f)
    val paper=theme.color("paper")
    drawRect(paper,Offset(left,top),Size(side,side))
    drawRect(ink,Offset(left,top),Size(side,side),style=Stroke(.85f*unit))
    val text=code.filter { it.isLetterOrDigit() }.take(2).ifEmpty { "01" }
    // Monospaced factory print, deliberately distinct from the handwritten title.
    val em=(if(text.length==1)11f else 9f)*unit
    val textWidth=text.length*em*.6f
    demoText(text,left+(side-textWidth)/2f,top+side*.5f+em*.35f,em,ink,bold=true,mono=true)
}

/** Light transmission, haze, absorption and moulded highlights are identical on both surfaces. */
@Composable
internal fun DemoCaseSurface(style:DemoStyle,modifier:Modifier=Modifier,spine:Boolean=false,content:@Composable BoxScope.()->Unit) {
    val tint=style.color("tint");val opacity=style.number("opacity")
    val transmission=demoCaseTransmission(tint,opacity)
    val shape=RoundedCornerShape(3.dp)
    Box(modifier.clip(shape).background(tint.copy(alpha=.16f))) {
        Box(Modifier.fillMaxSize().padding(if(spine)2.dp else 5.dp).drawWithCache {
            val matrix=ColorMatrix(floatArrayOf(transmission.red,0f,0f,0f,0f,0f,transmission.green,0f,0f,0f,0f,0f,transmission.blue,0f,0f,0f,0f,0f,1f,0f))
            val paint=Paint().apply{colorFilter=ColorFilter.colorMatrix(matrix)}
            onDrawWithContent {
                drawContext.canvas.saveLayer(Rect(Offset.Zero,size),paint)
                drawContent();drawContext.canvas.restore()
            }
        },content=content)
        Canvas(Modifier.matchParentSize()) {
            val rim=1.2.dp.toPx();val corner=CornerRadius(3.dp.toPx())
            drawRoundRect(tint.copy(alpha=opacity*.23f),cornerRadius=corner)
            drawRoundRect(style.color("hazeColor",Color(0xFFF4F8FA)).copy(alpha=style.number("haze")),cornerRadius=corner)
            drawRoundRect(Brush.linearGradient(0f to Color.White.copy(alpha=.14f),.35f to Color.Transparent,.67f to Color.Transparent,.68f to Color.White.copy(alpha=.19f),.73f to Color.White.copy(alpha=.1f),.74f to Color.Transparent,start=Offset.Zero,end=Offset(size.width,size.height*.6f)),cornerRadius=corner)
            drawRoundRect(Color(0x88778F84),Offset(.5f,.5f),Size(size.width-1,size.height-1),corner,style=Stroke(1f))
            drawRoundRect(Brush.linearGradient(listOf(Color(0xCCFFFFFF),tint.copy(alpha=.6f),Color(0xA6FFFFFF),Color(0x73405953),Color(0xD9FFFFFF)),Offset.Zero,Offset(size.width,size.height)),Offset(rim,rim),Size(size.width-2*rim,size.height-2*rim),corner,style=Stroke(rim))
            val edge=style.color("edge",Color.White);val seam=style.color("hinge",Color(0xFF49645D))
            for(x in listOf(size.width*.025f,size.width*.975f)) {
                drawLine(seam.copy(alpha=.3f),Offset(x,size.height*.18f),Offset(x,size.height*.82f),.65.dp.toPx())
                drawLine(edge.copy(alpha=.5f),Offset(x+1,size.height*.2f),Offset(x+1,size.height*.8f),.6.dp.toPx())
            }
            drawLine(Color.White.copy(alpha=.7f),Offset(rim*2,rim*2),Offset(size.width-rim*2,rim*2),.65.dp.toPx())
            drawLine(tint.copy(alpha=.65f),Offset(rim*2,size.height-rim*2),Offset(size.width-rim*2,size.height-rim*2),.85.dp.toPx())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DemoSpine(name:String,properties:MixtapeVisualProperties,modifier:Modifier=Modifier,current:Boolean=false,onClick:(()->Unit)?=null,onLongClick:(()->Unit)?=null) {
    val themes=rememberDemoThemes();val paper=themes.sleeve(properties.sleeveTheme)
    val ink=demoCaseInk(properties.nameColor.toComposeColor(),paper.color("innerPaper",paper.color("paper")),themes.case(properties.caseTheme))
    val click=if(onClick!=null)Modifier.combinedClickable(role=Role.Button,onClick=onClick,onLongClick=onLongClick,onLongClickLabel=if(onLongClick!=null)"Customize mixtape"else null)else Modifier
    BoxWithConstraints(modifier.then(click).semantics(mergeDescendants=true){contentDescription=if(current)"Current mixtape $name"else"Mixtape $name";selected=current}) {
        val outerWidth=maxWidth
        val height=outerWidth/DEMO_SPINE_ASPECT_RATIO
        DemoCaseSurface(themes.case(properties.caseTheme),Modifier.fillMaxWidth().height(height).then(if(current)Modifier.border(1.5.dp,MaterialTheme.colorScheme.primary,RoundedCornerShape(3.dp))else Modifier),spine=true) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal=outerWidth*.019f,vertical=height*.06f).clipToBounds()) {
                Canvas(Modifier.fillMaxSize()){paperArt(paper,true);demoSpineCatalogueMark(paper,properties.decorativeId)}
                val unit=maxWidth/420f
                val density=LocalDensity.current
                val placement=properties.spineSymbolPlacement
                val margin=if(paper.has("motif"))28f else 16f
                val symbolSize=unit*28f
                val symbolX=if(placement.onLeft)unit*margin else maxWidth-unit*margin-symbolSize
                val symbolY=(maxHeight-symbolSize).coerceAtLeast(0.dp)*placement.verticalBias
                val titleLeft=unit*(margin+if(placement.onLeft)40f else 0f)
                val titleRight=unit*(margin+if(placement.onLeft)0f else 40f)
                val alignment=when(properties.spineTextAlignment) {
                    SpineTextAlignment.Left -> TextAlign.Left
                    SpineTextAlignment.Center -> TextAlign.Center
                    SpineTextAlignment.Right -> TextAlign.Right
                }
                JitteredHandwritingText(name,properties.jitterStartIndex,
                    modifier=Modifier.fillMaxWidth(),
                    color=ink,fontSize=with(density){(unit*72).toSp()},fontFamily=properties.handwritingFont.cassetteHandwritingFontFamily(),
                    fontOpticalScale=properties.handwritingFont.opticalScale(),fontWeight=FontWeight.Bold,fitSpineTitle=true,
                    spineTextAlign=alignment,spineInkHeightFraction=.94f,
                    spineAlignmentInsetLeft=titleLeft,spineAlignmentInsetRight=titleRight)
                HandDrawnEmbellishment(properties.embellishment,Modifier.offset(x=symbolX,y=symbolY).size(symbolSize),
                    demoCaseInk(properties.symbolColor.toComposeColor(),paper.color("innerPaper",paper.color("paper")),themes.case(properties.caseTheme)))
            }
        }
    }
}

/** Only the full-page library uses two columns; the narrow player shelf stays single-column. */
@Composable
internal fun DemoTapeLibrary(groups:List<MixTapeGroup>,currentIndex:Int,listState:LazyListState,gridState:LazyGridState,onSelect:(Int)->Unit,modifier:Modifier=Modifier,onCustomizeTape:(Int)->Unit={}) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if(maxWidth>maxHeight) {
            LazyVerticalGrid(columns=GridCells.Fixed(2),state=gridState,
                modifier=Modifier.fillMaxSize().semantics{contentDescription="Tape list"},
                contentPadding=PaddingValues(4.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                gridItemsIndexed(groups,key={_,group->group.stableKey}){index,group->
                    DemoSpine(group.name,group.visualProperties,Modifier.fillMaxWidth().padding(vertical=4.dp),current=index==currentIndex,onClick={onSelect(index)},onLongClick={onCustomizeTape(index)})
                }
            }
        } else {
            DemoTapeShelf(groups,currentIndex,listState,onSelect,Modifier.fillMaxSize(),centerCurrent=false,onCustomizeTape=onCustomizeTape)
        }
    }
}

@Composable
internal fun DemoTapeShelf(groups:List<MixTapeGroup>,currentIndex:Int,state:LazyListState,onSelect:(Int)->Unit,modifier:Modifier=Modifier,centerCurrent:Boolean=true,onCustomizeTape:(Int)->Unit={}) {
    val density=LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        LaunchedEffect(currentIndex,maxHeight,maxWidth,groups.size,centerCurrent,density) {
            if(centerCurrent&&currentIndex in groups.indices) {
                val target=with(density) {
                    val edge=4.dp.roundToPx()
                    val rowHeight=((maxWidth.roundToPx()-2*edge).coerceAtLeast(0)/DEMO_SPINE_ASPECT_RATIO).roundToInt()+2*edge
                    demoShelfScrollTarget(currentIndex,groups.size,rowHeight,maxHeight.roundToPx(),8.dp.roundToPx(),edge)
                }
                state.scrollToItem(target.index,target.offset)
            }
        }
        // Real, fixed edge padding: centering may never add half a screen of empty shelf.
        LazyColumn(Modifier.fillMaxSize().semantics{contentDescription="Tape list"},state=state,
            contentPadding=PaddingValues(4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            itemsIndexed(groups,key={_,group->group.stableKey}){index,group->
                DemoSpine(group.name,group.visualProperties,Modifier.fillMaxWidth().padding(vertical=4.dp),current=index==currentIndex,onClick={onSelect(index)},onLongClick={onCustomizeTape(index)})
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DemoTrackList(tracks:List<Track>,currentIndex:Int,properties:MixtapeVisualProperties,onSelect:(Int,Track)->Unit,onDelete:(Track)->Unit,onRemove:(Track)->Unit,onInfo:(Track)->Unit,modifier:Modifier=Modifier,previewRows:Int?=null) {
    val themes=rememberDemoThemes();val paper=themes.sleeve(properties.sleeveTheme)
    val background=paper.color("innerPaper",paper.color("paper"))
    val ink=demoCaseInk(Color(properties.sleeveTheme.inkColor(properties.sleeveInk).argb),background,themes.case(properties.caseTheme))
    val accent=demoCaseInk(paper.color("accent"),background,themes.case(properties.caseTheme))
    val density=LocalDensity.current
    val family=properties.handwritingFont.cassetteHandwritingFontFamily()
    val rowStyle=TextStyle(fontFamily=family,fontWeight=FontWeight.ExtraBold,
        fontSize=32.sp*(LocalHandwritingFontSize.current.scale*properties.handwritingFont.opticalScale()))
    val inkBounds=rememberHandwritingInkBounds(rememberTextMeasurer(),rowStyle)
    val rowHeight=with(density) { inkBounds.paddedHeight(rowStyle.fontSize.toPx()).toDp().coerceAtLeast(24.dp) }
    val scroll=rememberLazyListState()
    var menuTrack by remember {mutableStateOf<Long?>(null)}
    LaunchedEffect(currentIndex,tracks) {
        val visible=scroll.layoutInfo.visibleItemsInfo
        if(currentIndex>=0&&currentIndex !in visible.map{it.index})scroll.scrollToItem(currentIndex)
    }
    // Font-gallery cards fit complete rows using exactly the same ink metrics as playback.
    val caseModifier=if(previewRows!=null)modifier.fillMaxWidth().height((rowHeight+2.dp)*previewRows.coerceAtLeast(1)+20.dp)
        else modifier.fillMaxSize()
    DemoCaseSurface(themes.case(properties.caseTheme),caseModifier) {
        Canvas(Modifier.fillMaxSize()){paperArt(paper,false)}
        LazyColumn(Modifier.fillMaxSize().semantics{contentDescription="Track list"},state=scroll,contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)) {
            itemsIndexed(tracks,key={_,track->track.id}) {index,track->
                Box(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().heightIn(min=26.dp)
                        .combinedClickable(role=Role.Button,onClick={onSelect(index,track)},onDoubleClick={onSelect(index,track)},onLongClick={menuTrack=track.id},onLongClickLabel="Track options")
                        .semantics{selected=index==currentIndex;contentDescription=track.title}
                        .then(if(index==currentIndex)Modifier.background(accent.copy(alpha=.055f))else Modifier)
                        .padding(horizontal=4.dp,vertical=1.dp),contentAlignment=Alignment.CenterStart) {
                        JitteredHandwritingText(track.title,properties.jitterStartIndex+index*31,
                            modifier=Modifier.fillMaxWidth(),color=if(index==currentIndex)accent else ink,
                            fontFamily=properties.handwritingFont.cassetteHandwritingFontFamily(),fontOpticalScale=properties.handwritingFont.opticalScale(),
                            fontSize=32.sp,fontWeight=properties.handwritingFont.effectiveCassetteWeight(if(index==currentIndex)FontWeight.ExtraBold else FontWeight.Medium),
                            verticalInkBounds=inkBounds,fixedRowHeight=rowHeight,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    Canvas(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter)){drawLine(ink.copy(alpha=.14f),Offset.Zero,Offset(size.width,0f),1f)}
                    DropdownMenu(modifier=Modifier.semantics { contentDescription="Track actions for ${track.title}" },expanded=menuTrack==track.id,onDismissRequest={menuTrack=null}) {
                        DropdownMenuItem(text={Text("Track info")},onClick={menuTrack=null;onInfo(track)})
                        DropdownMenuItem(text={Text("Remove from mixtape")},onClick={menuTrack=null;onRemove(track)})
                        DropdownMenuItem(text={Text("Delete from device")},onClick={menuTrack=null;onDelete(track)})
                    }
                }
            }
        }
    }
}
