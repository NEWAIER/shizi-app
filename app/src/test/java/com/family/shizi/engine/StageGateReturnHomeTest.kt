package com.family.shizi.engine

import com.family.shizi.domain.engine.GrowthMapModel
import org.junit.Assert.assertEquals
import org.junit.Test

class StageGateReturnHomeTest {
    private val ids = (1..50).map { "char_$it" }

    @Test fun completingFirstGateReleasesCharacterElevenOnlyAfterGateCompletion() {
        assertEquals(
            GrowthMapModel.MapEntry.StageTestNode(0, 10),
            GrowthMapModel.currentEntry(10, emptySet(), ids),
        )
        assertEquals(
            GrowthMapModel.MapEntry.CharacterNode("char_11", 11),
            GrowthMapModel.currentEntry(10, setOf(0), ids),
        )
    }

    @Test fun completingFinalGateLeavesAllFiftyCharactersAndGatesComplete() {
        val final = GrowthMapModel.currentEntry(50, setOf(0, 1, 2, 3, 4), ids)
        assertEquals(null, final)
    }
}
