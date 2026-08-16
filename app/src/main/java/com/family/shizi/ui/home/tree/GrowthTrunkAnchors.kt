package com.family.shizi.ui.home.tree

object GrowthTrunkAnchors {
    private val fruit = listOf(
        NormalizedPoint(0.28f, 0.84f), NormalizedPoint(0.68f, 0.76f), NormalizedPoint(0.78f, 0.67f),
        NormalizedPoint(0.42f, 0.59f), NormalizedPoint(0.24f, 0.51f), NormalizedPoint(0.58f, 0.43f),
        NormalizedPoint(0.76f, 0.35f), NormalizedPoint(0.40f, 0.28f), NormalizedPoint(0.22f, 0.20f),
        NormalizedPoint(0.61f, 0.12f),
    )

    fun fruitAnchor(localIndex: Int): NormalizedPoint = fruit[localIndex.coerceIn(0, fruit.lastIndex)]

    fun stageHoleAnchor(): NormalizedPoint = NormalizedPoint(0.50f, 0.055f)
}
