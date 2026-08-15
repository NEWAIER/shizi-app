package com.family.shizi.ui.home.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.family.shizi.domain.engine.GrowthMapModel
import com.family.shizi.ui.theme.Coral
import com.family.shizi.ui.theme.PrimaryText
import com.family.shizi.ui.theme.StarYellow

/**
 * 地图节点：一个汉字果子。
 * 仅 [GrowthMapModel.NodeState.CURRENT] 节点使用无限动画（呼吸闪烁），
 * 已完成与未解锁节点保持静态，避免 50 节点同时运行动画。
 */
@Composable
fun GameMapNode(
    character: String,
    number: Int,
    state: GrowthMapModel.NodeState,
    onClick: () -> Unit,
    onTap: () -> Unit = {},
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val sparkleAlpha = if (state == GrowthMapModel.NodeState.CURRENT) {
        rememberInfiniteTransition(label = "map_node_current_$number").animateFloat(
            initialValue = 0.62f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse),
            label = "map_node_current_alpha_$number",
        ).value
    } else 1f
    val nodeSize = if (state == GrowthMapModel.NodeState.CURRENT) 72.dp else size

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            "$number",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        )
        Box(
            modifier = Modifier
                .size(nodeSize)
                .clip(CircleShape)
                .background(
                    when (state) {
                        GrowthMapModel.NodeState.COMPLETED ->
                            Brush.linearGradient(listOf(StarYellow, StarYellow.copy(alpha = 0.72f)))
                        GrowthMapModel.NodeState.CURRENT, GrowthMapModel.NodeState.UPCOMING ->
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                ),
                            )
                        GrowthMapModel.NodeState.LOCKED ->
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                ),
                            )
                    },
                )
                .alpha(
                    when (state) {
                        GrowthMapModel.NodeState.CURRENT -> sparkleAlpha
                        GrowthMapModel.NodeState.LOCKED -> 0.55f
                        else -> 1f
                    },
                )
                .pointerInput(state, number) {
                    detectTapGestures(
                        onTap = {
                            when (state) {
                                GrowthMapModel.NodeState.CURRENT -> onClick()
                                GrowthMapModel.NodeState.COMPLETED -> onTap()
                                else -> Unit
                            }
                        },
                        onLongPress = { if (state == GrowthMapModel.NodeState.COMPLETED) onLongPress() },
                    )
                }
                .testTag("home_tree_fruit_$number"),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                GrowthMapModel.NodeState.COMPLETED -> {
                    Text(
                        character,
                        color = PrimaryText,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    StarMark(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp), color = Coral)
                }
                GrowthMapModel.NodeState.CURRENT -> {
                    Text(
                        character,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                GrowthMapModel.NodeState.UPCOMING -> Unit
                GrowthMapModel.NodeState.LOCKED -> {
                    LockMark(color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StarMark(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier.size(14.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val path = Path()
        repeat(10) { index ->
            val angle = (-90 + index * 36) * Math.PI / 180.0
            val radius = if (index % 2 == 0) size.minDimension / 2f else size.minDimension / 5f
            val point = Offset(center.x + kotlin.math.cos(angle).toFloat() * radius, center.y + kotlin.math.sin(angle).toFloat() * radius)
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, color = color)
    }
}

@Composable
private fun LockMark(color: Color) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Canvas(modifier = Modifier.size(18.dp)) {
        drawRoundRect(color = color.copy(alpha = 0.78f), topLeft = Offset(size.width * 0.18f, size.height * 0.42f), size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.42f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
        drawArc(color = color.copy(alpha = 0.78f), startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(size.width * 0.3f, size.height * 0.12f), size = androidx.compose.ui.geometry.Size(size.width * 0.4f, size.height * 0.52f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        drawCircle(color = surfaceColor, radius = 1.5.dp.toPx(), center = Offset(size.width / 2f, size.height * 0.62f))
    }
}
