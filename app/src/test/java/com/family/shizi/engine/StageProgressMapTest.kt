package com.family.shizi.engine

import com.family.shizi.domain.engine.GrowthMapModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StageProgressMapTest {
    private val ids = (1..50).map { "char_$it" }

    @Test fun mapHasFiftyCharactersAndFiveHolesInOrder() {
        val entries = GrowthMapModel.entries(ids)
        assertEquals(55, entries.size)
        assertEquals(50, entries.count { it is GrowthMapModel.MapEntry.CharacterNode })
        assertEquals(5, entries.count { it is GrowthMapModel.MapEntry.StageTestNode })
        assertEquals(10, (entries[10] as GrowthMapModel.MapEntry.StageTestNode).afterCharacterOrder)
        assertEquals(20, (entries[21] as GrowthMapModel.MapEntry.StageTestNode).afterCharacterOrder)
        assertEquals(50, (entries[54] as GrowthMapModel.MapEntry.StageTestNode).afterCharacterOrder)
    }

    @Test fun currentMovesThroughCharacterAndStageGate() {
        assertEquals(10, (GrowthMapModel.currentEntry(9, emptySet(), 50) as GrowthMapModel.MapEntry.CharacterNode).order)
        assertEquals(10, (GrowthMapModel.currentEntry(10, emptySet(), 50) as GrowthMapModel.MapEntry.StageTestNode).afterCharacterOrder)
        assertEquals(11, (GrowthMapModel.currentEntry(10, setOf(0), 50) as GrowthMapModel.MapEntry.CharacterNode).order)
        assertEquals(20, (GrowthMapModel.currentEntry(20, setOf(0), 50) as GrowthMapModel.MapEntry.StageTestNode).afterCharacterOrder)
        assertEquals(50, (GrowthMapModel.currentEntry(49, setOf(0, 1, 2, 3), 50) as GrowthMapModel.MapEntry.CharacterNode).order)
        assertTrue(GrowthMapModel.currentEntry(50, setOf(0, 1, 2, 3), 50) is GrowthMapModel.MapEntry.StageTestNode)
    }
}
