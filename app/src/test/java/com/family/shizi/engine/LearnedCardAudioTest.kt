package com.family.shizi.engine

import com.family.shizi.content.TestContent
import com.family.shizi.domain.engine.LearnedCardAudio
import org.junit.Assert.assertEquals
import org.junit.Test

class LearnedCardAudioTest {
    private val character = TestContent.packageData().characters.first()

    @Test fun tapPlaysCharacterAndFirstWord() {
        val assets = LearnedCardAudio.tapAssets(character)
        // 单击：字音 + 第 1 个词语
        assertEquals(2, assets.size)
        assertEquals(character.audio.character, assets[0])
        assertEquals(character.words.first().audioAsset, assets[1])
    }

    @Test fun longPressPlaysCharacterAllWordsAndSentence() {
        val assets = LearnedCardAudio.longPressAssets(character)
        // 长按：字音 + 全部词语 + 例句
        assertEquals(1 + character.words.size + 1, assets.size)
        assertEquals(character.audio.character, assets[0])
        assertEquals(character.words.map { it.audioAsset }, assets.subList(1, 1 + character.words.size))
        assertEquals(character.sentence.audioAsset, assets.last())
    }

    @Test fun tapOrderIsStable() {
        assertEquals(LearnedCardAudio.tapAssets(character), LearnedCardAudio.tapAssets(character))
    }
}
