package com.family.shizi.ui.home.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    remaining: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glow = rememberInfiniteTransition(label = "tree_hole_glow_$batchIndex").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "tree_hole_glow_alpha_$batchIndex",
    ).value
    Card(
        modifier = modifier
            .size(width = 118.dp, height = 58.dp)
            .clickable(enabled = unlocked) { onClick() }
            .testTag("home_tree_hole_$number"),
        shape = RoundedCornerShape(topStart = 99.dp, topEnd = 99.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("树洞 · 第${batchIndex + 1}关", style = MaterialTheme.typography.labelLarge)
            Text(
                if (unlocked) "挑战已开启" else "再学 $remaining 个字",
                modifier = Modifier.alpha(if (unlocked) glow else 1f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
