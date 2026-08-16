package com.family.shizi.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Lightweight scene layer: five forests share one visual language but have distinct decorations. */
@Composable
fun ForestBackdrop(chapterIndex: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val palette = when (chapterIndex % 5) {
        0 -> listOf(Color(0xFFE9F8D9), Color(0xFFC7EBA6))
        1 -> listOf(Color(0xFFFFF0D7), Color(0xFFFFD0DA))
        2 -> listOf(Color(0xFFDDF5F4), Color(0xFFA9DFF0))
        3 -> listOf(Color(0xFFE9E4FF), Color(0xFFC9D8FF))
        else -> listOf(Color(0xFF334278), Color(0xFF1E2858))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(palette)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (chapterIndex % 5) {
                0 -> {
                    drawCircle(Color(0xFF8BCB70).copy(alpha = 0.55f), 24.dp.toPx(), Offset(28.dp.toPx(), 40.dp.toPx()))
                    drawCircle(Color(0xFFFFF2A6).copy(alpha = 0.75f), 5.dp.toPx(), Offset(w - 38.dp.toPx(), 80.dp.toPx()))
                }
                1 -> {
                    repeat(5) { i -> drawCircle(Color(0xFFFF9EB5).copy(alpha = 0.35f), (8 + i % 3 * 3).dp.toPx(), Offset(20.dp.toPx() + i * 56.dp.toPx(), 48.dp.toPx() + (i % 2) * 90.dp.toPx())) }
                }
                2 -> {
                    drawOval(Color(0xFF73C9E5).copy(alpha = 0.38f), topLeft = Offset(w * 0.12f, h * 0.08f), size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.82f))
                    drawCircle(Color.White.copy(alpha = 0.8f), 4.dp.toPx(), Offset(w - 36.dp.toPx(), 46.dp.toPx()))
                }
                3 -> {
                    drawCircle(Color.White.copy(alpha = 0.6f), 22.dp.toPx(), Offset(34.dp.toPx(), 54.dp.toPx()))
                    drawCircle(Color.White.copy(alpha = 0.5f), 17.dp.toPx(), Offset(66.dp.toPx(), 46.dp.toPx()))
                    drawCircle(Color(0xFFFFD76B).copy(alpha = 0.65f), 7.dp.toPx(), Offset(w - 40.dp.toPx(), 64.dp.toPx()))
                }
                else -> {
                    repeat(9) { i -> drawCircle(Color(0xFFFFF1A6).copy(alpha = 0.7f), (2 + i % 2).dp.toPx(), Offset(20.dp.toPx() + (i * 47 % 250).dp.toPx(), 30.dp.toPx() + (i * 61 % 180).dp.toPx())) }
                    drawCircle(Color(0xFFFFF3B0).copy(alpha = 0.8f), 18.dp.toPx(), Offset(w - 40.dp.toPx(), 56.dp.toPx()))
                }
            }
        }
        content()
    }
}
