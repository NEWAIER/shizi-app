package com.family.shizi.ui.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.family.shizi.data.content.CharacterContent
import com.family.shizi.domain.engine.GrowthMapModel

/** 每个节点在章节内的 x 位置（蛇形摆动），保证 360dp 宽度不重叠。 */
private val nodeXs = listOf(78, 150, 222, 168, 96, 210, 120, 240, 60, 186)

private val stepY = 96

/**
 * 成长森林地图：按章节分区展示全部节点。
 * 每章一个横幅 + 蜿蜒路径 + 节点 + 树洞 + 当前节点旁的毛毛虫。
 * 无限动画仅出现在：当前节点、毛毛虫、已解锁树洞。
 */
@Composable
fun GrowthMap(
    characters: List<CharacterContent>,
    learnedCount: Int,
    dailyTarget: Int,
    onLearn: () -> Unit,
    onOpenStageTest: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chapters = GrowthMapModel.chapters(characters.size)
    val currentChapter = GrowthMapModel.currentChapterIndex(learnedCount, characters.size)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_growth_tree"),
    ) {
        chapters.forEach { chapter ->
            ChapterSection(
                chapter = chapter,
                characters = characters,
                learnedCount = learnedCount,
                dailyTarget = dailyTarget,
                isCurrentChapter = chapter.index == currentChapter,
                onLearn = onLearn,
                onOpenStageTest = onOpenStageTest,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ChapterSection(
    chapter: GrowthMapModel.Chapter,
    characters: List<CharacterContent>,
    learnedCount: Int,
    dailyTarget: Int,
    isCurrentChapter: Boolean,
    onLearn: () -> Unit,
    onOpenStageTest: (Int) -> Unit,
) {
    val bannerColor = chapterBannerColor(chapter.index)
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        ChapterBanner(chapter = chapter, current = isCurrentChapter, bannerColor = bannerColor)
        val chapterCharacters = characters.filter { it.order in chapter.startNumber..chapter.endNumber }
        val topMargin = 40
        val bottomMargin = 96
        val chapterHeight = topMargin.dp + ((chapterCharacters.size - 1).coerceAtLeast(0) * stepY).dp + bottomMargin.dp
        val focusRequester = remember { BringIntoViewRequester() }
        LaunchedEffect(isCurrentChapter) {
            if (isCurrentChapter) {
                delay(120)
                focusRequester.bringIntoView()
            }
        }
        ForestBackdrop(chapterIndex = chapter.index, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).bringIntoViewRequester(focusRequester)) {
            Box(modifier = Modifier.fillMaxWidth().height(chapterHeight)) {
                // 蜿蜒路径：连接本章节节点
                val points = chapterCharacters.mapIndexed { index, _ ->
                    val number = chapter.startNumber + index
                    androidx.compose.ui.unit.DpOffset(
                        nodeXs[(number - 1) % nodeXs.size].dp,
                        (topMargin + index * stepY).dp,
                    )
                }
                GamePath(
                    points = points,
                    modifier = Modifier.fillMaxWidth().height(chapterHeight),
                    color = bannerColor,
                )
                chapterCharacters.forEachIndexed { index, character ->
                    val number = chapter.startNumber + index
                    val state = GrowthMapModel.nodeState(number, learnedCount, dailyTarget, characters.size)
                    GameMapNode(
                        character = character.character,
                        number = number,
                        state = state,
                        onClick = onLearn,
                        modifier = Modifier
                            .offset(
                                x = (nodeXs[(number - 1) % nodeXs.size] - 28).dp,
                                y = (topMargin + index * stepY).dp,
                            )
                            .testTag("map_node_$number"),
                    )
                }
                // 章节末尾树洞
                val lastNumber = chapter.endNumber
                if (lastNumber % GrowthMapModel.CHAPTER_SIZE == 0) {
                    val batchIndex = lastNumber / GrowthMapModel.CHAPTER_SIZE - 1
                    val holeUnlocked = GrowthMapModel.chapterUnlocked(batchIndex, learnedCount)
                    val remaining = (lastNumber - learnedCount).coerceAtLeast(0)
                    val lastX = nodeXs[(lastNumber - 1) % nodeXs.size]
                    TreeHoleGate(
                        batchIndex = batchIndex,
                        number = lastNumber,
                        unlocked = holeUnlocked,
                        completed = learnedCount > lastNumber,
                        remaining = remaining,
                        onClick = { onOpenStageTest(batchIndex) },
                        modifier = Modifier.offset(
                            x = if (lastX < 150) 168.dp else 14.dp,
                            y = (topMargin + (lastNumber - chapter.startNumber) * stepY + 84).dp,
                        ),
                    )
                }
                // 毛毛虫：停在当前节点旁
                if (learnedCount >= chapter.startNumber && learnedCount < chapter.endNumber) {
                    val indexInChapter = learnedCount - chapter.startNumber
                    val cx = nodeXs[learnedCount % nodeXs.size]
                    val cy = topMargin + indexInChapter * stepY + 92
                    CaterpillarMascot(
                        segmentCount = 2 + (learnedCount - chapter.startNumber).coerceAtMost(6),
                        state = CaterpillarState.LEARNING,
                        modifier = Modifier.offset(x = (cx - 26).dp, y = cy.dp),
                    )
                }
            }
        }
    }
}
