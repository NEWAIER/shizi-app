package com.family.shizi.ui.home.components.assets

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.family.shizi.R

/**
 * 用主树图片（growth_tree_main.webp）渲染一棵连续参天大树。
 *
 * 图片是一棵完整卡通树（顶部树冠、中部树干、底部树根）。为使树随可见节点数
 * （已学 + 今日目标）向上生长，同时保持图片质感：图片被纵向切成 4 条，
 * 底行固定显示树根，顶行固定显示树冠，中间行循环延展树干段；行数增加时
 * 树干被拉长，形成一棵连续长高的大树。
 */
@Composable
fun GrowthTreeArtwork(
    visibleCount: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.growth_tree_main).asImageBitmap()
    }
    Canvas(modifier) {
        drawContinuousTree(bitmap, visibleCount)
    }
}

private fun DrawScope.drawContinuousTree(bitmap: ImageBitmap, visibleCount: Int) {
    if (size.height <= 0f) return
    val rows = visibleCount.coerceAtLeast(1)
    val destinationSlice = size.height / rows.toFloat()
    val sliceCount = 4
    val sourceSlice = bitmap.height / sliceCount.toFloat()

    repeat(rows) { row ->
        val sourceRow = when {
            // 底行：树根；顶行：树冠；中间循环树干。
            row == 0 -> sliceCount - 1
            row == rows - 1 -> 0
            else -> 1 + Math.floorMod(row - 1, sliceCount - 2)
        }
        val sourceTop = (sourceRow * sourceSlice).toInt().coerceAtLeast(0)
        val sourceBottom = if (sourceRow == sliceCount - 1) bitmap.height else ((sourceRow + 1) * sourceSlice).toInt().coerceAtMost(bitmap.height)
        // 相邻行重叠 3dp，避免拼接缝隙。
        val dstTop = (row * destinationSlice - if (row > 0) 3.dp.toPx() else 0f)
        val dstHeight = destinationSlice + if (row < rows - 1) 3.dp.toPx() else 0f
        drawImage(
            image = bitmap,
            srcOffset = androidx.compose.ui.unit.IntOffset(0, sourceTop),
            srcSize = androidx.compose.ui.unit.IntSize(bitmap.width, sourceBottom - sourceTop),
            dstOffset = androidx.compose.ui.unit.IntOffset(0, dstTop.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), dstHeight.toInt()),
            alpha = .98f,
        )
    }
}
