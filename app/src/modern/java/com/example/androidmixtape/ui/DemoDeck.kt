package com.example.androidmixtape.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.androidmixtape.viewmodel.*
import com.example.androidmixtape.playback.TransportCueDirection
import kotlin.math.*

/** A single design-space face scales uniformly. Orientation never squashes the counter or bay. */
@Composable
internal fun DemoDeck(
    theme: DeckTheme,
    name: String,
    properties: MixtapeVisualProperties,
    progress: Float = 0f,
    counter: Int = 0,
    counterRevision: Long = 0,
    playing: Boolean = false,
    transportCue: TransportCueDirection = TransportCueDirection.NONE,
    leftLevel: Float = 0f,
    rightLevel: Float = 0f,
    ejected: Boolean = false,
    showTapes: Boolean = false,
    hasTrack: Boolean = true,
    canPrevious: Boolean = true,
    canNext: Boolean = true,
    onEject: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onNext: () -> Unit = {},
    onToggleList: () -> Unit = {},
    onSettings: () -> Unit = {},
    onCustomize: () -> Unit = {},
    preview: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val style=rememberDemoThemes().deck(theme)
    val geometry=demoDeckGeometry(theme)
    val body=style.color("body");val trim=style.color("trim");val ink=style.color("ink");val accent=style.color("accent")
    val face=style.color("counterFace",Color(0xFF17211D));val counterInk=style.color("counterInk",Color(0xFFDDE6BC))
    BoxWithConstraints(modifier.aspectRatio(geometry.aspectRatio).semantics { contentDescription="Cassette player" }) {
        val unit=maxWidth/560f
        val density=LocalDensity.current
        val digital=style.text("counterStyle")=="digital"
        val counterGeometry=demoCounterGeometry(geometry,digital)
        val counterX=counterGeometry.bezel.center.x
        Canvas(Modifier.fillMaxSize()) {
            scale(size.width/560f,size.height/geometry.height,Offset.Zero) {
                val height=geometry.height
                for(x in listOf(40f,450f))drawRoundRect(Color(0xFF17211D),Offset(x,height-12),Size(67f,12f),CornerRadius(4f))
                drawRoundRect(trim,Offset(1f,6f),Size(558f,height-10),CornerRadius(12f))
                drawRoundRect(body,Offset(1f,1f),Size(558f,height-12),CornerRadius(12f))
                drawRoundRect(Brush.linearGradient(listOf(Color.White.copy(alpha=.09f),Color.Transparent,Color.Black.copy(alpha=.1f)),Offset.Zero,Offset(560f,height)),Offset(1f,1f),Size(558f,height-12),CornerRadius(12f))
                drawRoundRect(Color.White.copy(alpha=.28f),Offset(2f,2f),Size(556f,height-14),CornerRadius(11f),style=Stroke(1f))
                if(geometry.android) {
                    drawRoundRect(style.color("panel",body),Offset(18f,18f),Size(524f,geometry.height-40f),CornerRadius(5f))
                    drawRoundRect(trim.copy(alpha=.35f),Offset(18f,18f),Size(524f,geometry.height-40f),CornerRadius(5f),style=Stroke(1.2f))
                }
                if(style.text("finish")=="brushed") {
                    for(i in 8 until height.toInt() step 3)drawLine(Color.White.copy(alpha=if(i%2==0).05f else .025f),Offset(8f,i.toFloat()),Offset(552f,i.toFloat()),.55f)
                }
                val bay=geometry.bay
                drawRoundRect(style.color("wellFrame",trim),bay.topLeft-Offset(4f,4f),Size(bay.width+8,bay.height+8),CornerRadius(10f))
                drawRoundRect(Color.Black.copy(alpha=.4f),bay.topLeft-Offset(1f,1f),Size(bay.width+2,bay.height+2),CornerRadius(9f))
                drawRoundRect(style.color("well",Color(0xFF182521)),bay.topLeft,bay.size,CornerRadius(8f))
                drawRoundRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha=.5f),Color.Transparent),startY=bay.top,endY=bay.top+24),bay.topLeft,bay.size,CornerRadius(8f))
                drawLine(Color.White.copy(alpha=.23f),Offset(bay.left+10,bay.bottom),Offset(bay.right-10,bay.bottom),1f)
                val tape=geometry.tape
                if(ejected) translate(tape.left,tape.top){ scale(tape.width/560f,tape.height/356f,Offset.Zero) {
                    drawRoundRect(Color(0xFF192321),Offset(40f,37f),Size(480f,290f),CornerRadius(14f))
                    demoSprocket(Offset(174f,190f),Color(0xFF87968D));demoSprocket(Offset(386f,190f),Color(0xFF87968D))
                    demoScrew(Offset(72f,78f));demoScrew(Offset(489f,78f))
                    drawRoundRect(Color(0xFF929A90),Offset(237f,284f),Size(86f,38f),CornerRadius(5f))
                    drawRect(Color(0xFF3E4B42),Offset(260f,293f),Size(40f,15f))
                } }
                val power=if(geometry.right)Offset(counterX-17f,if(geometry.android)46f else 35f)else Offset(39f,379f)
                drawCircle(if(playing)accent else ink.copy(alpha=.25f),3.2f,power)
                if(playing)drawCircle(accent.copy(alpha=.16f),6f,power)
                demoText(when(transportCue){TransportCueDirection.FAST_FORWARD->"F.FWD";TransportCueDirection.REWIND->"REW";else->if(playing)"PLAY"else"STANDBY"},power.x+8,power.y+2.5f,6.3f,ink,true)
                if(geometry.right) {
                    val mx=if(geometry.android)477f else 462f
                    val my=if(geometry.android)88f else 57f
                    val mh=if(geometry.android)170f else 132f
                    demoMeter(Offset(mx,my),Size(8f,mh),if(playing)leftLevel else 0f,style.text("meterStyle","studio"),accent,true)
                    demoMeter(Offset(mx+14f,my),Size(8f,mh),if(playing)rightLevel else 0f,style.text("meterStyle","studio"),accent,true)
                    demoText("L",mx+1,my+mh+11f,7f,ink,true);demoText("R",mx+15,my+mh+11f,7f,ink,true)
                } else {
                    val mx=145f;val my=372f
                    demoMeter(Offset(mx,my),Size(158f,8f),if(playing)leftLevel else 0f,"studio",accent,false)
                    demoMeter(Offset(mx,my+14),Size(158f,8f),if(playing)rightLevel else 0f,"studio",accent,false)
                    demoText("L",mx-12,my+7,7f,ink,true);demoText("R",mx-12,my+21,7f,ink,true)
                    demoText("−20    −10       0   +3",mx,my-6,6f,ink.copy(alpha=.7f),mono=true)
                    demoText("◖◗",461f,376f,13f,ink,true)
                    demoText("HIGH FIDELITY",452f,391f,5.5f,ink)
                }
                // Content-sized counter bezel. It stays entirely outside the cassette well.
                val bezel=counterGeometry.bezel
                drawRoundRect(trim.copy(alpha=.8f),bezel.topLeft,bezel.size,CornerRadius(4f))
                drawRoundRect(face,bezel.topLeft+Offset(3f,3f),Size(bezel.width-6,bezel.height-6),CornerRadius(3f))
                drawRoundRect(Color.White.copy(alpha=.13f),bezel.topLeft+Offset(3f,3f),Size(bezel.width-6,bezel.height-6),CornerRadius(3f),style=Stroke(1f))
                demoText("COUNTER",counterX-17,counterGeometry.labelY,6f,ink.copy(alpha=.7f),true)
                val keys=geometry.transport
                drawRoundRect(Color(0xFF17261F),keys.topLeft,keys.size,CornerRadius(5f))
                drawRoundRect(Color.Black.copy(alpha=.5f),keys.topLeft,keys.size,CornerRadius(5f),style=Stroke(2f))
            }
        }
        if(!ejected) DemoCassette(name,properties,progress,playing,
            Modifier.offset(unit*geometry.tape.left,unit*geometry.tape.top).width(unit*geometry.tape.width),
            onCustomize=if(preview)null else onCustomize,transportCue=if(preview)TransportCueDirection.NONE else transportCue)
        val display=counterGeometry.display
        val counterModifier=Modifier.offset(unit*display.left,unit*display.top).size(unit*display.width,unit*display.height)
        if(digital) {
            Canvas(counterModifier.semantics { contentDescription="Tape counter ${counter.coerceIn(0,999).toString().padStart(3,'0')}" }) {
                drawDigitalCounter(counter.coerceIn(0,999).toString().padStart(3,'0'),counterInk)
            }
        } else Box(counterModifier,contentAlignment=Alignment.Center) {
            CompositionLocalProvider(LocalDensity provides Density(density.density*unit.value*counterGeometry.wheelScale,1f)) {
                CounterWheels(counter,playing,counterRevision,counterInk,deckTheme=theme,motionEnabled=!preview)
            }
        }
        val rect=geometry.transport
        val keyWidth=(rect.width-9f-2.5f*5)/6
        val keyHeight=rect.height-9f
        val labels=listOf(if(ejected)"LOAD"else"EJECT","REW","STOP",if(playing)"PAUSE"else"PLAY","F.FWD","")
        val descriptions=listOf(if(ejected)"Load cassette"else"Eject cassette","Rewind to previous track","Stop playback",if(playing)"Pause playback"else"Play cassette","Fast forward to next track",if(showTapes)"Show track list"else"Show tape list")
        val callbacks=listOf(onEject,onPrevious,onStop,onPlayPause,onNext,onToggleList)
        for(index in 0..5) {
            val enabled=!preview&&(index==0||index==5||(!ejected&&hasTrack&&(index!=1||canPrevious)&&(index!=4||canNext)))
            val hitHeight=maxOf(48.dp,unit*keyHeight)
            DemoTransportKey(index,labels[index],descriptions[index],style,pressed=(index==3&&playing)||(index==5&&showTapes)||(index==1&&transportCue==TransportCueDirection.REWIND)||(index==4&&transportCue==TransportCueDirection.FAST_FORWARD),
                enabled=enabled,onClick=callbacks[index],onLongClick=if(index==5)onSettings else null,
                modifier=Modifier.offset(unit*(rect.left+4.5f+index*(keyWidth+2.5f)),unit*(rect.top+4.5f)+(unit*keyHeight-hitHeight)/2)
                    .width(unit*keyWidth).height(hitHeight),visualHeight=unit*keyHeight)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DemoTransportKey(index:Int,label:String,description:String,style:DemoStyle,pressed:Boolean,enabled:Boolean,onClick:()->Unit,onLongClick:(()->Unit)?,modifier:Modifier,visualHeight:androidx.compose.ui.unit.Dp) {
    Box(modifier.combinedClickable(enabled=enabled,role=Role.Button,onClick=onClick,onLongClick=onLongClick,onLongClickLabel=if(onLongClick!=null)"Settings"else null)
        .semantics {contentDescription=description;if(index==3||index==5)selected=pressed},contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(visualHeight)) {
            scale(size.width/78f,size.height/52f,Offset.Zero) {
                val accent=style.color("accent")
                val button=if(index==3)accent else style.color("button",Color(0xFFACB6A5))
                val ink=demoReadableInk(style.color("buttonInk",Color(0xFF354335)),button)
                drawRoundRect(lerp(button,Color.Black,.32f),Offset.Zero,Size(78f,52f),CornerRadius(3f))
                val shift=if(pressed)3f else 0f
                drawRoundRect(Brush.verticalGradient(listOf(lerp(button,Color.White,if(pressed).06f else .22f),button)),Offset(0f,shift),Size(78f,47f),CornerRadius(3f))
                drawRoundRect(Color.White.copy(alpha=.25f),Offset(1f,shift+1),Size(76f,44f),CornerRadius(2f),style=Stroke(.8f))
                if(index==5&&pressed)drawRoundRect(demoReadableInk(accent,button),Offset(2f,shift+2),Size(74f,42f),CornerRadius(2f),style=Stroke(1.8f))
                val keyInk=ink.copy(alpha=if(enabled)1f else .32f)
                translate(39f,if(index==5)24f+shift else 19f+shift) {
                    when(index) {
                        0 -> {drawPath(demoPath("M-8 3 0-7 8 3Z"),keyInk);drawRoundRect(keyInk,Offset(-8f,6f),Size(16f,2.4f),CornerRadius(.3f))}
                        1,4 -> scale(if(index==1)-1f else 1f,1f,Offset.Zero) {
                            drawPath(demoPath("M-11-7 0 0-11 7ZM0-7 11 0 0 7Z"),keyInk)
                        }
                        3 -> {drawPath(demoPath("M-14-7-3 0-14 7Z"),keyInk);drawRoundRect(keyInk,Offset(3f,-7f),Size(3f,14f),CornerRadius(.6f));drawRoundRect(keyInk,Offset(10f,-7f),Size(3f,14f),CornerRadius(.6f))}
                        2 -> drawRoundRect(keyInk,Offset(-6.5f,-6.5f),Size(13f,13f),CornerRadius(1f))
                        5 -> translate(-17f,-17f) {
                            val arrow=demoReadableInk(accent,button)
                            drawPath(demoPath("M4.76 25.84 25.84 4.76M20.06 4.76h5.78v5.78"),arrow,style=Stroke(3f,cap=StrokeCap.Round,join=StrokeJoin.Round))
                            drawPath(demoPath("M29.24 8.16 8.16 29.24M13.94 29.24H8.16v-5.78"),keyInk,style=Stroke(3f,cap=StrokeCap.Round,join=StrokeJoin.Round))
                            repeat(8){rotate(it*45f,Offset(33f,33f)){drawLine(arrow,Offset(33f,29f),Offset(33f,27f),2.5f,StrokeCap.Round)}}
                            drawCircle(arrow,2.9f,Offset(33f,33f),style=Stroke(2.2f))
                        }
                    }
                }
                if(label.isNotEmpty())demoText(label,39f-label.length*2.5f,39f+shift,7.8f,keyInk,true,mono=true)
            }
        }
    }
}

internal fun DrawScope.demoMeter(origin:Offset,extent:Size,level:Float,style:String,accent:Color,vertical:Boolean) {
    val value=if(level.isFinite())level.coerceIn(0f,1f)else 0f
    drawRoundRect(Color(0xCC101511),origin-Offset(2f,2f),Size(extent.width+4,extent.height+4),CornerRadius(2f))
    if(style=="continuous") {
        drawRect(accent.copy(alpha=.12f),origin,extent)
        if(vertical)drawRect(accent,origin+Offset(0f,extent.height*(1-value)),Size(extent.width,extent.height*value))
        else drawRect(accent,origin,Size(extent.width*value,extent.height))
        repeat(10){val p=it/10f;if(vertical)drawLine(Color.Black.copy(alpha=.2f),origin+Offset(0f,extent.height*p),origin+Offset(extent.width,extent.height*p),.5f)}
    } else repeat(10){i->
        val color=when {i<6->Color(0xFF62BF70);i<8->Color(0xFFEAAF48);else->Color(0xFFE45B48)}
        val lit=value>(i+.1f)/10f
        val fill=color.copy(alpha=if(lit)1f else .12f)
        if(vertical) drawRect(fill,origin+Offset(0f,extent.height*(9-i)/10),Size(extent.width,extent.height/10-2f))
        else drawRect(fill,origin+Offset(extent.width*i/10,0f),Size(extent.width/10-2f,extent.height))
    }
}

private fun DrawScope.drawDigitalCounter(value:String,ink:Color) {
    val segments=arrayOf(intArrayOf(0,1,2,4,5,6),intArrayOf(2,5),intArrayOf(0,2,3,4,6),intArrayOf(0,2,3,5,6),intArrayOf(1,2,3,5),intArrayOf(0,1,3,5,6),intArrayOf(0,1,3,4,5,6),intArrayOf(0,2,5),intArrayOf(0,1,2,3,4,5,6),intArrayOf(0,1,2,3,5,6))
    val strokes=arrayOf(floatArrayOf(.24f,.14f,.76f,.14f),floatArrayOf(.19f,.19f,.19f,.48f),floatArrayOf(.81f,.19f,.81f,.48f),floatArrayOf(.24f,.50f,.76f,.50f),floatArrayOf(.19f,.52f,.19f,.81f),floatArrayOf(.81f,.52f,.81f,.81f),floatArrayOf(.24f,.86f,.76f,.86f))
    val cell=size.width/3
    value.forEachIndexed{index,digit->strokes.forEachIndexed{segment,line->
        drawLine(ink.copy(alpha=if(segment in segments[digit.digitToInt()])1f else .10f),Offset((index+line[0])*cell,line[1]*size.height),Offset((index+line[2])*cell,line[3]*size.height),size.height*.075f,StrokeCap.Round)
    }}
}
