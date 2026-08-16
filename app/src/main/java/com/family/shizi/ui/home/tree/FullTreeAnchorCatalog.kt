package com.family.shizi.ui.home.tree

/** 整棵树的统一坐标表；坐标相对于 growth_tree_main.webp 归一化。 */
object FullTreeAnchorCatalog {
    private val columns = floatArrayOf(0.24f, 0.38f, 0.50f, 0.62f, 0.76f)
    private val tiers = floatArrayOf(0.90f, 0.82f, 0.74f, 0.66f, 0.58f, 0.50f, 0.42f, 0.34f, 0.26f, 0.18f)

    fun fruitAnchor(order: Int): NormalizedPoint {
        require(order in 1..50)
        val tier = (order - 1) / columns.size
        val column = (order - 1) % columns.size
        val x = if (tier % 2 == 0) columns[column] else columns[columns.lastIndex - column]
        return NormalizedPoint(x, tiers[tier])
    }

    fun stageHoleAnchor(batchIndex: Int): NormalizedPoint {
        require(batchIndex in 0..4)
        return NormalizedPoint(0.50f, 0.86f - batchIndex * 0.16f)
    }
}
