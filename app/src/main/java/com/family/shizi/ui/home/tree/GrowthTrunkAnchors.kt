package com.family.shizi.ui.home.tree

object GrowthTrunkAnchors {
    private val fruit = listOf(
        // 从下到上：底部左枝挂 2 个，其余枝条按不规则高度挂 1 个。
        NormalizedPoint(0.20f, 0.86f), NormalizedPoint(0.30f, 0.79f),
        NormalizedPoint(0.82f, 0.73f),
        NormalizedPoint(0.18f, 0.61f),
        NormalizedPoint(0.82f, 0.53f),
        NormalizedPoint(0.16f, 0.45f), NormalizedPoint(0.28f, 0.50f),
        NormalizedPoint(0.82f, 0.36f),
        NormalizedPoint(0.18f, 0.22f),
        NormalizedPoint(0.82f, 0.15f),
    )

    fun fruitAnchor(localIndex: Int): NormalizedPoint = fruit[localIndex.coerceIn(0, fruit.lastIndex)]

    fun stageHoleAnchor(): NormalizedPoint = NormalizedPoint(0.50f, 0.055f)
}
