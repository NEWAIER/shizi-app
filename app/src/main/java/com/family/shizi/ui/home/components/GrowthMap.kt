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
    onCompletedTap: (CharacterContent) -> Unit = {},
    onCompletedLongPress: (CharacterContent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val entries = remember(characters) { GrowthMapModel.entries(characters.map { it.id }) }
    val currentIndex = GrowthMapModel.currentEntryIndex(learnedCount, completedStageBatches, characters.map { it.id })
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex, entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(currentIndex.coerceIn(0, entries.lastIndex), scrollOffset = -260)
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
                    TreeSegment(index = index, x = x, modifier = Modifier.fillMaxSize())
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
                            modifier = Modifier.offset(x = if (x < 150) (x + 58).dp else (x - 58).dp, y = 38.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeSegment(index: Int, x: Int, modifier: Modifier) {
    GamePath(
        points = listOf(
            androidx.compose.ui.unit.DpOffset(150.dp, 0.dp),
            androidx.compose.ui.unit.DpOffset(x.dp, 52.dp),
            androidx.compose.ui.unit.DpOffset(150.dp, 104.dp),
        ),
        modifier = modifier,
        color = chapterBannerColor((index / 10).coerceIn(0, 4)),
    )
}
