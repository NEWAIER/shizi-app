package com.family.shizi.ui.home.components.tree

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Composable
fun LeafCluster(index: Int, fruitX: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val colors = listOf(Color(0xFF3D8F4B), Color(0xFF62B957), Color(0xFF9BD66B))
        val points = listOf(
            Offset((fruitX + 5).dp.toPx(), 20.dp.toPx()),
            Offset((fruitX + 48).dp.toPx(), 72.dp.toPx()),
            Offset(size.width / 2f + 24.dp.toPx(), 16.dp.toPx()),
            Offset(size.width / 2f - 34.dp.toPx(), 78.dp.toPx()),
        )
        points.forEachIndexed { leafIndex, point ->
            drawLeaf(point, 14.dp.toPx() + ((index + leafIndex) % 3) * 4.dp.toPx(), ((index + leafIndex) % 4 - 1) * .35f, colors[(index + leafIndex) % colors.size])
        }
    }
}

private fun DrawScope.drawLeaf(center: Offset, length: Float, tilt: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y)
        cubicTo(center.x - length * .55f, center.y - length * .35f, center.x - length * .7f, center.y - length * .9f, center.x - length, center.y - length)
        cubicTo(center.x - length * .25f, center.y - length * 1.08f, center.x + length * .35f, center.y - length * .65f, center.x, center.y)
        close()
    }
    drawPath(path, color)
    drawLine(Color.White.copy(alpha = .22f), center, Offset(center.x - length * .62f, center.y - length * .7f), strokeWidth = 1.4.dp.toPx())
}
