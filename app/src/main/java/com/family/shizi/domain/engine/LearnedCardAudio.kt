package com.family.shizi.domain.engine

import com.family.shizi.data.content.CharacterContent

/**
 * 字卡（图鉴卡）音频行为纯逻辑：
 * - 单击：字音 + 第 1 个词语
 * - 长按：字音 + 全部词语 + 例句
 * 保证规则单一来源、可单测，UI 只负责播放返回的 asset 序列。
 */
object LearnedCardAudio {
    fun tapAssets(character: CharacterContent): List<String> =
        listOfNotNull(character.audio.character, character.words.firstOrNull()?.audioAsset)

    fun longPressAssets(character: CharacterContent): List<String> =
        buildList {
            add(character.audio.character)
            addAll(character.words.map { it.audioAsset })
            add(character.sentence.audioAsset)
        }
}
