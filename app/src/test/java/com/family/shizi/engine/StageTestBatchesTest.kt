package com.family.shizi.engine

import com.family.shizi.domain.engine.StageTestBatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StageTestBatchesTest {
    @Test fun rangeOfSplitsEveryTenCharacters() {
        assertEquals(0..9, StageTestBatches.rangeOf(0))
        assertEquals(10..19, StageTestBatches.rangeOf(1))
        assertEquals(20..29, StageTestBatches.rangeOf(2))
        assertEquals(40..49, StageTestBatches.rangeOf(4))
    }

    @Test fun batchUnlocksOnlyAfterWholeBatchLearned() {
        assertFalse(StageTestBatches.isUnlocked(0, learnedCount = 9))
        assertTrue(StageTestBatches.isUnlocked(0, learnedCount = 10))
        assertFalse(StageTestBatches.isUnlocked(1, learnedCount = 19))
        assertTrue(StageTestBatches.isUnlocked(1, learnedCount = 20))
    }

    @Test fun unlockedBatchesListOnlyCompletedBatches() {
        assertEquals(emptyList<Int>(), StageTestBatches.unlockedBatches(0))
        assertEquals(emptyList<Int>(), StageTestBatches.unlockedBatches(9))
        assertEquals(listOf(0), StageTestBatches.unlockedBatches(10))
        assertEquals(listOf(0, 1), StageTestBatches.unlockedBatches(20))
        assertEquals(listOf(0, 1, 2, 3, 4), StageTestBatches.unlockedBatches(50))
    }

    @Test fun latestUsableBatchIsMostRecentCompletedBatch() {
        assertNull(StageTestBatches.latestUsableBatch(0))
        assertNull(StageTestBatches.latestUsableBatch(9))
        assertEquals(0, StageTestBatches.latestUsableBatch(10))
        assertEquals(1, StageTestBatches.latestUsableBatch(19))
        assertEquals(1, StageTestBatches.latestUsableBatch(20))
        assertEquals(4, StageTestBatches.latestUsableBatch(50))
    }

    @Test fun characterIdsOfSlicesLearningOrderPerBatch() {
        val order = (1..50).map { "char_$it" }
        assertEquals((1..10).map { "char_$it" }, StageTestBatches.characterIdsOf(order, 0))
        assertEquals((11..20).map { "char_$it" }, StageTestBatches.characterIdsOf(order, 1))
        assertEquals((41..50).map { "char_$it" }, StageTestBatches.characterIdsOf(order, 4))
        // 超出课程范围的批次返回空列表。
        assertEquals(emptyList<String>(), StageTestBatches.characterIdsOf(order, 5))
    }

    @Test fun characterIdsOfShrinksForTrailingPartialBatch() {
        val order = (1..13).map { "char_$it" }
        assertEquals((11..13).map { "char_$it" }, StageTestBatches.characterIdsOf(order, 1))
    }
}
