package com.family.shizi.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.domain.engine.GrowthMapModel
import com.family.shizi.ui.home.components.assets.GrowthTreeArtwork

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
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxSize().testTag("home_growth_tree")) {
        ForestBackdrop(chapterIndex = ((learnedCount.coerceAtMost(characters.size)) / 10).coerceIn(0, 4), modifier = Modifier.fillMaxSize()) {}
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewportHeight = maxHeight.value
            val contentHeight = (entries.size * ENTRY_HEIGHT).dp
            LaunchedEffect(currentIndex, entries.size, viewportHeight) {
                if (entries.isNotEmpty()) {
                    val rowTop = (entries.size - currentIndex - 1) * ENTRY_HEIGHT
                    val target = (rowTop + ENTRY_HEIGHT / 2f - viewportHeight * .47f).coerceAtLeast(0f)
                    scrollState.animateScrollTo(target.toInt())
                    onAutoFocusComplete(currentIndex)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight)
                    .verticalScroll(scrollState),
            ) {
                GrowthTreeArtwork(Modifier.fillMaxSize())
                entries.forEachIndexed { index, entry ->
                    GrowthMapEntry(
                        index = index,
                        entry = entry,
                        entryCount = entries.size,
                        characters = characters,
                        learnedCount = learnedCount,
                        dailyTarget = dailyTarget,
                        completedStageBatches = completedStageBatches,
                        onLearn = onLearn,
                        onOpenStageTest = onOpenStageTest,
                        onCompletedTap = onCompletedTap,
                        onCompletedLongPress = onCompletedLongPress,
                    )
                }
            }
        }
    }
}

@Composable
private fun GrowthMapEntry(
    index: Int,
    entry: GrowthMapModel.MapEntry,
    entryCount: Int,
    characters: List<CharacterContent>,
    learnedCount: Int,
    dailyTarget: Int,
    completedStageBatches: Set<Int>,
    onLearn: (String) -> Unit,
    onOpenStageTest: (Int) -> Unit,
    onCompletedTap: (CharacterContent) -> Unit,
    onCompletedLongPress: (CharacterContent) -> Unit,
) {
    val state = GrowthMapModel.entryState(entry, learnedCount, completedStageBatches, dailyTarget, characters.map { it.id })
    val x = nodeXs[index % nodeXs.size]
    val rowTop = (entryCount - index - 1) * ENTRY_HEIGHT
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ENTRY_HEIGHT.dp)
            .offset(y = rowTop.dp)
            .testTag("map_entry_$index"),
    ) {
        when (entry) {
            is GrowthMapModel.MapEntry.CharacterNode -> {
                val character = characters.getOrNull(entry.order - 1) ?: return
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
