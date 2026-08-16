package com.family.shizi.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.domain.engine.GrowthMapModel
import com.family.shizi.domain.engine.SingleGrowthTreeGeometry
import com.family.shizi.ui.home.components.assets.CaterpillarArtwork

/**
 * 一棵连续参天大树：单一主干从底部蜿蜒向上，树高随「已学 + 今日目标」生长，
 * 每个汉字是一个挂在枝头的果子。不采用多棵树段拼接，整棵树是一根连续主干。
 *
 * 性能约束：无限动画只出现在当前节点、毛毛虫与已解锁树洞（由子组件保证），
 * 主干与树枝为静态绘制。
 */
@Composable
fun GrowthMap(
    characters: List<CharacterContent>,
    learnedCount: Int,
    dailyTarget: Int,
    completedStageBatches: Set<Int> = emptySet(),
    onLearn: (String) -> Unit,
    onOpenStageTest: (Int) -> Unit,
    onAutoFocusComplete: (Int) -> Unit = {},
    onCompletedTap: (CharacterContent) -> Unit = {},
    onCompletedLongPress: (CharacterContent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ids = remember(characters) { characters.map { it.id } }
    val mapEntries = remember(ids) { GrowthMapModel.entries(ids) }
    val visible = remember(learnedCount, dailyTarget, characters.size) {
        SingleGrowthTreeGeometry.visibleCount(learnedCount, dailyTarget, characters.size)
    }
    val treeHeight = remember(visible) { SingleGrowthTreeGeometry.treeHeight(visible).dp }
    val currentMapIndex = GrowthMapModel.currentEntryIndex(learnedCount, completedStageBatches, ids)
    val scrollState = rememberScrollState()

    // 自动定位：让当前节点出现在视口中部。
    // mapEntries 从底部排列，条目 index 对应树上的纵向位置：
    // 节点 y = nodeY(visible - index, visible)。
    val currentY = if (visible > 0 && currentMapIndex in mapEntries.indices) {
        SingleGrowthTreeGeometry.nodeY(visible - currentMapIndex, visible)
    } else 0
    LaunchedEffect(currentY, treeHeight) {
        scrollState.scrollTo(currentY.coerceAtLeast(0))
        onAutoFocusComplete(currentMapIndex)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("home_growth_tree")) {
        // geometry 全部使用 dp 坐标，centerX 为 dp 值。
        val centerX = (maxWidth.value / 2f).toInt()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(treeHeight)
                .verticalScroll(scrollState),
        ) {
            ForestBackdrop(
                chapterIndex = ((learnedCount.coerceAtMost(characters.size)) / 10).coerceIn(0, 4),
                modifier = Modifier.fillMaxWidth().height(treeHeight),
            ) {}
            // 连续主干与树枝：纯静态绘制。
            Canvas(modifier = Modifier.fillMaxWidth().height(treeHeight)) {
                val topY = SingleGrowthTreeGeometry.TOP_MARGIN
                val bottomY = topY + (visible - 1) * SingleGrowthTreeGeometry.ROW_HEIGHT
                val trunkPath = Path().apply {
                    // 从树根（bottom）向树冠（top）蜿蜒。
                    moveTo(SingleGrowthTreeGeometry.trunkXAt(bottomY, visible, centerX), bottomY.toFloat())
                    var y = bottomY - SingleGrowthTreeGeometry.ROW_HEIGHT
                    while (y >= topY) {
                        lineTo(SingleGrowthTreeGeometry.trunkXAt(y, visible, centerX), y.toFloat())
                        y -= SingleGrowthTreeGeometry.ROW_HEIGHT / 2
                    }
                    lineTo(SingleGrowthTreeGeometry.trunkXAt(topY, visible, centerX), topY.toFloat())
                }
                // 粗主干 + 内芯亮色，形成立体感。
                drawPath(
                    trunkPath,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF8A5A2B), Color(0xFF6B4226)),
                        startY = bottomY.toFloat(),
                        endY = topY.toFloat(),
                    ),
                    style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round),
                )
                drawPath(
                    trunkPath,
                    color = Color(0xFFC89B6A).copy(alpha = .55f),
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                )
                // 树枝：从主干分叉到每个果子锚点。
                (1..visible).forEach { order ->
                    val nodeX = SingleGrowthTreeGeometry.nodeX(order, centerX)
                    val nodeY = SingleGrowthTreeGeometry.nodeY(order, visible)
                    val trunkXAt = SingleGrowthTreeGeometry.trunkXAt(nodeY, visible, centerX)
                    val branch = Path().apply {
                        moveTo(trunkXAt, nodeY.toFloat())
                        lineTo(nodeX.toFloat(), (nodeY - 6).toFloat())
                    }
                    drawPath(branch, color = Color(0xFF8A5A2B), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            // 节点：挂在树枝末端。
            (1..visible).forEach { order ->
                val entry = mapEntries.getOrNull(order - 1) ?: return@forEach
                val state = GrowthMapModel.entryState(
                    entry = entry,
                    learnedCount = learnedCount,
                    completedStageBatches = completedStageBatches,
                    dailyTarget = dailyTarget,
                    characterIds = ids,
                )
                val x = SingleGrowthTreeGeometry.nodeX(order, centerX)
                val y = SingleGrowthTreeGeometry.nodeY(order, visible)
                when (entry) {
                    is GrowthMapModel.MapEntry.CharacterNode -> {
                        val character = characters.getOrNull(entry.order - 1) ?: return@forEach
                        GameMapNode(
                            character = character.character,
                            number = entry.order,
                            state = state,
                            onClick = { onLearn(character.id) },
                            onTap = { onCompletedTap(character) },
                            onLongPress = { onCompletedLongPress(character) },
                            modifier = Modifier.offset(x = (x - 34).dp, y = (y - 30).dp).testTag("map_node_${entry.order}"),
                        )
                    }
                    is GrowthMapModel.MapEntry.StageTestNode -> {
                        val holeX = SingleGrowthTreeGeometry.stageHoleX(entry.batchIndex, centerX)
                        val holeY = SingleGrowthTreeGeometry.stageHoleY(entry.batchIndex, visible)
                        TreeHoleGate(
                            batchIndex = entry.batchIndex,
                            number = entry.afterCharacterOrder,
                            unlocked = state == GrowthMapModel.NodeState.CURRENT || state == GrowthMapModel.NodeState.COMPLETED,
                            completed = state == GrowthMapModel.NodeState.COMPLETED,
                            remaining = (entry.afterCharacterOrder - learnedCount).coerceAtLeast(0),
                            onClick = { if (state == GrowthMapModel.NodeState.CURRENT) onOpenStageTest(entry.batchIndex) },
                            modifier = Modifier.offset(x = (holeX - 59).dp, y = (holeY - 38).dp),
                        )
                    }
                }
                // 毛毛虫：停在当前条目旁。
                if (state == GrowthMapModel.NodeState.CURRENT) {
                    CaterpillarArtwork(
                        state = if (entry is GrowthMapModel.MapEntry.StageTestNode) CaterpillarState.CHALLENGE else CaterpillarState.LEARNING,
                        facingLeft = x < centerX,
                        modifier = Modifier.offset(
                            x = (if (x < centerX) x + 52 else x - 148).dp,
                            y = (y - 40).dp,
                        ).width(96.dp).height(94.dp),
                    )
                }
            }
            // 树根：底部圆台。
            Box(
                modifier = Modifier
                    .offset(x = (centerX - 90).dp, y = (SingleGrowthTreeGeometry.treeHeight(visible) - 84).dp)
                    .width(180.dp).height(64.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF8A5A2B), Color(0xFF5C3A1E))))
                    .testTag("tree_root"),
            ) {}
        }
    }
}
