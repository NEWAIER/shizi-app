package com.family.shizi.engine

import com.family.shizi.domain.engine.GrowthMapModel
import org.junit.Assert.assertEquals
import org.junit.Test

/** Contract tests for the frozen one-fruit/one-character loop. */
class SingleCharacterFlowTest {
    private val ids = (1..50).map { "char_$it" }

    @Test fun completingCharacterOneMakesCharacterTwoCurrent() {
        val current = GrowthMapModel.currentEntry(1, emptySet(), ids)
        assertEquals(GrowthMapModel.MapEntry.CharacterNode("char_2", 2), current)
    }

    @Test fun completingCharacterTenStopsAtStageGate() {
        val current = GrowthMapModel.currentEntry(10, emptySet(), ids)
        assertEquals(GrowthMapModel.MapEntry.StageTestNode(0, 10), current)
    }
}
