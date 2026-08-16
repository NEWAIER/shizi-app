package com.family.shizi.ui.home.components.tree

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun CartoonBranch(index: Int, fruitX: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = size.width / 2f
        val target = fruitX.dp.toPx() + 36.dp.toPx()
        val start = Offset(center, size.height * .62f)
        val end = Offset(target, size.height * .5f)
        val dx = end.x - start.x
        val path = Path().apply {
            moveTo(start.x - 9.dp.toPx(), start.y)
            cubicTo(start.x + dx * .25f, start.y - 3.dp.toPx(), end.x - dx * .15f, end.y + 8.dp.toPx(), end.x, end.y + 4.dp.toPx())
            lineTo(end.x, end.y - 4.dp.toPx())
            cubicTo(end.x - dx * .15f, end.y - 2.dp.toPx(), start.x + dx * .25f, start.y - 13.dp.toPx(), start.x + 9.dp.toPx(), start.y)
            close()
        }
        drawPath(path, Color(0xFF673B29))
        drawPath(path, Color(0xFFA7653D).copy(alpha = .9f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
    }
}
