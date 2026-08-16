package com.family.shizi.ui.home.tree

object GrowthTrunkAnchors {
    private val fruit = listOf(
        // Upper-left branch: two fruits.
        NormalizedPoint(0.18f, 0.28f), NormalizedPoint(0.28f, 0.34f),
        // Upper-right branch: two fruits.
        NormalizedPoint(0.82f, 0.28f), NormalizedPoint(0.72f, 0.34f),
        // Lower-left branch: two fruits.
        NormalizedPoint(0.13f, 0.57f), NormalizedPoint(0.25f, 0.59f),
        // Lower-right branch: two fruits.
        NormalizedPoint(0.87f, 0.57f), NormalizedPoint(0.75f, 0.59f),
        // Front center branch: two fruits.
        NormalizedPoint(0.48f, 0.43f), NormalizedPoint(0.58f, 0.46f),
    )

    fun fruitAnchor(localIndex: Int): NormalizedPoint = fruit[localIndex.coerceIn(0, fruit.lastIndex)]

    fun stageHoleAnchor(): NormalizedPoint = NormalizedPoint(0.50f, 0.055f)
}
