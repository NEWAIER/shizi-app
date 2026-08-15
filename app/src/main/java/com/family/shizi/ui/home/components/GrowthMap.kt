package com.family.shizi.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.domain.engine.GrowthMapModel

private val nodeXs = listOf(76, 150, 224, 166, 98, 210, 124, 238, 62, 184)
private const val ENTRY_HEIGHT = 104

/** 一棵连续的树：55 个 MapEntry 从底部向树冠排列。 */
@OptIn(ExperimentalFoundationApi::class)
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
    val entries = remember(characters) { GrowthMapModel.entries(characters.map { it.id }) }
    val currentIndex = GrowthMapModel.currentEntryIndex(learnedCount, completedStageBatches, characters.map { it.id })
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex, entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex.coerceIn(0, entries.lastIndex), scrollOffset = -260)
            onAutoFocusComplete(currentIndex)
        }
    }
    Box(modifier = modifier.fillMaxSize().testTag("home_growth_tree")) {
        ForestBackdrop(chapterIndex = ((learnedCount.coerceAtMost(characters.size)) / 10).coerceIn(0, 4), modifier = Modifier.fillMaxSize()) {}
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
        ) {
            itemsIndexed(entries, key = { index, entry -> "map_entry_${index}_${entry.hashCode()}" }) { index, entry ->
                val state = GrowthMapModel.entryState(entry, learnedCount, completedStageBatches, dailyTarget, characters.size)
                val x = nodeXs[index % nodeXs.size]
                Box(modifier = Modifier.fillMaxWidth().height(ENTRY_HEIGHT.dp).testTag("map_entry_$index")) {
                    GrowthTreeSegment(index = index, x = x, modifier = Modifier.fillMaxSize())
                    when (entry) {
                        is GrowthMapModel.MapEntry.CharacterNode -> {
                            val character = characters.getOrNull(entry.order - 1) ?: return@itemsIndexed
                            GameMapNode(
                                character = character.character,
                                number = entry.order,
                                state = state,
                                onClick = { onLearn(character.id) },
                                onTap = { onCompletedTap(character) },
                                onLongPress = { onCompletedLongPress(character) },
                                modifier = Modifier.offset(x = (x - 36).dp, y = 12.dp).testTag("map_node_${entry.order}"),
                            )
                        }
                        is GrowthMapModel.MapEntry.StageTestNode -> {
                            TreeHoleGate(
                                batchIndex = entry.batchIndex,
                                number = entry.afterCharacterOrder,
                                unlocked = state == GrowthMapModel.NodeState.CURRENT || state == GrowthMapModel.NodeState.COMPLETED,
                                completed = state == GrowthMapModel.NodeState.COMPLETED,
                                remaining = (entry.afterCharacterOrder - learnedCount).coerceAtLeast(0),
                                onClick = { if (state == GrowthMapModel.NodeState.CURRENT) onOpenStageTest(entry.batchIndex) },
                                modifier = Modifier.offset(x = (x - 50).dp, y = 12.dp),
                            )
                        }
                    }
                    if (state == GrowthMapModel.NodeState.CURRENT) {
                        CaterpillarMascot(
                            segmentCount = 4,
                            state = if (entry is GrowthMapModel.MapEntry.StageTestNode) CaterpillarState.CHALLENGE else CaterpillarState.LEARNING,
                            facingLeft = x < 150,
                            modifier = Modifier.offset(x = if (x < 150) (x + 58).dp else (x - 58).dp, y = 38.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthTreeSegment(index: Int, x: Int, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val progress = (index / 54f).coerceIn(0f, 1f)
        val trunkWidth = (80f - progress * 44f).dp.toPx()
        val center = size.width / 2f + kotlin.math.sin(index * .28f) * 10.dp.toPx()
        val bottom = androidx.compose.ui.geometry.Offset(center, size.height + 4.dp.toPx())
        val top = androidx.compose.ui.geometry.Offset(center + kotlin.math.sin((index + 1) * .28f) * 10.dp.toPx(), -4.dp.toPx())
        drawLine(Color(0xFF5B3426), bottom, top, trunkWidth + 9.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Color(0xFF9A5D38), bottom, top, trunkWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Color(0xFFC47A4A), androidx.compose.ui.geometry.Offset(center - trunkWidth * .16f, size.height), androidx.compose.ui.geometry.Offset(top.x - trunkWidth * .16f, 0f), (trunkWidth * .16f).coerceAtLeast(3.dp.toPx()), cap = androidx.compose.ui.graphics.StrokeCap.Round)

        val branchEnd = androidx.compose.ui.geometry.Offset(x.dp.toPx() + 36.dp.toPx(), size.height * .5f)
        val branchStart = androidx.compose.ui.geometry.Offset(center, size.height * .58f)
        drawLine(Color(0xFF673B29), branchStart, branchEnd, 13.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(Color(0xFFA7653D), branchStart, branchEnd, 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)

        val leafColors = listOf(Color(0xFF3D8F4B), Color(0xFF62B957), Color(0xFF9BD66B))
        listOf(
            androidx.compose.ui.geometry.Offset((x + 12).dp.toPx(), 22.dp.toPx()),
            androidx.compose.ui.geometry.Offset((x + 48).dp.toPx(), 74.dp.toPx()),
            androidx.compose.ui.geometry.Offset(center + 22.dp.toPx(), 18.dp.toPx()),
        ).forEachIndexed { leafIndex, leaf ->
            drawOval(
                color = leafColors[(index + leafIndex) % leafColors.size],
                topLeft = androidx.compose.ui.geometry.Offset(leaf.x - 12.dp.toPx(), leaf.y - 7.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(24.dp.toPx(), 14.dp.toPx()),
            )
        }
        if (index == 0) {
            drawLine(Color(0xFF673B29), androidx.compose.ui.geometry.Offset(center, size.height - 8.dp.toPx()), androidx.compose.ui.geometry.Offset(center - 62.dp.toPx(), size.height + 14.dp.toPx()), 18.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(Color(0xFF673B29), androidx.compose.ui.geometry.Offset(center, size.height - 8.dp.toPx()), androidx.compose.ui.geometry.Offset(center + 62.dp.toPx(), size.height + 14.dp.toPx()), 18.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        if (index > 42) {
            repeat(4) { leafIndex ->
                val leaf = androidx.compose.ui.geometry.Offset((26 + leafIndex * 74).dp.toPx(), (18 + (leafIndex % 2) * 36).dp.toPx())
                drawCircle(Color(0xFF76C866), 13.dp.toPx(), leaf)
            }
        }
    }
}
