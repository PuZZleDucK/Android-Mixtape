package com.example.androidmixtape.ui

import org.junit.Assert.*
import org.junit.Test

class DemoShelfLayoutTest {
    private fun scroll(index:Int,count:Int=13,height:Int=160,viewport:Int=900,gap:Int=21,edge:Int=11):Long {
        val target=demoShelfScrollTarget(index,count,height,viewport,gap,edge)
        return target.index.toLong()*(height+gap)+target.offset
    }

    @Test fun firstAndNearbyTapesStartAtTheRealTopWithoutBlankSpace() {
        for(index in 0..1) assertEquals(0L,scroll(index))
        assertEquals(DemoShelfScrollTarget(0,0),demoShelfScrollTarget(0,13,160,900,21,11))
    }

    @Test fun lastAndNearbyTapesStopAtTheRealBottomWithoutBlankSpace() {
        val maximum=2L*11+13*160+12*21-900
        for(index in 11..12) assertEquals(maximum,scroll(index))
    }

    @Test fun middleTapesStillOpenAtTheCentre() {
        for(index in 2..10) assertEquals(11L+index*181+(160-900)/2,scroll(index))
    }

    @Test fun shortLibrariesStayTopAlignedRatherThanFloatingInTheCentre() {
        for(count in 1..4)for(index in 0 until count) assertEquals(0L,scroll(index,count=count))
    }

    @Test fun targetsAreBoundedAndSelectedSpinesRemainFullyVisibleAtPhoneAndTabletSizes() {
        for(height in listOf(48,73,160,177))for(viewport in listOf(240,433,900,1501)) {
            for(count in listOf(1,2,7,13,80))for(index in 0 until count) {
                val gap=21;val edge=11
                val content=2L*edge+count*height+(count-1)*gap
                val maximum=(content-viewport).coerceAtLeast(0)
                val target=demoShelfScrollTarget(index,count,height,viewport,gap,edge)
                val offset=target.index.toLong()*(height+gap)+target.offset
                assertTrue(target.index in 0 until count)
                assertTrue(target.offset in 0 until height+gap)
                assertTrue(offset in 0..maximum)
                val top=edge+index.toLong()*(height+gap)-offset
                assertTrue("Current spine top must remain visible",top>=0)
                assertTrue("Current spine bottom must remain visible",top+height<=viewport)
                if(offset>0 && offset<maximum)assertEquals(viewport/2.0,top+height/2.0,1.0)
            }
        }
    }

    @Test fun resizingRecomputesClampedPositionWithoutChangingTheSelectedTape() {
        assertTrue(scroll(6,viewport=433)>scroll(6,viewport=1501))
        assertEquals(0L,scroll(1,viewport=1501))
        assertEquals(2L*11+13*160+12*21-433,scroll(12,viewport=433))
    }

    @Test fun emptyAndUnavailableLayoutsDoNotProduceInvalidPositions() {
        for(target in listOf(
            demoShelfScrollTarget(-1,13,160,900,21,11),
            demoShelfScrollTarget(13,13,160,900,21,11),
            demoShelfScrollTarget(0,0,160,900,21,11),
            demoShelfScrollTarget(0,13,0,900,21,11),
            demoShelfScrollTarget(0,13,160,0,21,11)
        )) assertEquals(DemoShelfScrollTarget(0,0),target)
    }
}
