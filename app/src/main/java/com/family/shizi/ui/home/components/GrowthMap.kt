package com.family.shizi.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.domain.engine.GrowthMapModel
import com.family.shizi.domain.engine.SingleGrowthTreeGeometry
import com.family.shizi.ui.home.components.assets.CaterpillarArtwork
import com.family.shizi.ui.home.components.assets.GrowthTreeArtwork

/**
 * 一棵连续参天大树：用卡通树图片（growth_tree_main.webp）作为视觉主体，
 * 图片切片随可见节点数（已学 + 今日目标）向上延展，保持图片的卡通质感。
 * 每个汉字是一个挂在枝头的果子，树洞每 10 字一个。
 *
 * 性能约束：无限动画只出现在当前节点、毛毛虫与已解锁树洞（由子组件保证）。
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
    val currentY = if (visible > 0 && currentMapIndex in mapEntries.indices) {
        SingleGrowthTreeGeometry.nodeY(visible - currentMapIndex, visible)
    } else 0
    LaunchedEffect(currentY, treeHeight) {
        scrollState.scrollTo(currentY.coerceAtLeast(0))
        onAutoFocusComplete(currentMapIndex)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("home_growth_tree")) {
        // 图片树宽度取整屏，树高由可见节点数决定；节点 x 用归一化锚点（相对宽度）。
        val treeWidthDp = maxWidth
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
            // 图片树干：卡通质感，切片随可见节点数向上延展。
            GrowthTreeArtwork(
                visibleCount = visible,
                modifier = Modifier.fillMaxWidth().height(treeHeight),
            )
            // 节点：果子挂在图片树的枝干锚点上。
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
            // 树根：图片底部已含树根，无需额外几何块。
        }
    }
}
