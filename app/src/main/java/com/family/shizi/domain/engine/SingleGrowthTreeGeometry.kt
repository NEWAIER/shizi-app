package com.family.shizi.domain.engine

/**
 * 单棵参天大树几何模型：树只有一根连续主干，从底部（树根）蜿蜒向上生长。
 * 树高随「已学 + 今日目标」的可见节点数动态增长，节点沿主干两侧交替挂果，
 * 每 10 字一个树洞。纯逻辑、可单测，UI 只负责按锚点摆放。
 */
object SingleGrowthTreeGeometry {
    /** 相邻节点纵向间距（dp）。 */
    const val ROW_HEIGHT = 92

    /** 树根区与树冠区的额外留白（dp）。 */
    const val TOP_MARGIN = 72
    const val BOTTOM_MARGIN = 96

    /** 节点横向蛇形摆动序列（dp，相对树中心）。 */
    val swings = listOf(-96, 28, 118, 58, -38, 92, -128, 8, -72, 64)

    /** 可见节点数：已学 + 今日目标，至少 1，不超过总字数。 */
    fun visibleCount(learnedCount: Int, dailyTarget: Int, totalCharacters: Int): Int =
        (learnedCount + dailyTarget).coerceIn(1, totalCharacters)

    /** 整棵树高度（dp）：随可见节点数生长。 */
    fun treeHeight(visible: Int): Int =
        TOP_MARGIN + (visible - 1) * ROW_HEIGHT + BOTTOM_MARGIN

    /** 节点 [order]（1 起）的纵向位置（dp，自容器顶部向下）。order 1 在最底部。 */
    fun nodeY(order: Int, visible: Int): Int =
        TOP_MARGIN + (visible - order) * ROW_HEIGHT

    /** 节点 [order]（1 起）的横向位置（dp，自容器左侧）。 */
    fun nodeX(order: Int, centerX: Int): Int =
        centerX + swings[Math.floorMod(order - 1, swings.size)]

    /**
     * 主干在高度 [y]（dp）处的横向位置（dp）：用相邻节点的摆动做平滑插值，
     * 使主干成为穿过所有果子的一条连续蜿蜒曲线。
     */
    fun trunkXAt(y: Int, visible: Int, centerX: Int): Float {
        if (visible <= 0) return centerX.toFloat()
        val top = TOP_MARGIN
        val bottom = TOP_MARGIN + (visible - 1) * ROW_HEIGHT
        val clampedY = y.coerceIn(top, bottom)
        val orderFloat = (bottom - clampedY) / ROW_HEIGHT.toFloat() + 1f // 1..visible
        val lower = orderFloat.toInt().coerceIn(1, visible)
        val upper = (lower + 1).coerceAtMost(visible)
        val t = orderFloat - lower
        val xLower = nodeX(lower, centerX).toFloat()
        val xUpper = nodeX(upper, centerX).toFloat()
        return xLower + (xUpper - xLower) * t
    }

    /** 树洞锚点：第 [batchIndex] 批（0 起）树洞的 y（dp）。 */
    fun stageHoleY(batchIndex: Int, visible: Int): Int {
        val afterOrder = (batchIndex + 1) * GrowthMapModel.CHAPTER_SIZE
        val clamped = afterOrder.coerceAtMost(visible)
        return TOP_MARGIN + (visible - clamped) * ROW_HEIGHT + ROW_HEIGHT / 2
    }

    /** 树洞锚点 x（dp）：按批号奇偶交替放在主干两侧。 */
    fun stageHoleX(batchIndex: Int, centerX: Int): Int =
        if (batchIndex % 2 == 0) centerX - 150 else centerX + 150
}
