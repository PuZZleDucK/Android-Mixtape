package com.example.androidmixtape.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidmixtape.viewmodel.*
import com.example.androidmixtape.playback.TransportCueDirection
import kotlin.math.*

internal fun demoPath(data: String): Path = PathParser().parsePathString(data).toPath()
private val shellOutline = demoPath("M33 18H527Q547 18 547 38V313Q547 333 527 333H33Q13 333 13 313V38Q13 18 33 18Z")
private val shellStructure = demoPath("M56 51h448M56 58h448M63 63v184M497 63v184M83 61l24 36m370-36-24 36M235 61l19 74m71-74-19 74M74 257l24 18m388-18-24 18")
private val shellLower = demoPath("M121 282h318l14 45H107Z")
private val tooth = demoPath("M-4-24h8v12l-4 3-4-3Z")
private val screws = listOf(Offset(37f,41f),Offset(523f,41f),Offset(37f,307f),Offset(523f,307f),Offset(280f,286f))

internal fun DrawScope.demoText(text: String, x: Float, y: Float, fontSize: Float, color: Color, bold: Boolean = false, mono: Boolean = false, maxWidth: Float = Float.POSITIVE_INFINITY) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb(); textSize = fontSize
        typeface = Typeface.create(if (mono) "monospace" else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    val measured = paint.measureText(text)
    if (measured > maxWidth) paint.textScaleX = maxWidth / measured
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

internal fun DrawScope.demoScrew(p: Offset, color: Color = Color(0xFFC7C9C5), dark: Boolean = false) {
    drawCircle(Color(0x99111B1B),8f,p)
    drawCircle(if(dark) Color(0xFF394047) else color,6.1f,p)
    drawCircle(Color.White.copy(alpha=.4f),4.8f,p,style=Stroke(.8f))
    val slot = if(dark) Color(0xFF111820) else Color(0xFF35413E)
    drawLine(slot,p+Offset(-3.5f,0f),p+Offset(3.5f,0f),1.7f)
    drawLine(slot,p+Offset(0f,-3.5f),p+Offset(0f,3.5f),1.7f)
    drawLine(Color.White.copy(alpha=.5f),p+Offset(-3f,-3f),p+Offset(2f,-5f),1f)
}

internal fun DrawScope.demoSprocket(center: Offset, color: Color, angle: Float = 0f) {
    translate(center.x, center.y) {
        rotate(angle,Offset.Zero) {
            drawCircle(Color(0xB3152321),29f,Offset.Zero)
            drawCircle(color,26f,Offset.Zero)
            drawCircle(Color(0xFF27342F),18f,Offset.Zero)
            repeat(6) { n -> rotate(n*60f,Offset.Zero) {
                drawPath(tooth,color)
                drawLine(Color.White.copy(alpha=.55f),Offset(-2f,-22f),Offset(2f,-22f),1f)
            } }
            drawCircle(Color(0xFF42514A),2.1f,Offset(0f,-23f))
            drawCircle(Color(0xFF0E1B17),8f,Offset.Zero)
            drawCircle(Color(0xFF818C83),4.3f,Offset.Zero)
            drawLine(Color(0xFFD8DACC),Offset(0f,-6f),Offset(0f,6f),2f)
            drawLine(Color(0xFFD8DACC),Offset(-6f,0f),Offset(6f,0f),2f)
        }
    }
}

private fun DrawScope.winding(center: Offset, radius: Float) {
    drawCircle(Color(0xFF392A20),radius,center)
    drawCircle(Color(0xFF211D17),radius,center,style=Stroke(1.3f))
    var r=30.3f; var i=0
    while(r<radius) {
        drawCircle(if(i%3==0) Color(0x5CAD8B63) else Color(0x8C171A15),r,center,style=Stroke(.45f))
        r+=.9f; i++
    }
    if(radius>32f) drawArc(Color(0x38DEBE8C),223f,80f,false,center-Offset(radius*.96f,radius*.96f),Size(radius*1.92f,radius*1.92f),style=Stroke(1.3f))
    drawCircle(Color(0xFFBFBEB0),30f,center)
    drawCircle(Color(0xFF4A5149),30f,center,style=Stroke(1f))
}

private fun ribbon(radii: Pair<Float,Float>): Path {
    fun tangent(cx:Float,r:Float,gx:Float,side:Float):Offset {
        val dx=gx-cx;val dy=104f;val d2=dx*dx+dy*dy
        val a=r*r/d2;val b=r*sqrt(d2-r*r)/d2
        return Offset(cx+a*dx+side*b*dy,190+a*dy-side*b*dx)
    }
    val l=tangent(174f,radii.first,51f,-1f);val r=tangent(386f,radii.second,509f,1f)
    return Path().apply {moveTo(l.x,l.y);lineTo(51f,294f);quadraticTo(51f,304f,61f,304f);lineTo(499f,304f);quadraticTo(509f,304f,509f,294f);lineTo(r.x,r.y)}
}

internal fun DrawScope.demoSkinArt(skin: String, accent:Color, secondary:Color) {
    when(skin) {
        "ClassicCreamDots" -> listOf(accent,secondary,Color(0xFFD23B2D)).forEachIndexed{i,c->drawCircle(c,8f,Offset(237f+i*23,259f))}
        "BlackMagentaStripe" -> {drawRect(accent,Offset(54f,243f),Size(452f,17f));drawRect(secondary,Offset(54f,260f),Size(452f,8f))}
        "ChromeGreen" -> {drawRect(accent,Offset(54f,248f),Size(452f,17f));repeat(4){drawRoundRect(secondary,Offset(428f+it*16,62f),Size(8f,35f),CornerRadius(2f))}}
        "CharcoalGold" -> {drawRoundRect(accent,Offset(54f,241f),Size(452f,28f),CornerRadius(4f));drawLine(secondary,Offset(70f,133f),Offset(490f,133f),3f)}
        "TranslucentViolet" -> {drawRoundRect(accent.copy(alpha=.42f),Offset(64f,243f),Size(432f,25f),CornerRadius(12f));drawRoundRect(secondary.copy(alpha=.55f),Offset(456f,60f),Size(38f,67f),CornerRadius(12f))}
        "SmokedGreenLowNoise" -> {drawRect(accent.copy(alpha=.75f),Offset(54f,244f),Size(452f,26f));drawLine(secondary.copy(alpha=.7f),Offset(65f,132f),Offset(495f,132f),5f)}
        "TealMeterDeck" -> {drawRoundRect(accent,Offset(370f,62f),Size(125f,35f),CornerRadius(5f));repeat(7){drawRoundRect(secondary,Offset(382f+it*15,72f+it%2*4),Size(5f,16f),CornerRadius(1f))}}
        "IvoryRedStripe" -> {drawRect(secondary,Offset(54f,241f),Size(452f,9f));drawRect(accent,Offset(54f,251f),Size(452f,17f))}
        "IvoryBlue120" -> {drawPath(demoPath("m54 278 131-35h190l131 35Z"),accent.copy(alpha=.85f));drawLine(secondary,Offset(80f,132f),Offset(485f,132f),4f)}
        "RubyFerroGrid" -> {drawRect(accent.copy(alpha=.65f),Offset(54f,243f),Size(452f,23f));repeat(8){drawLine(secondary.copy(alpha=.45f),Offset(80f+it*56,234f),Offset(80f+it*56,278f),2f)}}
        "MidnightCreamSuper" -> {drawRect(secondary.copy(alpha=.42f),Offset(54f,243f),Size(452f,10f));drawCircle(accent,12f,Offset(476f,90f));drawLine(accent,Offset(70f,274f),Offset(490f,274f),5f)}
        "LimeNavyC30" -> {drawRoundRect(accent,Offset(55f,43f),Size(60f,28f),CornerRadius(4f));drawRoundRect(accent,Offset(449f,43f),Size(57f,28f),CornerRadius(4f));drawLine(secondary,Offset(140f,131f),Offset(420f,131f),5f)}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DemoCassette(
    name: String,
    properties: MixtapeVisualProperties,
    progress: Float,
    playing: Boolean,
    modifier: Modifier = Modifier,
    onCustomize: (() -> Unit)? = null,
    transportCue: TransportCueDirection = TransportCueDirection.NONE,
) {
    val themes=rememberDemoThemes()
    val tape=themes.tape(properties.cassetteTheme);val label=themes.sticker(properties.stickerTheme)
    val clear=tape.text("material")=="clear"
    val sticker=tape.flag("sticker",true)&&label.text("shape")!="none"
    val strip=label.text("shape")=="strip"
    val shell=tape.color("shell");val radii=demoWindingRadii(progress)
    var leftAngle by rememberSaveable {mutableFloatStateOf(0f)}
    var rightAngle by rememberSaveable {mutableFloatStateOf(0f)}
    val latestRadii by rememberUpdatedState(radii)
    val motion=platformCounterMotionEnabled()
    val turning=playing||transportCue!=TransportCueDirection.NONE
    LaunchedEffect(playing,transportCue,motion) {
        if(turning&&motion) {
            var last=withFrameNanos{it}
            while(true) withFrameNanos { now ->
                val delta=((now-last)/1_000_000_000f).coerceAtMost(.05f); last=now
                leftAngle=demoAdvanceReel(leftAngle,delta,demoReelDegreesPerSecond(latestRadii.first,playing,transportCue))
                rightAngle=demoAdvanceReel(rightAngle,delta,demoReelDegreesPerSecond(latestRadii.second,playing,transportCue))
            }
        }
    }
    BoxWithConstraints(modifier=modifier.aspectRatio(560f/356f)
        .then(if(onCustomize!=null) Modifier.combinedClickable(onClick={},onLongClickLabel="Customize mixtape",onLongClick=onCustomize) else Modifier)
        .semantics {contentDescription="${tape.name} cassette${if(sticker) ": $name" else ", stickerless mechanism"}${if(turning) ", reels turning" else ""}"
            stateDescription=when(transportCue) {
                TransportCueDirection.FAST_FORWARD -> "Fast-forwarding reels"
                TransportCueDirection.REWIND -> "Rewinding reels"
                TransportCueDirection.NONE -> if(playing)"Playing reels"else"Stopped reels"
            }
        }) {
        val unit=maxWidth/560f
        val density=LocalDensity.current
        Canvas(Modifier.fillMaxSize()) {
            scale(size.width/560f, size.height/356f, Offset.Zero) {
                val window=Path().apply {
                    if(tape.text("window")=="split") {
                        addOval(Rect(126f,142f,222f,238f));addOval(Rect(338f,142f,434f,238f))
                        addRoundRect(RoundRect(Rect(245f,162f,314f,217f),CornerRadius(8f)))
                    } else addRoundRect(RoundRect(Rect(112f,143f,448f,239f),CornerRadius(48f)))
                }
                val shellFace=Path.combine(PathOperation.Difference,shellOutline,window)
                drawPath(shellOutline,tape.color("well",if(clear)Color(0xFFE5EEEA)else Color(0xFF16241F)).copy(alpha=if(clear).16f else 1f))
                drawPath(shellOutline,Color(0xB33C5149),style=Stroke(3f))
                drawRoundRect(shell.copy(alpha=if(clear).13f else 1f),Offset(22f,25f),Size(516f,299f),CornerRadius(14f))
                drawPath(shellStructure,Color(0x52799187),style=Stroke(2f))
                screws.forEach{drawCircle(Color(0x52799187),13f,it,style=Stroke(2f))}
                val tapeRibbon=ribbon(radii)
                drawPath(tapeRibbon,tape.color("ribbon",Color(0xFF35271C)),style=Stroke(3.5f))
                drawPath(tapeRibbon,Color(0xB3B1986F),style=Stroke(.7f))
                winding(Offset(174f,190f),radii.first);winding(Offset(386f,190f),radii.second)
                for(x in listOf(61f,499f)) {
                    drawCircle(Color(0xFF5C685B),9f,Offset(x,294f));drawCircle(Color(0xFFDAE2D0),9f,Offset(x,294f),style=Stroke(1.2f));drawCircle(Color(0xFFB5C2B2),4f,Offset(x,294f))
                }
                drawPath(demoPath("M257 308h46m-40 0v-5m34 5v-5"),Color(0xFFBDA268),style=Stroke(3f))
                drawLine(Color(0xFF444239),Offset(268f,303f),Offset(292f,303f),4f)
                demoSprocket(Offset(174f,190f),tape.color("reel"),leftAngle)
                demoSprocket(Offset(386f,190f),tape.color("reel"),rightAngle)
                if(!clear) drawPath(shellFace,shell)
                clipPath(shellFace) {
                    drawPath(shellOutline,Brush.linearGradient(listOf(Color(0xFFF5FFFA).copy(alpha=if(clear).19f else .65f),shell.copy(alpha=if(clear).05f else 1f),shell.copy(alpha=if(clear).08f else 1f),Color(0xFF172923).copy(alpha=if(clear).12f else .8f)),Offset.Zero,Offset(196f,356f)))
                    drawPath(shellLower,tape.color("lower",shell).copy(alpha=if(clear).12f else .5f))
                    drawPath(shellLower,Color(0x66435A4C),style=Stroke(1.3f))
                    repeat(29){drawLine(Color(0x2E435E4D),Offset(137f+it*10,289f),Offset(137f+it*10,314f),1f)}
                }
                if(clear) {
                    val openings=Path().apply{addOval(Rect(156f,172f,192f,208f));addOval(Rect(368f,172f,404f,208f))}
                    val opticalFace=Path.combine(PathOperation.Difference,shellOutline,openings)
                    drawPath(opticalFace,shell.copy(alpha=tape.number("shellDensity",.3f)),blendMode=BlendMode.Multiply)
                    drawPath(opticalFace,Brush.linearGradient(0f to Color.White.copy(alpha=.22f),.38f to Color.Transparent,.68f to Color.Transparent,.69f to Color.White.copy(alpha=.16f),.75f to Color.Transparent,start=Offset.Zero,end=Offset(560f,356f)))
                }
                clipPath(shellFace) {demoSkinArt(tape.text("skin"),tape.color("accent",tape.color("reel")),tape.color("secondary",tape.color("hardware")))}
                if(tape.has("windowTint")) drawPath(window,tape.color("windowTint").copy(alpha=.09f))
                drawPath(shellOutline,Brush.verticalGradient(listOf(Color(0xD9F8FFF7),shell,Color(0xFF50625B),Color(0xB3F0FAF0)),endY=356f),style=Stroke(3.5f))
                drawRoundRect(Color(0xFFF0F8ED).copy(alpha=if(clear).55f else .3f),Offset(24f,29f),Size(512f,294f),CornerRadius(11f),style=Stroke(1f))
                drawLine(Color(0x59E6F3E5),Offset(40f,81f),Offset(40f,259f),2f);drawLine(Color(0x59E6F3E5),Offset(521f,81f),Offset(521f,259f),2f)
                if(sticker) {
                    val labelHeight=if(strip)94f else 235f
                    val labelBase=Path().apply { addRoundRect(RoundRect(Rect(54f,43f,506f,43f+labelHeight),CornerRadius(12f))) }
                    val cut=Path().apply{addRoundRect(RoundRect(Rect(113f,145f,447f,238f),CornerRadius(46f)))}
                    val shape=if(strip)labelBase else Path.combine(PathOperation.Difference,labelBase,cut)
                    clipPath(shape) {
                        drawPath(labelBase,label.color("paper"))
                        if(label.has("innerPaper")&&!strip)drawRoundRect(label.color("innerPaper"),Offset(72f,74f),Size(416f,163f),CornerRadius(10f))
                        translate(54f,43f) {demoPaperTexture(label,452f,labelHeight)}
                        val accent=label.color("accent");val ink=demoReadableInk(label.color("ink"),label.color("paper"))
                        if(label.text("printLayout")=="android") {
                            drawRect(accent,Offset(54f,43f),if(strip)Size(11f,94f) else Size(452f,26f))
                            demoSkinArt(label.text("skin"),accent,label.color("secondary",ink))
                        } else when(label.text("style")) {
                            "studio"->{drawRect(accent,Offset(54f,246f),Size(452f,32f));drawPath(demoPath("m398 43 41 89h20l-41-89Zm32 0 41 89h13l-41-89Z"),accent);drawLine(ink,Offset(55f,238f),Offset(506f,238f),3f)}
                            "album"->{drawPath(demoPath("M355 48q-125 133 70 225m18-226Q298 170 491 264M391 43q-104 140 64 224"),accent.copy(alpha=.6f),style=Stroke(2f));drawCircle(accent.copy(alpha=.14f),68f,Offset(483f,53f))}
                        }
                        demoText(label.text("brand").take(38),77f,61f,9f,if(label.text("printLayout")=="android"&&!strip) demoReadableInk(label.color("paper"),accent) else ink,true,maxWidth=355f)
                        if(!strip){
                            val bottom=if(label.text("style")=="studio")accent else label.color("paper")
                            val bottomInk=demoReadableInk(ink,bottom)
                            demoText("STEREO",77f,266f,11f,bottomInk,true)
                            demoText(if(label.text("skin")=="IvoryBlue120")"C–120" else if(label.text("skin")=="LimeNavyC30")"C–30" else "C–90",435f,266f,15f,bottomInk,true)
                        }
                    }
                    drawPath(labelBase,label.color("accent").copy(alpha=.4f),style=Stroke(1f))
                }
                drawPath(window,Color(0x736C7B6B),style=Stroke(2f))
                val ticks=if(clear)Color(0x995A6C5C)else Color(0x99C4D3B9)
                drawLine(ticks,Offset(248f,180f),Offset(311f,180f),1f);drawLine(ticks,Offset(248f,199f),Offset(311f,199f),1f)
                repeat(7){drawLine(ticks,Offset(253f+it*8,180f),Offset(253f+it*8,if(it==3)195f else 187f),1f)}
                screws.forEach{demoScrew(it,tape.color("hardware",Color(0xFFC7C9C5)),properties.screwTheme==ScrewTheme.Dark)}
                for(x in listOf(151f,409f)) {drawOval(Color(0xFF26392E),Offset(x-7,311f),Size(14f,10f));drawOval(Color(0xFFB5C3AE),Offset(x-7,311f),Size(14f,10f),style=Stroke(1f))}
                for(x in listOf(209f,317f))drawRoundRect(Color(0xFF23382B),Offset(x,318f),Size(33f,8f),CornerRadius(2f))
                demoText(tape.text("brand"),49f,278f,7.5f,if(clear)Color(0xFF546F5E)else Color(0xFFA8B8AC))
                drawLine(Color.White.copy(alpha=.65f),Offset(64f,23f),Offset(494f,23f),1.5f)
            }
        }
        if(sticker) {
            JitteredHandwritingText(name,properties.jitterStartIndex,
                modifier=Modifier.offset(unit*77,unit*74).width(unit*341).height(unit*47),
                color=demoReadableInk(properties.nameColor.toComposeColor(),label.color("innerPaper",label.color("paper"))),
                fontSize=with(density){(unit*38).toSp()},fontFamily=properties.handwritingFont.cassetteHandwritingFontFamily(),
                fontOpticalScale=properties.handwritingFont.opticalScale(),fontWeight=FontWeight.Bold,fitSpineTitle=true,spineInkHeightFraction=.85f)
            HandDrawnEmbellishment(properties.embellishment,Modifier.offset(unit*437,unit*80).size(unit*42),
                color=demoReadableInk(properties.symbolColor.toComposeColor(),label.color("innerPaper",label.color("paper"))))
        }
    }
}
