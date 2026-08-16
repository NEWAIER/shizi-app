package com.family.shizi.ui.home.tree

import androidx.annotation.DrawableRes

data class NormalizedPoint(val x: Float, val y: Float) {
    init {
        require(x in 0f..1f)
        require(y in 0f..1f)
    }
}

data class TrunkConnector(val centerX: Float, val widthFraction: Float) {
    init {
        require(centerX in 0f..1f)
        require(widthFraction in 0f..1f)
    }
}

data class TreeSegmentSpec(
    val variantId: String,
    @DrawableRes val artworkRes: Int,
    val fruitAnchors: List<NormalizedPoint>,
    val stageHoleAnchor: NormalizedPoint,
    val bottomConnector: TrunkConnector,
    val topConnector: TrunkConnector,
    val overlapFraction: Float = 0.08f,
) {
    init {
        require(fruitAnchors.size == 10)
        require(overlapFraction in 0f..0.2f)
    }
}
