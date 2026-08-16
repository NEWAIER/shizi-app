package com.family.shizi.ui.home.components.tree

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.family.shizi.R
import com.family.shizi.domain.engine.GrowthMapModel

/** 直接使用参考图风格的独立果实资源；同一果实可以重复挂载到无限树干节点。 */
@Composable
fun CharacterFruit(state: GrowthMapModel.NodeState, seed: Int = 0, modifier: Modifier = Modifier) {
    val pulse = if (state == GrowthMapModel.NodeState.CURRENT) {
        rememberInfiniteTransition(label = "fruit_pulse").animateFloat(
            initialValue = .84f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
            label = "fruit_pulse_alpha",
        ).value
    } else 1f
    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.learning_fruit),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .alpha(if (state == GrowthMapModel.NodeState.LOCKED) .35f else pulse)
                .graphicsLayer { rotationZ = (seed % 5 - 2) * 1.5f },
        )
        if (state == GrowthMapModel.NodeState.CURRENT) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                drawCircle(Color(0xFFFFF0A0).copy(alpha = .25f * pulse), radius = size.minDimension * .66f, center = center)
            }
        }
    }
}
