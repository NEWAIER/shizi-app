package com.family.shizi.ui.home.components.assets

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.family.shizi.R

/**
 * Infinite-growth visual layer.
 * The root segment is the uploaded tree master. Upper segments are distinct
 * transparent artworks derived from that same master language; they are laid
 * out once in order and never tiled or looped.
 */
@Composable
fun GrowthTreeArtwork(segmentCount: Int = 5, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val segments = remember(segmentCount) {
        listOf(
            R.drawable.growth_tree_main,
            R.drawable.growth_tree_segment_01,
            R.drawable.growth_tree_segment_02,
            R.drawable.growth_tree_segment_03,
            R.drawable.growth_tree_segment_04,
        ).take(segmentCount.coerceIn(1, 5)).map { resourceId ->
            BitmapFactory.decodeResource(context.resources, resourceId).asImageBitmap()
        }
    }
    androidx.compose.foundation.Canvas(modifier) {
        drawUniqueSegments(segments)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawUniqueSegments(
    segments: List<androidx.compose.ui.graphics.ImageBitmap>,
) {
    if (segments.isEmpty()) return
    val slotHeight = size.height / segments.size
    segments.forEachIndexed { index, image ->
        val targetWidth = size.width * .96f
        val naturalHeight = targetWidth * image.height.toFloat() / image.width.toFloat()
        val targetHeight = maxOf(naturalHeight, slotHeight * 1.06f)
        val x = (size.width - targetWidth) / 2f
        val slotCenter = size.height - slotHeight * (index + .5f)
        val y = slotCenter - targetHeight / 2f
        drawImage(
            image = image,
            dstOffset = IntOffset(x.toInt(), y.toInt()),
            dstSize = IntSize(targetWidth.toInt(), targetHeight.toInt()),
            alpha = .98f,
        )
    }
}
