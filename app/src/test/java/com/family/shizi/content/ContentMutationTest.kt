package com.family.shizi.content

import com.family.shizi.content.TestContent.replaceQuestion
import com.family.shizi.data.content.ContentErrorCode
import com.family.shizi.data.content.ContentPackage
import com.family.shizi.data.content.ContentValidator
import com.family.shizi.data.content.EvidenceCategory
import com.family.shizi.data.content.OptionKind
import com.family.shizi.data.content.QuestionType
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentMutationTest {
    @Test fun removingCorrectOptionFailsWithStableCode() = assertCode(
        TestContent.packageData().replaceQuestion { it.copy(optionIds = it.optionIds.filterNot { id -> id == it.correctOptionId }) },
        ContentErrorCode.CORRECT_OPTION_INVALID,
    )

    @Test fun duplicateCorrectOptionFailsWithStableCode() = assertCode(
        TestContent.packageData().replaceQuestion { it.copy(optionIds = it.optionIds + it.correctOptionId) },
        ContentErrorCode.CORRECT_OPTION_INVALID,
    )

    @Test fun duplicateCharacterIdFails() {
        val source = TestContent.packageData()
        val changed = source.characters.toMutableList().also { it[1] = it[1].copy(id = it[0].id) }
        assertCode(source.copy(characters = changed), ContentErrorCode.CONTENT_ID_DUPLICATE)
    }

    @Test fun duplicateCharacterAndOrderFail() {
        val source = TestContent.packageData()
        val duplicateCharacter = source.characters.toMutableList().also { it[1] = it[1].copy(character = it[0].character) }
        val duplicateOrder = source.characters.toMutableList().also { it[1] = it[1].copy(order = it[0].order) }
        assertCode(source.copy(characters = duplicateCharacter), ContentErrorCode.CONTENT_ID_DUPLICATE)
        assertCode(source.copy(characters = duplicateOrder), ContentErrorCode.CONTENT_ID_DUPLICATE)
    }

    @Test fun missingOptionReferenceFails() = assertCode(
        TestContent.packageData().replaceQuestion { it.copy(optionIds = it.optionIds + "missing_option") },
        ContentErrorCode.OPTION_REFERENCE_MISSING,
    )

    @Test fun removingCharacterChooseAudioFailsReachability() {
        val source = TestContent.packageData()
        val characters = source.characters.toMutableList()
        characters[0] = characters[0].copy(questionSeeds = characters[0].questionSeeds.filterNot { it.type == QuestionType.CHARACTER_CHOOSE_AUDIO })
        assertCode(source.copy(characters = characters), ContentErrorCode.CONTENT_REACHABILITY_FAILED)
    }

    @Test fun reducingCharacterToThreeTypesFailsReachability() {
        val source = TestContent.packageData()
        val characters = source.characters.toMutableList()
        characters[0] = characters[0].copy(questionSeeds = characters[0].questionSeeds.take(3))
        assertCode(source.copy(characters = characters), ContentErrorCode.CONTENT_REACHABILITY_FAILED)
    }

    @Test fun firstQuestionWithLearnedCountOneFailsReachability() = assertCode(
        TestContent.packageData().replaceQuestion { it.copy(minLearnedCount = 1) },
        ContentErrorCode.CONTENT_REACHABILITY_FAILED,
    )

    @Test fun invalidRelativeAbsoluteAndUrlPathsFail() {
        listOf("../x.mp3", "/x.mp3", "https://example.invalid/x.mp3").forEach { path ->
            assertCode(TestContent.packageData().replaceQuestion { it.copy(promptAudio = path) }, ContentErrorCode.RESOURCE_PATH_INVALID)
        }
    }

    @Test fun imageOptionReferencingMp3Fails() = assertOptionAssetKindFails(
        OptionKind.IMAGE,
        "audio/options/wrong.mp3",
    )

    @Test fun audioOptionReferencingWebpFails() = assertOptionAssetKindFails(
        OptionKind.AUDIO,
        "images/options/wrong.webp",
    )

    @Test fun contextOptionReferencingMp3Fails() = assertOptionAssetKindFails(
        OptionKind.CONTEXT,
        "audio/options/wrong.mp3",
    )

    @Test fun promptAudioReferencingWebpFails() = assertCode(
        TestContent.packageData().replaceQuestion { it.copy(promptAudio = "images/prompts/wrong.webp") },
        ContentErrorCode.RESOURCE_PATH_INVALID,
    )

    @Test fun imageAssetReferencingMp3Fails() {
        val source = TestContent.packageData()
        val characters = source.characters.toMutableList()
        characters[0] = characters[0].copy(imageAsset = "audio/characters/wrong.mp3")
        assertCode(source.copy(characters = characters), ContentErrorCode.RESOURCE_PATH_INVALID)
    }

    @Test fun forbiddenConfusableCharacterInChildOptionFails() {
        val source = TestContent.packageData()
        val options = source.optionCatalog.map { if (it.id == "text_char_kou") it.copy(text = "入") else it }
        assertCode(source.copy(optionCatalog = options), ContentErrorCode.CONFUSABLE_OPTION_FORBIDDEN)
    }

    @Test fun textReviewFalseFails() {
        val source = TestContent.packageData()
        val characters = source.characters.toMutableList()
        characters[0] = characters[0].copy(contentReview = characters[0].contentReview.copy(textReviewed = false))
        assertCode(source.copy(characters = characters), ContentErrorCode.CONTENT_REVIEW_INVALID)
    }

    @Test fun duplicateOrDescendingReviewOffsetsFail() {
        assertCode(TestContent.packageData().copy(reviewOffsetsDays = listOf(1, 3, 3, 14)), ContentErrorCode.REVIEW_OFFSETS_INVALID)
        assertCode(TestContent.packageData().copy(reviewOffsetsDays = listOf(1, 7, 3, 14)), ContentErrorCode.REVIEW_OFFSETS_INVALID)
    }

    @Test fun wrongEvidenceMappingFails() = assertCode(
        TestContent.packageData().replaceQuestion { it.copy(evidenceCategory = EvidenceCategory.CONTEXT) },
        ContentErrorCode.EVIDENCE_MAPPING_INVALID,
    )

    private fun assertCode(content: ContentPackage, code: ContentErrorCode) {
        val result = ContentValidator.validate(content)
        assertTrue("Expected $code, got ${result.errors}", result.has(code))
    }

    private fun assertOptionAssetKindFails(kind: OptionKind, asset: String) {
        val source = TestContent.packageData()
        val options = source.optionCatalog.toMutableList()
        val index = options.indexOfFirst { it.kind == kind }
        options[index] = options[index].copy(asset = asset)
        assertCode(source.copy(optionCatalog = options), ContentErrorCode.RESOURCE_PATH_INVALID)
    }
}
