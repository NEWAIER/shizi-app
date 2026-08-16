package com.family.shizi.ui.home.tree

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.domain.engine.GrowthMapModel
import com.family.shizi.ui.home.components.CaterpillarState
import com.family.shizi.ui.home.components.GameMapNode
import com.family.shizi.ui.home.components.TreeHoleGate
import com.family.shizi.ui.home.components.assets.CaterpillarArtwork

data class SegmentCharacterEntry(
    val globalMapIndex: Int,
    val localCharacterIndex: Int,
    val characterOrder: Int,
    val character: CharacterContent,
    val state: GrowthMapModel.NodeState,
)

data class SegmentStageEntry(
    val globalMapIndex: Int,
    val batchIndex: Int,
    val afterCharacterOrder: Int,
    val state: GrowthMapModel.NodeState,
)

@Composable
fun TreeSegmentView(
    segmentIndex: Int,
    spec: TreeSegmentSpec,
    characterEntries: List<SegmentCharacterEntry>,
    stageEntry: SegmentStageEntry?,
    currentGlobalMapIndex: Int?,
    onLearn: (String) -> Unit,
    onStageTest: (Int) -> Unit,
    onCompletedTap: (CharacterContent) -> Unit,
    onCompletedLongPress: (CharacterContent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().aspectRatio(1080f / 1800f),
    ) {
        val width = maxWidth
        val height = maxHeight
        Image(
            painter = painterResource(spec.artworkRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        characterEntries.forEach { entry ->
            val anchor = spec.fruitAnchors[entry.localCharacterIndex]
            val x = width * anchor.x
            val y = height * anchor.y
            Box(
                modifier = Modifier.offset(x = x - 36.dp, y = y - 36.dp).size(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                GameMapNode(
                    character = entry.character.character,
                    number = entry.characterOrder,
                    state = entry.state,
                    onClick = { if (entry.state == GrowthMapModel.NodeState.CURRENT) onLearn(entry.character.id) },
                    onTap = { if (entry.state == GrowthMapModel.NodeState.COMPLETED) onCompletedTap(entry.character) },
                    onLongPress = { if (entry.state == GrowthMapModel.NodeState.COMPLETED) onCompletedLongPress(entry.character) },
                )
            }
            if (entry.globalMapIndex == currentGlobalMapIndex) {
                CaterpillarArtwork(
                    state = CaterpillarState.LEARNING,
                    facingLeft = anchor.x > 0.5f,
                    modifier = Modifier.offset(
                        x = if (anchor.x > 0.5f) x - 115.dp else x + 38.dp,
                        y = y - 18.dp,
                    ).size(width = 96.dp, height = 94.dp),
                )
            }
        }

        stageEntry?.let { entry ->
            val anchor = spec.stageHoleAnchor
            val x = width * anchor.x
            val y = height * anchor.y
            TreeHoleGate(
                batchIndex = entry.batchIndex,
                number = entry.afterCharacterOrder,
                unlocked = entry.state == GrowthMapModel.NodeState.CURRENT || entry.state == GrowthMapModel.NodeState.COMPLETED,
                completed = entry.state == GrowthMapModel.NodeState.COMPLETED,
                remaining = 0,
                onClick = { if (entry.state == GrowthMapModel.NodeState.CURRENT) onStageTest(entry.batchIndex) },
                modifier = Modifier.offset(x = x - 52.dp, y = y - 35.dp),
            )
            if (entry.globalMapIndex == currentGlobalMapIndex) {
                CaterpillarArtwork(
                    state = CaterpillarState.CHALLENGE,
                    facingLeft = anchor.x > 0.5f,
                    modifier = Modifier.offset(
                        x = if (anchor.x > 0.5f) x - 120.dp else x + 45.dp,
                        y = y - 18.dp,
                    ).size(width = 96.dp, height = 94.dp),
                )
            }
        }
    }
}
