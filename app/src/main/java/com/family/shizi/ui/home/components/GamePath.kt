package com.family.shizi.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * 卡通曲线路径：连接同一章节内相邻节点的蜿蜒藤蔓。
 * 纯静态绘制，不运行动画（性能要求）。
 */
@Composable
fun GamePath(
    points: List<DpOffset>,
    modifier: Modifier = Modifier,
    color: Color,
    trunkWidth: Dp = 18.dp,
    branchWidth: Dp = 28.dp,
) {
    if (points.size < 2) return
    Canvas(modifier = modifier) {
        val path = Path().apply {
            val first = points.first()
            moveTo(first.x.toPx(), first.y.toPx())
            var previous = first
            points.drop(1).forEach { point ->
                quadraticTo((previous.x + point.x).toPx() / 2f, previous.y.toPx(), point.x.toPx(), point.y.toPx())
                previous = point
            }
        }
        drawPath(path, color = color.copy(alpha = 0.78f), style = Stroke(width = trunkWidth.toPx(), cap = StrokeCap.Round))
        drawPath(path, color = color.copy(alpha = 0.45f), style = Stroke(width = branchWidth.toPx(), cap = StrokeCap.Round))
    }
}
