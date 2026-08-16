package com.family.shizi.ui.home.components.tree

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun TreeRoots(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = size.width / 2f
        listOf(-1.0f, -.55f, .55f, 1.0f).forEachIndexed { index, side ->
            val path = Path().apply {
                moveTo(center + side * 10.dp.toPx(), size.height * .34f)
                cubicTo(center + side * 24.dp.toPx(), size.height * .42f, center + side * (42 + index * 8).dp.toPx(), size.height * .65f, center + side * (70 + index * 10).dp.toPx(), size.height * .78f)
                lineTo(center + side * (66 + index * 10).dp.toPx(), size.height * .88f)
                cubicTo(center + side * 40.dp.toPx(), size.height * .75f, center + side * 22.dp.toPx(), size.height * .58f, center + side * 4.dp.toPx(), size.height * .48f)
                close()
            }
            drawPath(path, Color(0xFF673B29))
        }
    }
}
