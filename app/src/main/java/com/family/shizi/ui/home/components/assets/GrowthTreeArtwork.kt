package com.family.shizi.ui.home.components.assets

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val sourceSlice = bitmap.height / 4
    val destinationSlice = size.height / 4f
    repeat(4) { slice ->
        val sourceTop = when (slice) {
            0 -> 0
            1 -> sourceSlice - 4
            2 -> sourceSlice * 2 - 4
            else -> sourceSlice * 3 - 4
        }.coerceAtLeast(0)
        val sourceBottom = if (slice == 3) bitmap.height else ((slice + 1) * sourceSlice + 4).coerceAtMost(bitmap.height)
        drawImage(
            image = bitmap,
            srcOffset = androidx.compose.ui.unit.IntOffset(0, sourceTop),
            srcSize = androidx.compose.ui.unit.IntSize(bitmap.width, sourceBottom - sourceTop),
            dstOffset = androidx.compose.ui.unit.IntOffset(0, (slice * destinationSlice).toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), (destinationSlice + 6.dp.toPx()).toInt()),
            alpha = .98f,
        )
    }
}
