package com.family.shizi.engine

import com.family.shizi.domain.engine.SingleGrowthTreeGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleGrowthTreeGeometryTest {
    @Test fun visibleCountTracksLearnedPlusDailyTarget() {
        assertEquals(3, SingleGrowthTreeGeometry.visibleCount(0, 3, 50))
        assertEquals(4, SingleGrowthTreeGeometry.visibleCount(1, 3, 50))
        assertEquals(13, SingleGrowthTreeGeometry.visibleCount(10, 3, 50))
        assertEquals(50, SingleGrowthTreeGeometry.visibleCount(49, 3, 50))
        assertEquals(50, SingleGrowthTreeGeometry.visibleCount(50, 3, 50))
        assertEquals(1, SingleGrowthTreeGeometry.visibleCount(0, 0, 1))
    }

    @Test fun treeHeightGrowsWithVisibleCount() {
        val one = SingleGrowthTreeGeometry.treeHeight(1)
        val ten = SingleGrowthTreeGeometry.treeHeight(10)
        val fifty = SingleGrowthTreeGeometry.treeHeight(50)
        assertTrue(ten > one)
        assertTrue(fifty > ten)
        assertEquals(SingleGrowthTreeGeometry.TOP_MARGIN + SingleGrowthTreeGeometry.BOTTOM_MARGIN, one)
        assertEquals(
            SingleGrowthTreeGeometry.TOP_MARGIN + 9 * SingleGrowthTreeGeometry.ROW_HEIGHT + SingleGrowthTreeGeometry.BOTTOM_MARGIN,
            ten,
        )
    }

    @Test fun nodeOneIsAtBottomAndHigherOrdersClimbUp() {
        val visible = 10
        val y1 = SingleGrowthTreeGeometry.nodeY(1, visible)
        val y10 = SingleGrowthTreeGeometry.nodeY(10, visible)
        assertTrue(y1 > y10) // order 1 在底部
        assertEquals(y1 - y10, 9 * SingleGrowthTreeGeometry.ROW_HEIGHT)
    }

    @Test fun nodesSwayAroundCenterWithoutOverlap() {
        val centerX = 180
        val positions = (1..10).map { order -> SingleGrowthTreeGeometry.nodeX(order, centerX) }
        assertEquals(positions.size, positions.toSet().size) // 无重复 → 不重叠
        assertTrue(positions.all { it in 20..340 }) // 360dp 宽度内不越界
    }

    @Test fun trunkPassesThroughNodeColumn() {
        val visible = 10
        val centerX = 180
        val y = SingleGrowthTreeGeometry.nodeY(5, visible)
        val trunkX = SingleGrowthTreeGeometry.trunkXAt(y, visible, centerX)
        val nodeX = SingleGrowthTreeGeometry.nodeX(5, centerX)
        // 主干在节点高度处应接近节点 x（插值结果在节点摆动范围内）。
        assertTrue(kotlin.math.abs(trunkX - nodeX) < 120f)
    }

    @Test fun stageHoleAnchorsSitNextToBatchEnd() {
        val visible = 20
        val centerX = 180
        val y0 = SingleGrowthTreeGeometry.stageHoleY(0, visible)
        val y1 = SingleGrowthTreeGeometry.stageHoleY(1, visible)
        assertTrue(y0 > y1) // 第 1 批树洞在更低处
        val x0 = SingleGrowthTreeGeometry.stageHoleX(0, centerX)
        val x1 = SingleGrowthTreeGeometry.stageHoleX(1, centerX)
        assertTrue(x0 != x1) // 不同批树洞在主干两侧
    }
}
