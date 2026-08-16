package com.family.shizi.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.domain.engine.GrowthMapModel
import com.family.shizi.domain.engine.InfiniteGrowthTreeModel
import com.family.shizi.ui.home.tree.SegmentCharacterEntry
import com.family.shizi.ui.home.tree.SegmentStageEntry
import com.family.shizi.ui.home.tree.TreeSegmentCatalog
import com.family.shizi.ui.home.tree.TreeSegmentView

private val SEGMENT_OVERLAP = (-72).dp

/** 每个树段是一个 LazyColumn item，节点只使用正式树段的归一化锚点。 */
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
    val segments = remember(mapEntries.size) { InfiniteGrowthTreeModel.segments(mapEntries.size) }
    val currentMapIndex = GrowthMapModel.currentEntryIndex(learnedCount, completedStageBatches, ids)
    val currentSegmentIndex = (currentMapIndex / InfiniteGrowthTreeModel.ENTRIES_PER_SEGMENT)
        .coerceIn(0, segments.lastIndex.coerceAtLeast(0))
    val listState = rememberLazyListState()

    LaunchedEffect(currentSegmentIndex, mapEntries.size) {
        if (segments.isNotEmpty()) {
            listState.scrollToItem(currentSegmentIndex.coerceIn(0, segments.lastIndex))
            onAutoFocusComplete(currentMapIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("home_growth_tree")) {
        ForestBackdrop(
            chapterIndex = ((learnedCount.coerceAtMost(characters.size)) / 10).coerceIn(0, 4),
            modifier = Modifier.fillMaxSize(),
        ) {}
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(SEGMENT_OVERLAP),
        ) {
            items(items = segments, key = { it.segmentIndex }) { segment ->
                val spec = TreeSegmentCatalog.specFor(segment.segmentIndex)
                val segmentEntries = buildSegmentEntries(
                    segment = segment,
                    mapEntries = mapEntries,
                    characters = characters,
                    learnedCount = learnedCount,
                    dailyTarget = dailyTarget,
                    completedStageBatches = completedStageBatches,
                )
                TreeSegmentView(
                    segmentIndex = segment.segmentIndex,
                    spec = spec,
                    characterEntries = segmentEntries.characters,
                    stageEntry = segmentEntries.stage,
                    currentGlobalMapIndex = currentMapIndex,
                    onLearn = onLearn,
                    onStageTest = onOpenStageTest,
                    onCompletedTap = onCompletedTap,
                    onCompletedLongPress = onCompletedLongPress,
                    modifier = Modifier.fillMaxWidth().testTag("home_tree_segment_${segment.segmentIndex}"),
                )
            }
        }
    }
}

private data class SegmentEntries(
    val characters: List<SegmentCharacterEntry>,
    val stage: SegmentStageEntry?,
)

private fun buildSegmentEntries(
    segment: InfiniteGrowthTreeModel.TreeSegment,
    mapEntries: List<GrowthMapModel.MapEntry>,
    characters: List<CharacterContent>,
    learnedCount: Int,
    dailyTarget: Int,
    completedStageBatches: Set<Int>,
): SegmentEntries {
    val characterById = characters.associateBy { it.id }
    val characterEntries = mutableListOf<SegmentCharacterEntry>()
    var stage: SegmentStageEntry? = null
    mapEntries.subList(segment.firstEntryIndex, segment.lastEntryIndex + 1).forEachIndexed { localIndex, entry ->
        val globalIndex = segment.firstEntryIndex + localIndex
        val state = GrowthMapModel.entryState(
            entry = entry,
            learnedCount = learnedCount,
            completedStageBatches = completedStageBatches,
            dailyTarget = dailyTarget,
            characterIds = characters.map { it.id },
        )
        when (entry) {
            is GrowthMapModel.MapEntry.CharacterNode -> characterById[entry.characterId]?.let { character ->
                characterEntries += SegmentCharacterEntry(
                    globalMapIndex = globalIndex,
                    localCharacterIndex = localIndex,
                    characterOrder = entry.order,
                    character = character,
                    state = state,
                )
            }
            is GrowthMapModel.MapEntry.StageTestNode -> stage = SegmentStageEntry(
                globalMapIndex = globalIndex,
                batchIndex = entry.batchIndex,
                afterCharacterOrder = entry.afterCharacterOrder,
                state = state,
            )
        }
    }
    return SegmentEntries(characterEntries, stage)
}
