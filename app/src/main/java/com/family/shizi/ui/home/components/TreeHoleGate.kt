package com.family.shizi.ui.home.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * 树洞关卡门：半圆拱形入口，每批（10 字）一个。
 * 已解锁时带呼吸光晕（唯一允许的树洞无限动画），未解锁时静态置灰。
 */
@Composable
fun TreeHoleGate(
    batchIndex: Int,
    number: Int,
    unlocked: Boolean,
    completed: Boolean = false,
    remaining: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = if (unlocked && !completed) rememberInfiniteTransition(label = "tree_hole_glow_$batchIndex").animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "tree_hole_glow_alpha_$batchIndex",
    ).value else 1f
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(width = 118.dp, height = 76.dp)
            .clickable(enabled = unlocked) { onClick() }
            .testTag("home_tree_hole_$number"),
    ) {
        Canvas(Modifier.size(width = 118.dp, height = 76.dp).alpha(if (unlocked) glow else 0.56f)) {
            val holeColor = when {
                completed -> Color(0xFFFFC857)
                unlocked -> Color(0xFF875E4A)
                else -> Color(0xFF6F7280)
            }
            if (unlocked && !completed) {
                drawCircle(Color(0xFFFFE38A).copy(alpha = .3f), radius = 34.dp.toPx(), center = Offset(size.width / 2f, 48.dp.toPx()))
            }
            drawArc(Color(0xFF5B3426), 180f, 180f, true, topLeft = Offset(6.dp.toPx(), 5.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width - 12.dp.toPx(), size.height - 10.dp.toPx()))
            drawArc(holeColor, 180f, 180f, true, topLeft = Offset(10.dp.toPx(), 8.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width - 20.dp.toPx(), size.height - 14.dp.toPx()))
            drawCircle(Color(0xFF2D252A), radius = 28.dp.toPx(), center = Offset(size.width / 2f, 48.dp.toPx()))
            if (completed) {
                drawCircle(Color(0xFFFFF2A1), radius = 3.dp.toPx(), center = Offset(size.width / 2f - 10.dp.toPx(), 43.dp.toPx()))
                drawCircle(Color(0xFFFFF2A1), radius = 3.dp.toPx(), center = Offset(size.width / 2f + 10.dp.toPx(), 43.dp.toPx()))
            } else if (!unlocked) {
                drawArc(Color(0xFFB6B8C4), 180f, 180f, false, topLeft = Offset(size.width / 2f - 8.dp.toPx(), 37.dp.toPx()), size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 20.dp.toPx()), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawRoundRect(Color(0xFFB6B8C4), topLeft = Offset(size.width / 2f - 10.dp.toPx(), 47.dp.toPx()), size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 14.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            }
        }
        Text(if (completed) "已通关" else "第${batchIndex + 1}关", modifier = Modifier.align(Alignment.Center).padding(top = 32.dp), style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}
