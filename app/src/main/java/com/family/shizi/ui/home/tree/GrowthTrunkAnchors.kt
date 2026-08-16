package com.family.shizi.ui.home.tree

object GrowthTrunkAnchors {
    private val fruit = listOf(
        // 严格从下到上：果柄落在枝梢末端，枝条数量和长度不规则。
        NormalizedPoint(0.12f, 0.82f), NormalizedPoint(0.22f, 0.77f),
        NormalizedPoint(0.89f, 0.71f),
        NormalizedPoint(0.12f, 0.60f),
        NormalizedPoint(0.92f, 0.47f), NormalizedPoint(0.82f, 0.50f),
        NormalizedPoint(0.10f, 0.39f),
        NormalizedPoint(0.93f, 0.30f),
        NormalizedPoint(0.18f, 0.16f),
        NormalizedPoint(0.88f, 0.13f),
    )

    fun fruitAnchor(localIndex: Int): NormalizedPoint = fruit[localIndex.coerceIn(0, fruit.lastIndex)]

    fun stageHoleAnchor(): NormalizedPoint = NormalizedPoint(0.50f, 0.055f)
}
