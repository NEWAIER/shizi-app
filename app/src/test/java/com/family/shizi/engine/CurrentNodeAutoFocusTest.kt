package com.family.shizi.engine

import com.family.shizi.domain.engine.GrowthMapModel
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentNodeAutoFocusTest {
    @Test fun focusIndexMovesForwardAfterCharacterCompletion() {
        val ids = (1..50).map { "char_$it" }
        val before = GrowthMapModel.currentEntryIndex(0, emptySet(), ids)
        val after = GrowthMapModel.currentEntryIndex(1, emptySet(), ids)
        assertTrue(after > before)
    }

    @Test fun focusIndexMovesToHoleAtTen() {
        val ids = (1..50).map { "char_$it" }
        val index = GrowthMapModel.currentEntryIndex(10, emptySet(), ids)
        assertTrue(index == 10)
    }
}
