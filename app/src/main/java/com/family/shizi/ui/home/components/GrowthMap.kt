package com.family.shizi.ui.home.components

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
import com.family.shizi.ui.home.components.assets.GrowthTreeArtwork
import com.family.shizi.ui.home.components.assets.CaterpillarArtwork
import com.family.shizi.ui.home.tree.FullTreeAnchorCatalog
import com.family.shizi.ui.home.tree.NormalizedPoint

private val FULL_TREE_HEIGHT = 900.dp

/** 首页是一张完整连续的树图，所有节点都挂在同一张树上。 */
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
    val currentIndex = GrowthMapModel.currentEntryIndex(learnedCount, completedStageBatches, ids)
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("home_growth_tree")) {
        val viewportHeight = maxHeight
        val mapWidth = maxWidth
        val contentHeight = maxOf(FULL_TREE_HEIGHT, viewportHeight * 1.65f)
        val current = anchorForEntry(currentIndex, entries)
        LaunchedEffect(currentIndex, contentHeight) {
            val target = (contentHeight * current.y - viewportHeight * 0.45f).coerceAtLeast(0.dp)
            scrollState.animateScrollTo(target.value.toInt())
            onAutoFocusComplete(currentIndex)
        }

        Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Box(modifier = Modifier.fillMaxWidth().height(contentHeight)) {
                GrowthTreeArtwork(modifier = Modifier.fillMaxSize())
                entries.forEachIndexed { index, entry ->
                    val anchor = anchorForEntry(index, entries)
                    val x = mapWidth * anchor.x
                    val y = contentHeight * anchor.y
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
                                modifier = Modifier.offset(x = x - 36.dp, y = y - 36.dp).testTag("map_node_${entry.order}"),
                            )
                            if (state == GrowthMapModel.NodeState.CURRENT) {
                                CaterpillarArtwork(
                                    state = CaterpillarState.LEARNING,
                                    facingLeft = anchor.x > 0.5f,
                                    modifier = Modifier.offset(
                                        x = if (anchor.x > 0.5f) x - 115.dp else x + 38.dp,
                                        y = y - 18.dp,
                                    ).height(94.dp).width(96.dp),
                                )
                            }
                        }
                        is GrowthMapModel.MapEntry.StageTestNode -> {
                            val holeAnchor = FullTreeAnchorCatalog.stageHoleAnchor(entry.batchIndex)
                            val holeX = mapWidth * holeAnchor.x
                            val holeY = contentHeight * holeAnchor.y
                            TreeHoleGate(
                                batchIndex = entry.batchIndex,
                                number = entry.afterCharacterOrder,
                                unlocked = state == GrowthMapModel.NodeState.CURRENT || state == GrowthMapModel.NodeState.COMPLETED,
                                completed = state == GrowthMapModel.NodeState.COMPLETED,
                                remaining = (entry.afterCharacterOrder - learnedCount).coerceAtLeast(0),
                                onClick = { if (state == GrowthMapModel.NodeState.CURRENT) onOpenStageTest(entry.batchIndex) },
                                modifier = Modifier.offset(x = holeX - 52.dp, y = holeY - 35.dp),
                            )
                            if (state == GrowthMapModel.NodeState.CURRENT) {
                                CaterpillarArtwork(
                                    state = CaterpillarState.CHALLENGE,
                                    facingLeft = false,
                                    modifier = Modifier.offset(x = holeX + 45.dp, y = holeY - 18.dp).height(94.dp).width(96.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun anchorForEntry(index: Int, entries: List<GrowthMapModel.MapEntry>): NormalizedPoint {
    return when (val entry = entries.getOrNull(index)) {
        is GrowthMapModel.MapEntry.CharacterNode -> FullTreeAnchorCatalog.fruitAnchor(entry.order)
        is GrowthMapModel.MapEntry.StageTestNode -> FullTreeAnchorCatalog.stageHoleAnchor(entry.batchIndex)
        null -> FullTreeAnchorCatalog.fruitAnchor(1)
    }
}
