package com.family.shizi.data.content

import kotlinx.serialization.Serializable

@Serializable
data class ContentPackage(
    val schemaVersion: Int,
    val contentVersion: String,
    val course: CourseConfig = CourseConfig(),
    val learningOrder: List<String>,
    val reviewOffsetsDays: List<Int>,
    val optionCatalog: List<OptionContent>,
    val characters: List<CharacterContent>,
)

@Serializable
data class CourseConfig(
    val stageTestThreshold: Int = 3,
    val badgeMilestones: List<BadgeMilestone> = listOf(
        BadgeMilestone("first_character", "启蒙星", "认识第一个字", 1),
        BadgeMilestone("three_characters", "三字小能手", "认识3个字", 3),
        BadgeMilestone("five_characters", "五字达人", "认识5个字", 5),
    ),
)

@Serializable data class BadgeMilestone(
    val id: String,
    val title: String,
    val detail: String,
    val learnedCount: Int,
)

@Serializable
data class CharacterContent(
    val id: String,
    val character: String,
    val pinyin: String,
    val toneNumber: Int,
    /** V2 extension. V1 keeps pinyin/toneNumber and decodes with an empty readings list. */
    val readings: List<ReadingContent> = emptyList(),
    val order: Int,
    val meaningForChild: String,
    val imageAsset: String,
    val imageAlt: String,
    val words: List<WordContent>,
    val sentence: SentenceContent,
    val audio: AudioRefs,
    val teachingPrompt: String,
    val confusableRestrictions: List<String>,
    val misconceptions: List<String>,
    val questionSeeds: List<QuestionSeed>,
    val contentReview: ContentReview,
)

@Serializable data class ReadingContent(val pinyin: String, val toneNumber: Int, val audioAsset: String? = null)

@Serializable data class WordContent(val text: String, val audioAsset: String)
@Serializable data class SentenceContent(val text: String, val audioAsset: String)
@Serializable data class AudioRefs(val character: String, val meaning: String)

@Serializable
data class OptionContent(
    val id: String,
    val kind: OptionKind,
    val characterId: String,
    val text: String? = null,
    val asset: String? = null,
)

@Serializable
data class QuestionSeed(
    val id: String,
    val type: QuestionType,
    val promptAudio: String,
    val correctOptionId: String,
    val optionIds: List<String>,
    val minLearnedCount: Int,
    val evidenceCategory: EvidenceCategory,
)

@Serializable
data class ContentReview(
    val textReviewed: Boolean,
    val assetReviewedByDeveloper: Boolean,
    val assetReviewedByParent: Boolean,
    val blockedReason: String?,
)

@Serializable
enum class QuestionType {
    CHARACTER_CHOOSE_IMAGE,
    LISTEN_CHOOSE_CHARACTER,
    CHARACTER_CHOOSE_AUDIO,
    SHAPE_RECOGNITION,
    LIFE_WORD_CONTEXT,
}

@Serializable
enum class EvidenceCategory {
    SHAPE_TO_MEANING,
    SOUND_TO_SHAPE,
    SHAPE_TO_SOUND,
    SHAPE,
    CONTEXT,
}

@Serializable enum class OptionKind { TEXT, IMAGE, AUDIO, CONTEXT }
