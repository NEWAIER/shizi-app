package com.family.shizi.ui.home.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val sparkleAlpha = rememberInfiniteTransition(label = "map_node_current_$number").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse),
        label = "map_node_current_alpha_$number",
    ).value

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            "$number",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        )
        Box(
            modifier = Modifier
                .size(size)
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
                .clickable(enabled = state == GrowthMapModel.NodeState.CURRENT || state == GrowthMapModel.NodeState.UPCOMING) {
                    onClick()
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
                    Text(
                        "★",
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Coral,
                    )
                }
                GrowthMapModel.NodeState.CURRENT, GrowthMapModel.NodeState.UPCOMING -> {
                    Text(
                        character,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                GrowthMapModel.NodeState.LOCKED -> {
                    Text(
                        "?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
        when (state) {
            GrowthMapModel.NodeState.COMPLETED ->
                Text("已吃掉", style = MaterialTheme.typography.labelSmall, color = Coral)
            GrowthMapModel.NodeState.CURRENT ->
                Text("快来吃我", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            GrowthMapModel.NodeState.UPCOMING ->
                Text("待成熟", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            GrowthMapModel.NodeState.LOCKED ->
                Text("?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}
