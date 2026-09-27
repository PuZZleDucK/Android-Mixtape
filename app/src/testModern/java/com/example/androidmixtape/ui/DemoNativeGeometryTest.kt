package com.example.androidmixtape.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.androidmixtape.viewmodel.DeckTheme
import org.junit.Assert.*
import org.junit.Test

class DemoNativeGeometryTest {
    @Test fun windingsReachBothEndsAndConserveTapeArea() {
        assertEquals(79f, demoWindingRadii(0f).first, .001f)
        assertEquals(30f, demoWindingRadii(0f).second, .001f)
        assertEquals(30f, demoWindingRadii(1f).first, .001f)
        assertEquals(79f, demoWindingRadii(1f).second, .001f)
        for (step in 0..100) {
            val (a,b) = demoWindingRadii(step / 100f)
            assertEquals(79f*79f+30f*30f, a*a+b*b, .002f)
        }
        assertEquals(demoWindingRadii(0f),demoWindingRadii(Float.NaN))
        assertEquals(demoWindingRadii(0f),demoWindingRadii(-1f))
        assertEquals(demoWindingRadii(1f),demoWindingRadii(2f))
    }

    @Test fun everyDeckFitsAtPhonePortraitAndLandscapeWidthsWithoutChangingAspect() {
        for (theme in DeckTheme.entries) {
            val geometry=demoDeckGeometry(theme)
            for (width in listOf(280f,320f,387f,440f,700f)) {
                val height=width/geometry.aspectRatio
                assertEquals(560f/geometry.height,width/height,.00001f)
                assertTrue(geometry.transport.bottom<geometry.height)
                assertTrue(geometry.transport.right<560f)
                assertTrue(geometry.transport.top>geometry.bay.bottom)
                // The cassette SVG has a small transparent artboard margin.
                val shellLeft=geometry.tape.left+13f/560f*geometry.tape.width
                val shellRight=geometry.tape.left+547f/560f*geometry.tape.width
                val shellTop=geometry.tape.top+18f/356f*geometry.tape.height
                val shellBottom=geometry.tape.top+333f/356f*geometry.tape.height
                assertTrue(shellLeft>=geometry.bay.left && shellRight<=geometry.bay.right)
                assertTrue(shellTop>=geometry.bay.top && shellBottom<=geometry.bay.bottom)
                for(digital in listOf(false,true)) {
                    val counter=demoCounterGeometry(geometry,digital)
                    assertTrue("Counter bezel clears cassette",!counter.bezel.overlaps(geometry.bay))
                    assertTrue(counter.bezel.left>0f && counter.bezel.right<560f)
                    assertTrue(counter.display.left>counter.bezel.left && counter.display.right<counter.bezel.right)
                    assertTrue(counter.display.top>counter.bezel.top && counter.display.bottom<counter.bezel.bottom)
                    assertTrue("Counter and its label clear buttons",counter.labelY+3f<geometry.transport.top)
                }
                if(geometry.android) {
                    val panelBottom=geometry.height-22f
                    assertTrue("Buttons have a visible face margin",panelBottom-geometry.transport.bottom>=10f)
                }
            }
        }
    }

    @Test fun demoAndAppDecksKeepTheirDistinctChassisRatios() {
        assertEquals(560f/422f,demoDeckGeometry(DeckTheme.SafetyYellow).aspectRatio,.0001f)
        assertEquals(1.09375f,demoDeckGeometry(DeckTheme.GraphiteTall).aspectRatio,.0001f)
        assertEquals(1.612f,demoDeckGeometry(DeckTheme.StudioSilver).aspectRatio,.001f)
    }

    @Test fun tallerSpineRetainsSameProportionsAtDifferentDisplayWidths() {
        assertEquals(6.5f,DEMO_SPINE_ASPECT_RATIO,0f)
        for(width in listOf(280f,387f,700f)) {
            assertTrue(width/DEMO_SPINE_ASPECT_RATIO>width/8.75f)
            assertEquals(6.5f,width/(width/DEMO_SPINE_ASPECT_RATIO),.00001f)
        }
    }

    @Test fun counterDigitsGrowWithoutReturningToAnOversizedFrame() {
        for(theme in DeckTheme.entries)for(digital in listOf(false,true)) {
            val counter=demoCounterGeometry(demoDeckGeometry(theme),digital)
            assertTrue(counter.display.width>if(digital)54f else 40f)
            assertTrue(counter.display.height>if(digital)22f else 21f)
            assertEquals(5f,counter.display.left-counter.bezel.left,0f)
            assertEquals(4f,counter.display.top-counter.bezel.top,0f)
        }
    }

    @Test fun opticalTintAffectsAllThreeChannelsWithoutChangingAlpha() {
        val clear=demoCaseTransmission(Color.White,.4f)
        assertEquals(Color.White,clear)
        val blue=demoCaseTransmission(Color(0xFF187EFF),.3f)
        assertTrue(blue.red<blue.green && blue.green<blue.blue)
        assertEquals(1f,blue.alpha,0f)
        val light=demoCaseTransmission(Color(0xFF12141A),.1f)
        val dark=demoCaseTransmission(Color(0xFF12141A),.4f)
        assertTrue(dark.red<light.red && dark.green<light.green && dark.blue<light.blue)
    }

    @Test fun chosenInkRemainsReadableOnLightAndDarkPapers() {
        for(paper in listOf(Color(0xFF101319),Color(0xFFF8F6EC),Color(0xFF155281),Color(0xFFC9A876))) {
            for(ink in listOf(Color(0xFF263864),Color(0xFFD8A53F),Color(0xFFF1EFE8))) {
                val adjusted=demoReadableInk(ink,paper)
                val contrast=(maxOf(adjusted.luminance(),paper.luminance())+.05f)/(minOf(adjusted.luminance(),paper.luminance())+.05f)
                assertTrue("$adjusted on $paper has contrast $contrast",contrast>=4.5f)
            }
        }
    }
}
