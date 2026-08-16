package com.family.shizi.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.family.shizi.R
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.domain.engine.GrowthMapModel
import com.family.shizi.domain.engine.InfiniteGrowthTreeModel
import com.family.shizi.ui.home.components.assets.CaterpillarArtwork
import com.family.shizi.ui.home.tree.GrowthTrunkAnchors

/** 无树冠的无限成长树干：固定比例树干节纵向延伸，果实直接挂在每节树枝锚点。 */
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
    val entries = remember(ids) { GrowthMapModel.entries(ids) }
    val segments = remember(entries.size) { InfiniteGrowthTreeModel.segments(entries.size) }
    val currentIndex = GrowthMapModel.currentEntryIndex(learnedCount, completedStageBatches, ids)
    val currentSegment = (currentIndex / InfiniteGrowthTreeModel.ENTRIES_PER_SEGMENT).coerceIn(0, segments.lastIndex.coerceAtLeast(0))
    val listState = rememberLazyListState()

    LaunchedEffect(currentSegment, entries.size) {
        if (segments.isNotEmpty()) {
            listState.scrollToItem(currentSegment)
            onAutoFocusComplete(currentIndex)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("home_growth_tree")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            reverseLayout = true,
        ) {
            items(segments, key = { it.segmentIndex }) { segment ->
                val segmentEntries = entries.subList(segment.firstEntryIndex, segment.lastEntryIndex + 1)
                Box(
                    modifier = Modifier.fillMaxWidth().testTag("home_tree_trunk_segment_${segment.segmentIndex}"),
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val segmentWidth = maxWidth
                        val segmentHeight = segmentWidth * (1536f / 1024f)
                        Box(modifier = Modifier.fillMaxWidth().size(height = segmentHeight, width = segmentWidth)) {
                            Image(
                                painter = painterResource(R.drawable.growth_trunk_segment),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                            segmentEntries.forEachIndexed { localIndex, entry ->
                                val anchor = when (entry) {
                                    is GrowthMapModel.MapEntry.CharacterNode -> GrowthTrunkAnchors.fruitAnchor(localIndex)
                                    is GrowthMapModel.MapEntry.StageTestNode -> GrowthTrunkAnchors.stageHoleAnchor()
                                }
                                val x = segmentWidth * anchor.x
                                val y = segmentHeight * anchor.y
                                val state = GrowthMapModel.entryState(entry, learnedCount, completedStageBatches, dailyTarget, ids)
                                when (entry) {
                                    is GrowthMapModel.MapEntry.CharacterNode -> {
                                        val character = characters.getOrNull(entry.order - 1) ?: return@forEachIndexed
                                        GameMapNode(
                                            character = character.character,
                                            number = entry.order,
                                            state = state,
                                            onClick = { if (state == GrowthMapModel.NodeState.CURRENT) onLearn(character.id) },
                                            onTap = { if (state == GrowthMapModel.NodeState.COMPLETED) onCompletedTap(character) },
                                            onLongPress = { if (state == GrowthMapModel.NodeState.COMPLETED) onCompletedLongPress(character) },
                                            modifier = Modifier.offset(x = x - 36.dp, y = y - 36.dp),
                                        )
                                        if (entry.globalIndex(segment) == currentIndex) {
                                            CaterpillarArtwork(
                                                state = CaterpillarState.LEARNING,
                                                facingLeft = anchor.x > 0.5f,
                                                modifier = Modifier.offset(x = if (anchor.x > 0.5f) x - 115.dp else x + 38.dp, y = y - 18.dp).size(96.dp),
                                            )
                                        }
                                    }
                                    is GrowthMapModel.MapEntry.StageTestNode -> {
                                        TreeHoleGate(
                                            batchIndex = entry.batchIndex,
                                            number = entry.afterCharacterOrder,
                                            unlocked = state == GrowthMapModel.NodeState.CURRENT || state == GrowthMapModel.NodeState.COMPLETED,
                                            completed = state == GrowthMapModel.NodeState.COMPLETED,
                                            remaining = (entry.afterCharacterOrder - learnedCount).coerceAtLeast(0),
                                            onClick = { if (state == GrowthMapModel.NodeState.CURRENT) onOpenStageTest(entry.batchIndex) },
                                            modifier = Modifier.offset(x = x - 52.dp, y = y - 35.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun GrowthMapModel.MapEntry.globalIndex(segment: InfiniteGrowthTreeModel.TreeSegment): Int =
    when (this) {
        is GrowthMapModel.MapEntry.CharacterNode -> segment.firstEntryIndex + (order - 1) % InfiniteGrowthTreeModel.ENTRIES_PER_SEGMENT
        is GrowthMapModel.MapEntry.StageTestNode -> segment.lastEntryIndex
    }
