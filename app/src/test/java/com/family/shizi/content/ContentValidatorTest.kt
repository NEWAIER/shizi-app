package com.family.shizi.content

import com.family.shizi.data.content.ContentValidator
import com.family.shizi.data.content.EvidenceCategory
import com.family.shizi.data.content.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentValidatorTest {
    @Test fun frozenContentPassesAllG1Rules() {
        val content = TestContent.packageData()
        val result = ContentValidator.validate(content)
        assertTrue(result.errors.joinToString("\n"), result.isValid)
        assertEquals(listOf(4, 4, 5, 5, 4), content.characters.map { it.questionSeeds.map { q -> q.type }.distinct().size })
        assertEquals(List(5) { 3 }, content.characters.map { character ->
            character.questionSeeds.count { it.type in firstTypes && it.minLearnedCount == 0 }
        })
        assertEquals(List(5) { 2 }, content.characters.map { character -> character.questionSeeds.count { it.type in d14Types } })
        assertTrue(content.characters.all {
            it.contentReview.assetReviewedByDeveloper && it.contentReview.assetReviewedByParent && it.contentReview.blockedReason == null
        })
    }

    @Test fun typeToEvidenceMappingIsExact() {
        val actual = TestContent.packageData().characters.flatMap { it.questionSeeds }
            .associate { it.type to it.evidenceCategory }
        assertEquals(
            mapOf(
                QuestionType.LISTEN_CHOOSE_CHARACTER to EvidenceCategory.SOUND_TO_SHAPE,
                QuestionType.CHARACTER_CHOOSE_IMAGE to EvidenceCategory.SHAPE_TO_MEANING,
                QuestionType.CHARACTER_CHOOSE_AUDIO to EvidenceCategory.SHAPE_TO_SOUND,
                QuestionType.SHAPE_RECOGNITION to EvidenceCategory.SHAPE,
                QuestionType.LIFE_WORD_CONTEXT to EvidenceCategory.CONTEXT,
            ),
            actual,
        )
    }

    companion object {
        val firstTypes = setOf(QuestionType.CHARACTER_CHOOSE_IMAGE, QuestionType.LISTEN_CHOOSE_CHARACTER, QuestionType.CHARACTER_CHOOSE_AUDIO)
        val d14Types = setOf(QuestionType.LISTEN_CHOOSE_CHARACTER, QuestionType.CHARACTER_CHOOSE_AUDIO)
    }
}
