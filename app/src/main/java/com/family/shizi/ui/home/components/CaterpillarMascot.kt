package com.family.shizi.ui.home.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.family.shizi.ui.theme.PrimaryText
import com.family.shizi.ui.theme.SuccessGreen

/**
 * 毛毛虫主角：Canvas 矢量绘制，吃一个果子长一截。
 * 只有整体上下浮动是无限动画（唯一活跃的毛虫），身体段数随 learnedCount 增长。
 */
enum class CaterpillarState { WAITING, LEARNING, HAPPY, CHALLENGE }

@Composable
fun CaterpillarMascot(
    segmentCount: Int,
    state: CaterpillarState = CaterpillarState.WAITING,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    facingLeft: Boolean = false,
) {
    val headSize = 30.dp
    val segmentSize = 22.dp
    val green = SuccessGreen
    val darkGreen = Color(0xFF3FA371)
    val bob = rememberInfiniteTransition(label = "caterpillar_bob").animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "caterpillar_bob_alpha",
    ).value
    Box(modifier.size(width = (segmentCount * 14 + 30).dp, height = height)) {
        Canvas(modifier = Modifier.size(width = (segmentCount * 14 + 30).dp, height = height)) {
            withTransform({
                if (facingLeft) scale(-1f, 1f, pivot = Offset(size.width / 2f, size.height / 2f))
            }) {
            val bodyStartY = 30.dp.toPx() + bob.dp.toPx()
            (segmentCount - 1 downTo 1).forEach { seg ->
                val cx = seg * 14.dp.toPx() + 10.dp.toPx()
                val cy = bodyStartY - (seg % 2) * 4.dp.toPx()
                drawCircle(
                    color = if (seg % 2 == 0) green else darkGreen,
                    radius = segmentSize.toPx() / 2f,
                    center = Offset(cx, cy),
                )
            }
            val headX = (segmentCount * 14 + 10).dp.toPx()
            val headY = bodyStartY - 8.dp.toPx()
            drawCircle(color = green, radius = headSize.toPx() / 2f, center = Offset(headX, headY))
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(headX + 5.dp.toPx(), headY - 5.dp.toPx()))
            drawCircle(color = PrimaryText, radius = 2.dp.toPx(), center = Offset(headX + 6.dp.toPx(), headY - 5.dp.toPx()))
            drawArc(
                color = darkGreen,
                startAngle = 18f,
                sweepAngle = 145f,
                useCenter = false,
                topLeft = Offset(headX - 7.dp.toPx(), headY + 1.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = if (state == CaterpillarState.HAPPY) 2.8.dp.toPx() else 1.8.dp.toPx(), cap = StrokeCap.Round),
            )
            // 触角
            drawLine(
                color = darkGreen,
                start = Offset(headX + 2.dp.toPx(), headY - 12.dp.toPx()),
                end = Offset(headX + 6.dp.toPx(), headY - 20.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = darkGreen, radius = 3.dp.toPx(), center = Offset(headX + 6.dp.toPx(), headY - 21.dp.toPx()))
            drawLine(
                color = darkGreen,
                start = Offset(headX - 6.dp.toPx(), headY - 12.dp.toPx()),
                end = Offset(headX - 10.dp.toPx(), headY - 20.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = darkGreen, radius = 3.dp.toPx(), center = Offset(headX - 10.dp.toPx(), headY - 21.dp.toPx()))
            // 小脚
            drawCircle(color = darkGreen, radius = 2.5.dp.toPx(), center = Offset(headX - 8.dp.toPx(), headY + 12.dp.toPx()))
            drawCircle(color = darkGreen, radius = 2.5.dp.toPx(), center = Offset(headX - 14.dp.toPx(), headY + 14.dp.toPx()))
            }
        }
    }
}
