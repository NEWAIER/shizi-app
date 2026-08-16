package com.family.shizi.ui.home.components.tree

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.family.shizi.domain.engine.GrowthMapModel

@Composable
fun CharacterFruit(
    state: GrowthMapModel.NodeState,
    seed: Int = 0,
    modifier: Modifier = Modifier,
) {
    val pulse = if (state == GrowthMapModel.NodeState.CURRENT) {
        rememberInfiniteTransition(label = "fruit_pulse").animateFloat(
            initialValue = .82f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
            label = "fruit_pulse_alpha",
        ).value
    } else 1f
    Canvas(modifier) {
        val matureColors = listOf(
            Color(0xFFE85D45), Color(0xFFF29F38), Color(0xFFEBC43F),
            Color(0xFF8F75D6), Color(0xFF48B8C8), Color(0xFF7CCB55),
        )
        val base = when (state) {
            GrowthMapModel.NodeState.COMPLETED -> matureColors[seed.mod(matureColors.size)]
            GrowthMapModel.NodeState.CURRENT -> Color(0xFF49B975)
            GrowthMapModel.NodeState.UPCOMING -> Color(0xFF76C98A)
            GrowthMapModel.NodeState.LOCKED -> Color(0xFF84A9A1)
        }
        if (state == GrowthMapModel.NodeState.CURRENT) {
            drawCircle(Color(0xFFFFF0A0).copy(alpha = .26f * pulse), radius = size.minDimension * .66f, center = center)
        }
        drawFruit(base.copy(alpha = if (state == GrowthMapModel.NodeState.LOCKED) .62f else 1f))
    }
}

private fun DrawScope.drawFruit(color: Color) {
    val path = Path().apply {
        moveTo(size.width * .5f, size.height * .17f)
        cubicTo(size.width * .38f, size.height * .12f, size.width * .2f, size.height * .2f, size.width * .18f, size.height * .45f)
        cubicTo(size.width * .16f, size.height * .72f, size.width * .33f, size.height * .9f, size.width * .51f, size.height * .91f)
        cubicTo(size.width * .72f, size.height * .9f, size.width * .87f, size.height * .72f, size.width * .82f, size.height * .44f)
        cubicTo(size.width * .78f, size.height * .2f, size.width * .62f, size.height * .12f, size.width * .5f, size.height * .17f)
        close()
    }
    drawPath(path, Color(0xFF6B3D2A).copy(alpha = .58f))
    val inset = Path().apply {
        moveTo(size.width * .5f, size.height * .2f)
        cubicTo(size.width * .39f, size.height * .16f, size.width * .24f, size.height * .24f, size.width * .22f, size.height * .46f)
        cubicTo(size.width * .2f, size.height * .68f, size.width * .35f, size.height * .85f, size.width * .51f, size.height * .86f)
        cubicTo(size.width * .69f, size.height * .85f, size.width * .82f, size.height * .68f, size.width * .78f, size.height * .45f)
        cubicTo(size.width * .75f, size.height * .25f, size.width * .61f, size.height * .16f, size.width * .5f, size.height * .2f)
        close()
    }
    drawPath(
        inset,
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = .28f),
                color,
                shade(color, .68f),
            ),
            center = Offset(size.width * .36f, size.height * .31f),
            radius = size.minDimension * .78f,
        ),
    )
    // A short woody stem and two small sepals echo the reference tree fruit.
    drawLine(
        color = Color(0xFF6B4029),
        start = Offset(size.width * .5f, size.height * .2f),
        end = Offset(size.width * .52f, size.height * .04f),
        strokeWidth = 3.4.dp.toPx(),
    )
    drawLeaf(Offset(size.width * .51f, size.height * .16f), size.minDimension * .16f, Color(0xFF4D9B45))
    drawLeaf(Offset(size.width * .47f, size.height * .14f), size.minDimension * .12f, Color(0xFF8EC85A))
    drawOval(
        color = Color.White.copy(alpha = .58f),
        topLeft = Offset(size.width * .29f, size.height * .28f),
        size = androidx.compose.ui.geometry.Size(size.width * .13f, size.height * .22f),
    )
    drawOval(
        color = Color.White.copy(alpha = .2f),
        topLeft = Offset(size.width * .42f, size.height * .22f),
        size = androidx.compose.ui.geometry.Size(size.width * .07f, size.height * .1f),
    )
}

private fun shade(color: Color, factor: Float): Color = Color(
    red = (color.red * factor).coerceIn(0f, 1f),
    green = (color.green * factor).coerceIn(0f, 1f),
    blue = (color.blue * factor).coerceIn(0f, 1f),
    alpha = color.alpha,
)

private fun DrawScope.drawLeaf(center: Offset, length: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y)
        cubicTo(center.x - length * .65f, center.y - length * .45f, center.x - length * .55f, center.y - length, center.x - length, center.y - length * 1.05f)
        cubicTo(center.x - length * .38f, center.y - length * 1.05f, center.x + length * .25f, center.y - length * .5f, center.x, center.y)
        close()
    }
    drawPath(path, color)
}
