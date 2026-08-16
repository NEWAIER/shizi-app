package com.family.shizi.engine

import com.family.shizi.domain.engine.GrowthMapModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthMapModelTest {
    @Test fun chapterCountScalesByTen() {
        assertEquals(0, GrowthMapModel.chapterCount(0))
        assertEquals(1, GrowthMapModel.chapterCount(1))
        assertEquals(1, GrowthMapModel.chapterCount(9))
        assertEquals(1, GrowthMapModel.chapterCount(10))
        assertEquals(2, GrowthMapModel.chapterCount(11))
        assertEquals(5, GrowthMapModel.chapterCount(49))
        assertEquals(5, GrowthMapModel.chapterCount(50))
    }

    @Test fun chapterNamesCoverFiveForests() {
        assertEquals(5, GrowthMapModel.chapters(50).size)
        assertEquals("青草森林", GrowthMapModel.chapters(50)[0].name)
        assertEquals("花朵森林", GrowthMapModel.chapters(50)[1].name)
        assertEquals("小溪森林", GrowthMapModel.chapters(50)[2].name)
        assertEquals("彩云森林", GrowthMapModel.chapters(50)[3].name)
        assertEquals("星光森林", GrowthMapModel.chapters(50)[4].name)
    }

    @Test fun chapterRangesSplitTenCharacters() {
        val chapters = GrowthMapModel.chapters(50)
        assertEquals(1, chapters[0].startNumber)
        assertEquals(10, chapters[0].endNumber)
        assertEquals(11, chapters[1].startNumber)
        assertEquals(20, chapters[1].endNumber)
        assertEquals(41, chapters[4].startNumber)
        assertEquals(50, chapters[4].endNumber)
    }

    @Test fun trailingPartialChapterKeepsRemainingCharacters() {
        val chapters = GrowthMapModel.chapters(13)
        assertEquals(2, chapters.size)
        assertEquals(11, chapters[1].startNumber)
        assertEquals(13, chapters[1].endNumber)
    }

    @Test fun nodeStateTransitions() {
        // 0 字：节点 1 是当前
        assertEquals(GrowthMapModel.NodeState.CURRENT, GrowthMapModel.nodeState(1, 0, 3, 50))
        assertEquals(GrowthMapModel.NodeState.UPCOMING, GrowthMapModel.nodeState(2, 0, 3, 50))
        assertEquals(GrowthMapModel.NodeState.LOCKED, GrowthMapModel.nodeState(4, 0, 3, 50))
        // 1 字：节点 1 完成，节点 2 当前
        assertEquals(GrowthMapModel.NodeState.COMPLETED, GrowthMapModel.nodeState(1, 1, 3, 50))
        assertEquals(GrowthMapModel.NodeState.CURRENT, GrowthMapModel.nodeState(2, 1, 3, 50))
        // 9 字：9 完成，10 当前
        assertEquals(GrowthMapModel.NodeState.COMPLETED, GrowthMapModel.nodeState(9, 9, 3, 50))
        assertEquals(GrowthMapModel.NodeState.CURRENT, GrowthMapModel.nodeState(10, 9, 3, 50))
        // 10 字：10 完成，11 当前
        assertEquals(GrowthMapModel.NodeState.COMPLETED, GrowthMapModel.nodeState(10, 10, 3, 50))
        assertEquals(GrowthMapModel.NodeState.CURRENT, GrowthMapModel.nodeState(11, 10, 3, 50))
        // 11 字：11 完成
        assertEquals(GrowthMapModel.NodeState.COMPLETED, GrowthMapModel.nodeState(11, 11, 3, 50))
        // 49 字：49 完成，50 当前
        assertEquals(GrowthMapModel.NodeState.COMPLETED, GrowthMapModel.nodeState(49, 49, 3, 50))
        assertEquals(GrowthMapModel.NodeState.CURRENT, GrowthMapModel.nodeState(50, 49, 3, 50))
        // 50 字：全部完成，超出课程范围的节点锁定
        assertEquals(GrowthMapModel.NodeState.COMPLETED, GrowthMapModel.nodeState(50, 50, 3, 50))
        assertEquals(GrowthMapModel.NodeState.LOCKED, GrowthMapModel.nodeState(51, 50, 3, 50))
        // 0 节点无效
        assertEquals(GrowthMapModel.NodeState.LOCKED, GrowthMapModel.nodeState(0, 0, 3, 50))
    }

    @Test fun currentChapterTracksProgress() {
        assertEquals(0, GrowthMapModel.currentChapterIndex(0, 50))
        assertEquals(0, GrowthMapModel.currentChapterIndex(9, 50))
        assertEquals(1, GrowthMapModel.currentChapterIndex(10, 50))
        assertEquals(1, GrowthMapModel.currentChapterIndex(11, 50))
        assertEquals(4, GrowthMapModel.currentChapterIndex(49, 50))
        assertEquals(4, GrowthMapModel.currentChapterIndex(50, 50))
    }

    @Test fun chapterUnlockRequiresWholeBatch() {
        assertFalse(GrowthMapModel.chapterUnlocked(0, 9))
        assertTrue(GrowthMapModel.chapterUnlocked(0, 10))
        assertFalse(GrowthMapModel.chapterUnlocked(1, 19))
        assertTrue(GrowthMapModel.chapterUnlocked(1, 20))
        assertTrue(GrowthMapModel.chapterUnlocked(4, 50))
    }
}
