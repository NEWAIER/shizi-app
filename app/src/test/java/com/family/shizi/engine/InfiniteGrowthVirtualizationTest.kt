package com.family.shizi.engine

import com.family.shizi.domain.engine.GrowthMapModel
import com.family.shizi.domain.engine.InfiniteGrowthTreeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfiniteGrowthVirtualizationTest {
    @Test fun fiftyCharactersBecomeFiveSegments() {
        val entries = GrowthMapModel.entries((1..50).map { "char_$it" })
        assertEquals(55, entries.size)
        assertEquals(5, InfiniteGrowthTreeModel.segments(entries.size).size)
    }

    @Test fun fiveHundredCharactersBecomeFiftySegments() {
        val entries = GrowthMapModel.entries((1..500).map { "char_$it" })
        val segments = InfiniteGrowthTreeModel.segments(entries.size)
        assertEquals(550, entries.size)
        assertEquals(50, segments.size)
        assertTrue(InfiniteGrowthTreeModel.visibleRange(275, entries.size).count() <= 3)
    }

    @Test fun oneThousandCharactersBecomeOneHundredSegments() {
        val entries = GrowthMapModel.entries((1..1000).map { "char_$it" })
        assertEquals(1100, entries.size)
        assertEquals(100, InfiniteGrowthTreeModel.segments(entries.size).size)
    }
}
