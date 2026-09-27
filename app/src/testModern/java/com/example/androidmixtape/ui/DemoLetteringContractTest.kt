package com.example.androidmixtape.ui

import androidx.compose.ui.text.style.TextAlign
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class DemoLetteringContractTest {
    private fun source(name:String)=listOf(File("src/modern/java/com/example/androidmixtape/ui/$name.kt"),File("app/src/modern/java/com/example/androidmixtape/ui/$name.kt")).first { it.exists() }.readText()
    @Test fun inkAlignmentKeepsJustificationWhenNamesOverrun() {
        for(width in listOf(180,320,780))for(ink in listOf(20,55,120)) {
            assertEquals(0,spineTitleLineLeft(width,ink,TextAlign.Left))
            assertEquals((width-ink)/2,spineTitleLineLeft(width,ink,TextAlign.Center))
            assertEquals(width-ink,spineTitleLineLeft(width,ink,TextAlign.Right))
        }
        assertEquals(-30,spineTitleLineLeft(20,50,TextAlign.Right))
        assertEquals(-15,spineTitleLineLeft(20,50,TextAlign.Center))
        assertEquals(0,spineTitleLineLeft(20,50,TextAlign.Left))
    }
    @Test fun nativeSpineAlignsNamesNormallyButClipsOverrunOnlyAtThePaperEdge() {
        val spine=source("DemoPackaging").substringAfter("internal fun DemoSpine(").substringBefore("internal fun DemoTapeLibrary(")
        for(value in listOf("properties.spineSymbolPlacement","properties.spineTextAlignment","properties.nameColor","properties.symbolColor","spineInkHeightFraction=.94f","spineTextAlign=alignment"))assertTrue(spine.contains(value))
        assertTrue(spine.contains("margin+if(placement.onLeft)40f else 0f"))
        assertTrue(spine.contains("spineAlignmentInsetLeft=titleLeft,spineAlignmentInsetRight=titleRight"))
        assertTrue(spine.contains(".clipToBounds()"))
        assertTrue(spine.contains("modifier=Modifier.fillMaxWidth()"))
        assertFalse(spine.contains(".width(titleWidth)"))
        assertTrue(spine.contains("placement.verticalBias"))
    }
    @Test fun fontSettingsDriveVisibleInkNotOldPaddedLineBoxes() {
        assertTrue(source("JitteredHandwritingTextRenderer").contains("spineInkHeightFraction?.times(sizeScale)"))
        val tracks=source("DemoPackaging").substringAfter("internal fun DemoTrackList(")
        assertTrue(tracks.contains("rememberHandwritingInkBounds"))
        assertTrue(tracks.contains("fontSize=32.sp"))
        assertTrue(tracks.contains("fixedRowHeight=rowHeight"))
        assertTrue(tracks.contains("verticalInkBounds=inkBounds"))
        assertFalse(tracks.contains("lineHeightScale=.92f"))
    }
    @Test fun handwritingGalleryUsesExactlyTheListeningScreenComponents() {
        val preview=source("MixtapeApp").substringAfter("private fun HandwritingFontContextPreview(").substringBefore("private fun MixtapeEmbellishment.readableName")
        assertTrue(preview.contains("DemoSpine("))
        assertTrue(preview.contains("DemoTrackList("))
        assertTrue(preview.contains("previewRows=tracks.size"))
        assertFalse(preview.contains("height(88.dp)"))
        assertTrue(source("DemoPackaging").contains("(rowHeight+2.dp)*previewRows.coerceAtLeast(1)+20.dp"))
        assertFalse(preview.contains("CassetteSpineRow("))
    }
    @Test fun namesAlwaysStayOnOneLineAndOnlyHeightSetsTheirScale() {
        val fit=source("FilledSpineTitleRaster")
        assertTrue(fit.contains("val factor=(target-2f).coerceAtLeast(1f)/(bottom-top)"))
        assertTrue(fit.contains("spineTitlePaperOrigin(width,runWidth,alignment,alignmentInsetLeft,alignmentInsetRight)"))
        assertFalse(fit.contains("minByOrNull"))
        assertFalse(fit.contains("TextOverflow.Ellipsis"))
        assertTrue(fit.contains("return SpineTitleRaster(result,0,target)"))
    }
}
