package com.family.shizi.ui.home.tree

import com.family.shizi.R

object TreeSegmentCatalog {
    private val variants = listOf(
        TreeSegmentSpec(
            variantId = "segment_root",
            artworkRes = R.drawable.growth_tree_main,
            fruitAnchors = anchors(
                0.31f to 0.82f, 0.61f to 0.75f, 0.72f to 0.66f, 0.39f to 0.59f,
                0.22f to 0.50f, 0.55f to 0.45f, 0.76f to 0.37f, 0.44f to 0.30f,
                0.25f to 0.22f, 0.58f to 0.14f,
            ),
            stageHoleAnchor = NormalizedPoint(0.48f, 0.07f),
            bottomConnector = TrunkConnector(0.50f, 0.21f),
            topConnector = TrunkConnector(0.52f, 0.23f),
        ),
        TreeSegmentSpec(
            variantId = "segment_left",
            artworkRes = R.drawable.growth_tree_segment_01,
            fruitAnchors = anchors(
                0.63f to 0.84f, 0.34f to 0.76f, 0.20f to 0.67f, 0.51f to 0.60f,
                0.73f to 0.52f, 0.60f to 0.44f, 0.30f to 0.37f, 0.18f to 0.28f,
                0.48f to 0.21f, 0.69f to 0.13f,
            ),
            stageHoleAnchor = NormalizedPoint(0.50f, 0.07f),
            bottomConnector = TrunkConnector(0.52f, 0.23f),
            topConnector = TrunkConnector(0.47f, 0.20f),
        ),
        TreeSegmentSpec(
            variantId = "segment_right",
            artworkRes = R.drawable.growth_tree_segment_02,
            fruitAnchors = anchors(
                0.36f to 0.84f, 0.66f to 0.76f, 0.80f to 0.67f, 0.49f to 0.60f,
                0.27f to 0.52f, 0.40f to 0.44f, 0.70f to 0.37f, 0.82f to 0.28f,
                0.52f to 0.21f, 0.31f to 0.13f,
            ),
            stageHoleAnchor = NormalizedPoint(0.50f, 0.07f),
            bottomConnector = TrunkConnector(0.47f, 0.20f),
            topConnector = TrunkConnector(0.52f, 0.21f),
        ),
    )

    fun specFor(segmentIndex: Int): TreeSegmentSpec = variants[Math.floorMod(segmentIndex, variants.size)]

    fun validateConnection(lower: TreeSegmentSpec, upper: TreeSegmentSpec): Boolean {
        val xDelta = kotlin.math.abs(lower.topConnector.centerX - upper.bottomConnector.centerX)
        val widthDelta = kotlin.math.abs(lower.topConnector.widthFraction - upper.bottomConnector.widthFraction)
        return xDelta <= 0.03f && widthDelta <= 0.04f
    }

    fun allSpecs(): List<TreeSegmentSpec> = variants

    private fun anchors(vararg points: Pair<Float, Float>) = points.map { NormalizedPoint(it.first, it.second) }
}
