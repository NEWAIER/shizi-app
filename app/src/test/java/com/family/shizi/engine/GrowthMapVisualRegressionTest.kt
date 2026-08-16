package com.family.shizi.engine

import com.family.shizi.domain.engine.GrowthMapModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthMapVisualRegressionTest {
    private val ids = (1..50).map { "char_$it" }

    @Test fun visualMapKeepsFiftyFruitsAndFiveTreeHoles() {
        val entries = GrowthMapModel.entries(ids)
        assertEquals(55, entries.size)
        assertEquals(50, entries.count { it is GrowthMapModel.MapEntry.CharacterNode })
        assertEquals(5, entries.count { it is GrowthMapModel.MapEntry.StageTestNode })
    }

    @Test fun currentFocusCoversRequiredReleaseStates() {
        val states = listOf(
            GrowthMapModel.currentEntry(0, emptySet(), 50),
            GrowthMapModel.currentEntry(10, emptySet(), 50),
            GrowthMapModel.currentEntry(10, setOf(0), 50),
            GrowthMapModel.currentEntry(20, emptySet(), 50),
            GrowthMapModel.currentEntry(50, setOf(0, 1, 2, 3), 50),
        )
        assertTrue(states[0] is GrowthMapModel.MapEntry.CharacterNode)
        assertTrue(states[1] is GrowthMapModel.MapEntry.StageTestNode)
        assertTrue(states[2] is GrowthMapModel.MapEntry.CharacterNode)
        assertTrue(states[3] is GrowthMapModel.MapEntry.StageTestNode)
        assertTrue(states[4] is GrowthMapModel.MapEntry.StageTestNode)
    }
}
