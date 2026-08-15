package com.family.shizi.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.family.shizi.domain.engine.GrowthMapModel
import com.family.shizi.ui.theme.PrimaryTeal

/**
 * 章节横幅：显示章节名与字数范围，附带每章不同的装饰
 * （第一森林石头、第二森林云朵、第三森林叶子、第四森林星星、第五星光森林星光）。
 * 静态绘制，无动画。
 */
@Composable
fun ChapterBanner(
    chapter: GrowthMapModel.Chapter,
    current: Boolean,
    modifier: Modifier = Modifier,
    bannerColor: Color = PrimaryTeal,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (current) bannerColor.copy(alpha = 0.28f) else bannerColor.copy(alpha = 0.14f))
            .testTag("chapter_banner_${chapter.index}"),
    ) {
        ChapterDeco(kind = chapter.index, modifier = Modifier.matchParentSize())
        Row(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                chapter.name,
                style = MaterialTheme.typography.titleLarge,
                color = if (current) bannerColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${chapter.startNumber.toString().padStart(2, '0')} - ${chapter.endNumber.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
            if (current) {
                Spacer(Modifier.width(8.dp))
                Text("🌱 当前章节", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ChapterDeco(kind: Int, modifier: Modifier = Modifier) {
    // 静态装饰，不运行动画。
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (kind % 5) {
            0 -> { // 第一森林：小石头
                drawCircle(Color(0xFFC9A37C), radius = 7.dp.toPx(), center = Offset(w - 40.dp.toPx(), h - 14.dp.toPx()))
                drawCircle(Color(0xFFB08D68), radius = 5.dp.toPx(), center = Offset(w - 22.dp.toPx(), h - 20.dp.toPx()))
            }
            1 -> { // 第二森林：云朵
                drawCircle(Color.White.copy(alpha = 0.8f), radius = 9.dp.toPx(), center = Offset(w - 52.dp.toPx(), 16.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.7f), radius = 7.dp.toPx(), center = Offset(w - 40.dp.toPx(), 12.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.8f), radius = 8.dp.toPx(), center = Offset(w - 28.dp.toPx(), 18.dp.toPx()))
            }
            2 -> { // 第三森林：叶子
                val leaf = Path().apply {
                    moveTo(w - 58.dp.toPx(), 30.dp.toPx())
                    quadraticTo(w - 42.dp.toPx(), 10.dp.toPx(), w - 24.dp.toPx(), 30.dp.toPx())
                    quadraticTo(w - 42.dp.toPx(), 46.dp.toPx(), w - 58.dp.toPx(), 30.dp.toPx())
                }
                drawPath(leaf, color = Color(0xFF7CB342).copy(alpha = 0.8f))
            }
            3 -> { // 第四森林：星星
                drawCircle(Color(0xFFFFD75E), radius = 6.dp.toPx(), center = Offset(w - 48.dp.toPx(), 16.dp.toPx()))
                drawCircle(Color(0xFFFFD75E).copy(alpha = 0.7f), radius = 4.dp.toPx(), center = Offset(w - 30.dp.toPx(), 26.dp.toPx()))
                drawCircle(Color(0xFFFFD75E).copy(alpha = 0.85f), radius = 5.dp.toPx(), center = Offset(w - 18.dp.toPx(), 12.dp.toPx()))
            }
            else -> { // 第五星光森林：星光
                repeat(5) { i ->
                    drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.9f), radius = 3.dp.toPx(), center = Offset(w - 20.dp.toPx() - i * 9.dp.toPx(), 14.dp.toPx() + (i % 2) * 12.dp.toPx()))
                }
            }
        }
    }
}

/** 每章推荐的横幅主色。 */
fun chapterBannerColor(chapterIndex: Int): Color = when (chapterIndex % 5) {
    0 -> PrimaryTeal
    1 -> Color(0xFF64A9FF)
    2 -> Color(0xFF7CB342)
    3 -> Color(0xFF8C7CF6)
    else -> Color(0xFFFFB74D)
}
