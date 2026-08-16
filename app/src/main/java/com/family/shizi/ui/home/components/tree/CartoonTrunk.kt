package com.family.shizi.ui.home.components.tree

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun CartoonTrunk(index: Int, progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = size.width / 2f + sin(index * .28f) * 10.dp.toPx()
        val width = (92f - progress * 50f).dp.toPx()
        val left = Path().apply {
            moveTo(center - width * .58f, size.height + 5.dp.toPx())
            cubicTo(center - width * .72f, size.height * .72f, center - width * .34f, size.height * .55f, center - width * .48f, size.height * .34f)
            cubicTo(center - width * .55f, size.height * .16f, center - width * .28f, 0f, center - width * .25f, -5.dp.toPx())
            lineTo(center + width * .22f, -5.dp.toPx())
            cubicTo(center + width * .05f, size.height * .2f, center + width * .37f, size.height * .34f, center + width * .18f, size.height * .53f)
            cubicTo(center + width * .05f, size.height * .72f, center + width * .6f, size.height * .82f, center + width * .48f, size.height + 5.dp.toPx())
            close()
        }
        drawPath(left, Color(0xFF5B3426))
        val body = Path().apply {
            moveTo(center - width * .45f, size.height + 2.dp.toPx())
            cubicTo(center - width * .56f, size.height * .72f, center - width * .2f, size.height * .55f, center - width * .36f, size.height * .33f)
            cubicTo(center - width * .4f, size.height * .15f, center - width * .18f, 0f, center - width * .17f, -2.dp.toPx())
            lineTo(center + width * .14f, -2.dp.toPx())
            cubicTo(center, size.height * .2f, center + width * .25f, size.height * .34f, center + width * .08f, size.height * .53f)
            cubicTo(center, size.height * .72f, center + width * .47f, size.height * .82f, center + width * .37f, size.height + 2.dp.toPx())
            close()
        }
        drawPath(body, Color(0xFF9A5D38))
        drawPath(body, Color(0xFFC47A4A), style = Stroke(width = 3.dp.toPx()))
        repeat(2) { n ->
            val y = size.height * (.28f + n * .27f)
            drawArc(Color(0xFF673B29).copy(alpha = .55f), 80f, 60f, false, Offset(center - width * .28f, y), androidx.compose.ui.geometry.Size(width * .35f, 10.dp.toPx()), style = Stroke(width = 1.8.dp.toPx()))
        }
    }
}
