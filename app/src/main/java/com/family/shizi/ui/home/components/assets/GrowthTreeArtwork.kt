package com.family.shizi.ui.home.components.assets

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import com.family.shizi.R

/**
 * The uploaded tree is the visual master. The four source slices keep one continuous
 * warm-brown tree language while allowing the 55-node map to be much taller than a phone.
 */
@Composable
fun GrowthTreeArtwork(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.growth_tree_main).asImageBitmap()
    }
    Canvas(modifier) {
        drawTreeSlices(bitmap)
    }
}

private fun DrawScope.drawTreeSlices(bitmap: androidx.compose.ui.graphics.ImageBitmap) {
    // One complete reference tree anchors the roots and canopy at the bottom.
    // Only a narrow bark crop is extended upward; full tree crowns are never tiled.
    val treeWidth = size.width * .96f
    val treeHeight = treeWidth * bitmap.height.toFloat() / bitmap.width.toFloat()
    val treeX = (size.width - treeWidth) / 2f
    val treeY = (size.height - treeHeight).coerceAtLeast(0f)
    val trunkSourceLeft = (bitmap.width * .39f).toInt()
    val trunkSourceRight = (bitmap.width * .64f).toInt()
    val trunkSourceTop = (bitmap.height * .16f).toInt()
    val trunkSourceBottom = (bitmap.height * .88f).toInt()
    val trunkWidth = size.width * .42f
    val trunkSegmentHeight = trunkWidth * (trunkSourceBottom - trunkSourceTop).toFloat() /
        (trunkSourceRight - trunkSourceLeft).toFloat()
    var y = treeY - trunkSegmentHeight
    while (y > -trunkSegmentHeight) {
        drawImage(
            image = bitmap,
            srcOffset = androidx.compose.ui.unit.IntOffset(trunkSourceLeft, trunkSourceTop),
            srcSize = androidx.compose.ui.unit.IntSize(trunkSourceRight - trunkSourceLeft, trunkSourceBottom - trunkSourceTop),
            dstOffset = androidx.compose.ui.unit.IntOffset(((size.width - trunkWidth) / 2f).toInt(), y.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(trunkWidth.toInt(), trunkSegmentHeight.toInt()),
            alpha = .94f,
        )
        y -= trunkSegmentHeight * .9f
    }
    drawImage(
        image = bitmap,
        dstOffset = androidx.compose.ui.unit.IntOffset(treeX.toInt(), treeY.toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(treeWidth.toInt(), treeHeight.toInt()),
        alpha = .98f,
    )
}
