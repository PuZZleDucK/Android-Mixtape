package com.example.androidmixtape.ui

import androidx.compose.ui.text.style.TextAlign
import com.example.androidmixtape.playback.TransportCueDirection as Cue
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class DemoPaperEdgeAndMotionTest {
    @Test fun alignmentMarginsDoNotNarrowThePaperClip() {
        assertEquals(20,spineTitlePaperOrigin(420,200,TextAlign.Left,20,60))
        assertEquals(90,spineTitlePaperOrigin(420,200,TextAlign.Center,20,60))
        assertEquals(160,spineTitlePaperOrigin(420,200,TextAlign.Right,20,60))
        // A 520-pixel run crosses the PAPER boundary at zero; it is not cut at the 20px text margin.
        assertEquals(-70,spineTitlePaperOrigin(420,520,TextAlign.Center,20,60))
        assertEquals(-160,spineTitlePaperOrigin(420,520,TextAlign.Right,20,60))
        assertEquals(20,spineTitlePaperOrigin(420,520,TextAlign.Left,20,60))
    }
    @Test fun playbackIsAnticlockwiseAndFastWindingHasTheCorrectSignsAndSpeed() {
        for(p in 0..100)for(radius in listOf(demoWindingRadii(p/100f).first,demoWindingRadii(p/100f).second)) {
            val play=demoReelDegreesPerSecond(radius,true,Cue.NONE)
            val forward=demoReelDegreesPerSecond(radius,false,Cue.FAST_FORWARD)
            val reverse=demoReelDegreesPerSecond(radius,false,Cue.REWIND)
            assertTrue(play<0f);assertTrue(forward<0f);assertTrue(reverse>0f)
            assertEquals(play*6,forward,.001f);assertEquals(-forward,reverse,.001f)
            assertEquals(0f,demoReelDegreesPerSecond(radius,false,Cue.NONE),0f)
        }
    }
    @Test fun frameIntegrationKeepsContinuityAndNormalizesNegativeAngles() {
        assertEquals(357f,demoAdvanceReel(0f,1f/30f,-90f),.001f)
        assertEquals(3f,demoAdvanceReel(0f,1f/30f,90f),.001f)
        assertEquals(213f,demoAdvanceReel(213f,.03f,0f),.001f)
        assertTrue(demoReelDegreesPerSecond(Float.NaN,true,Cue.NONE).isFinite())
        assertEquals(180f,demoAdvanceReel(180f,Float.NaN,90f),.001f)
    }
    @Test fun onlySixOfThirtyEightBoundPapersOptIntoCatalogueBoxes() {
        val file=listOf(File("src/modern/assets/demo-themes.json"),File("app/src/modern/assets/demo-themes.json")).first { it.exists() }
        val source=file.readText()
        val bindings=source.substringAfter("\"sleeves\": {").substringBefore("}")
        assertEquals(38,bindings.lines().count { it.contains(":") })
        assertEquals(6,Regex("\"catalogueBox\":").findAll(source).count())
    }
    @Test fun nativePlayerConsumesExplicitCueStateNotHumanReadableMessages() {
        fun source(name:String)=listOf(File("src/modern/java/com/example/androidmixtape/ui/$name.kt"),File("app/src/modern/java/com/example/androidmixtape/ui/$name.kt")).first { it.exists() }.readText()
        assertTrue(source("DemoPlayerScreen").contains("transportCue=state.transportCueDirection"))
        assertTrue(source("DemoCassette").contains("LaunchedEffect(playing,transportCue,motion)"))
        assertTrue(source("DemoCassette").contains("demoReelDegreesPerSecond"))
        assertFalse(source("DemoPlayerScreen").contains("state.message"))
        assertTrue(source("DemoPackaging").contains("onLongClick={onCustomizeTape(index)}"))
        assertTrue(source("MixtapeApp").contains("onRandomizeMixtapePackaging(group.stableKey, it)"))
    }
}
