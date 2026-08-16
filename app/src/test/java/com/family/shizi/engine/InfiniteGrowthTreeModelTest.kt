package com.family.shizi.engine

import com.family.shizi.domain.engine.InfiniteGrowthTreeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfiniteGrowthTreeModelTest {
    @Test fun fiftyFiveEntriesBecomeFiveNonOverlappingSegments() {
        val segments = InfiniteGrowthTreeModel.segments(55)
        assertEquals(5, segments.size)
        assertEquals(listOf(0, 11, 22, 33, 44), segments.map { it.firstEntryIndex })
        assertEquals(listOf(10, 21, 32, 43, 54), segments.map { it.lastEntryIndex })
        assertTrue(segments.map { it.visualSeed }.distinct().size == segments.size)
    }

    @Test fun visibleRangeFollowsCurrentEntry() {
        assertEquals(0..1, InfiniteGrowthTreeModel.visibleRange(0, 55))
        assertEquals(1..3, InfiniteGrowthTreeModel.visibleRange(22, 55))
        assertEquals(3..4, InfiniteGrowthTreeModel.visibleRange(54, 55))
    }
}
