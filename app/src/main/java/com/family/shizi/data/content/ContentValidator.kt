package com.family.shizi.data.content

object ContentValidator {
    private val requiredFirstTypes = setOf(
        QuestionType.CHARACTER_CHOOSE_IMAGE,
        QuestionType.LISTEN_CHOOSE_CHARACTER,
        QuestionType.CHARACTER_CHOOSE_AUDIO,
    )
    private val d14Types = setOf(
        QuestionType.LISTEN_CHOOSE_CHARACTER,
        QuestionType.CHARACTER_CHOOSE_AUDIO,
    )
    private val evidenceMapping = mapOf(
        QuestionType.LISTEN_CHOOSE_CHARACTER to EvidenceCategory.SOUND_TO_SHAPE,
        QuestionType.CHARACTER_CHOOSE_IMAGE to EvidenceCategory.SHAPE_TO_MEANING,
        QuestionType.CHARACTER_CHOOSE_AUDIO to EvidenceCategory.SHAPE_TO_SOUND,
        QuestionType.SHAPE_RECOGNITION to EvidenceCategory.SHAPE,
        QuestionType.LIFE_WORD_CONTEXT to EvidenceCategory.CONTEXT,
    )

    fun validate(content: ContentPackage): ContentValidationResult {
        val errors = mutableListOf<ContentValidationError>()
        fun error(code: ContentErrorCode, path: String, message: String) {
            errors += ContentValidationError(code, path, message)
        }

        checkRequiredStrings(content, ::error)
        checkUnique(content.characters.map { it.id }, "$.characters.id", ::error)
        checkUnique(content.characters.map { it.character }, "$.characters.character", ::error)
        checkUnique(content.characters.map { it.order }, "$.characters.order", ::error)
        checkUnique(content.optionCatalog.map { it.id }, "$.optionCatalog.id", ::error)
        checkUnique(content.characters.flatMap { it.questionSeeds }.map { it.id }, "$.questionSeeds.id", ::error)

        val characterIds = content.characters.map { it.id }
        if (content.learningOrder != content.characters.sortedBy { it.order }.map { it.id } ||
            content.learningOrder.toSet() != characterIds.toSet() ||
            content.learningOrder.size != characterIds.size
        ) {
            error(ContentErrorCode.LEARNING_ORDER_INVALID, "$.learningOrder", "Must exactly match characters ordered by order")
        }

        if (content.reviewOffsetsDays.isEmpty() ||
            content.reviewOffsetsDays.zipWithNext().any { (a, b) -> a >= b }
        ) {
            error(ContentErrorCode.REVIEW_OFFSETS_INVALID, "$.reviewOffsetsDays", "Offsets must be strictly increasing")
        }

        val options = content.optionCatalog.associateBy { it.id }
        content.optionCatalog.forEachIndexed { index, option ->
            val field = "$.optionCatalog[$index].asset"
            when (option.kind) {
                OptionKind.IMAGE, OptionKind.CONTEXT -> option.asset?.let { validateImagePath(it, field, ::error) }
                OptionKind.AUDIO -> option.asset?.let { validateAudioPath(it, field, ::error) }
                OptionKind.TEXT -> Unit
            }
        }

        content.characters.forEachIndexed { characterIndex, character ->
            val base = "$.characters[$characterIndex]"
            validateImagePath(character.imageAsset, "$base.imageAsset", ::error)
            character.words.forEachIndexed { index, word ->
                validateAudioPath(word.audioAsset, "$base.words[$index].audioAsset", ::error)
            }
            validateAudioPath(character.sentence.audioAsset, "$base.sentence.audioAsset", ::error)
            validateAudioPath(character.audio.character, "$base.audio.character", ::error)
            validateAudioPath(character.audio.meaning, "$base.audio.meaning", ::error)

            val presentTypes = character.questionSeeds.map { it.type }.toSet()
            val evidence = character.questionSeeds.map { it.evidenceCategory }.toSet()
            val first = character.questionSeeds.filter { it.type in requiredFirstTypes }
            val d14 = character.questionSeeds.filter { it.type in d14Types }
            if (!presentTypes.containsAll(requiredFirstTypes) || first.map { it.type }.toSet().size != 3 ||
                first.any { it.minLearnedCount != 0 } || presentTypes.size < 4 || evidence.size < 4 ||
                !presentTypes.containsAll(d14Types) || d14.map { it.type }.toSet().size != 2 ||
                d14.any { it.minLearnedCount !in 0..5 }
            ) {
                error(ContentErrorCode.CONTENT_REACHABILITY_FAILED, "$base.questionSeeds", "First 3, D14 2, and 4 evidence types must be reachable")
            }

            character.questionSeeds.forEachIndexed { questionIndex, question ->
                val path = "$base.questionSeeds[$questionIndex]"
                validateAudioPath(question.promptAudio, "$path.promptAudio", ::error)
                val expectedEvidence = evidenceMapping[question.type]
                if (question.evidenceCategory != expectedEvidence) {
                    error(ContentErrorCode.EVIDENCE_MAPPING_INVALID, "$path.evidenceCategory", "${question.type} requires $expectedEvidence")
                }
                val correctCount = question.optionIds.count { it == question.correctOptionId }
                if (correctCount != 1) {
                    error(ContentErrorCode.CORRECT_OPTION_INVALID, "$path.correctOptionId", "Correct option must occur exactly once")
                }
                question.optionIds.forEachIndexed { optionIndex, optionId ->
                    val option = options[optionId]
                    if (option == null) {
                        error(ContentErrorCode.OPTION_REFERENCE_MISSING, "$path.optionIds[$optionIndex]", "Unknown option $optionId")
                    } else {
                        val visibleText = option.text.orEmpty()
                        val forbidden = character.confusableRestrictions.firstOrNull { visibleText.contains(it) }
                        if (forbidden != null) {
                            error(ContentErrorCode.CONFUSABLE_OPTION_FORBIDDEN, "$path.optionIds[$optionIndex]", "Contains forbidden character $forbidden")
                        }
                    }
                }
                if (question.minLearnedCount !in 0..5) {
                    error(ContentErrorCode.CONTENT_REACHABILITY_FAILED, "$path.minLearnedCount", "Must be between 0 and 5")
                }
            }

            val review = character.contentReview
            val reviewStateValid = review.textReviewed &&
                review.assetReviewedByDeveloper == review.assetReviewedByParent &&
                if (review.assetReviewedByDeveloper) review.blockedReason == null else !review.blockedReason.isNullOrBlank()
            if (!reviewStateValid) {
                error(ContentErrorCode.CONTENT_REVIEW_INVALID, "$base.contentReview", "Asset reviews must agree; completed review requires null blockedReason")
            }
        }
        return ContentValidationResult(errors)
    }

    private fun checkUnique(values: List<Any>, path: String, error: (ContentErrorCode, String, String) -> Unit) {
        if (values.size != values.toSet().size) error(ContentErrorCode.CONTENT_ID_DUPLICATE, path, "Values must be unique")
    }

    private fun validateAudioPath(path: String, field: String, error: (ContentErrorCode, String, String) -> Unit) =
        validateTypedAssetPath(path, field, "audio/", ".mp3", error)

    private fun validateImagePath(path: String, field: String, error: (ContentErrorCode, String, String) -> Unit) =
        validateTypedAssetPath(path, field, "images/", ".webp", error)

    private fun validateTypedAssetPath(
        path: String,
        field: String,
        requiredPrefix: String,
        requiredExtension: String,
        error: (ContentErrorCode, String, String) -> Unit,
    ) {
        val validPrefixAndExtension = path.startsWith(requiredPrefix) && path.endsWith(requiredExtension)
        val invalid = path.isBlank() || path.contains("..") || path.contains('\\') ||
            path.startsWith('/') || Regex("^[A-Za-z]:").containsMatchIn(path) ||
            path.contains("://") || path.startsWith("file:") || !validPrefixAndExtension
        if (invalid) error(
            ContentErrorCode.RESOURCE_PATH_INVALID,
            field,
            "Expected $requiredPrefix*$requiredExtension relative asset path: $path",
        )
    }

    private fun checkRequiredStrings(
        content: ContentPackage,
        error: (ContentErrorCode, String, String) -> Unit,
    ) {
        fun require(value: String, path: String) {
            if (value.isBlank()) error(ContentErrorCode.REQUIRED_STRING_EMPTY, path, "Required string must not be blank")
        }
        require(content.contentVersion, "$.contentVersion")
        content.optionCatalog.forEachIndexed { i, option ->
            require(option.id, "$.optionCatalog[$i].id")
            require(option.characterId, "$.optionCatalog[$i].characterId")
            option.text?.let { require(it, "$.optionCatalog[$i].text") }
            option.asset?.let { require(it, "$.optionCatalog[$i].asset") }
        }
        content.characters.forEachIndexed { i, character ->
            val base = "$.characters[$i]"
            listOf(
                character.id to "$base.id", character.character to "$base.character",
                character.pinyin to "$base.pinyin", character.meaningForChild to "$base.meaningForChild",
                character.imageAsset to "$base.imageAsset", character.imageAlt to "$base.imageAlt",
                character.teachingPrompt to "$base.teachingPrompt",
            ).forEach { (value, path) -> require(value, path) }
            character.contentReview.blockedReason?.let { require(it, "$base.contentReview.blockedReason") }
            character.words.forEachIndexed { w, word -> require(word.text, "$base.words[$w].text") }
            character.questionSeeds.forEachIndexed { q, question ->
                require(question.id, "$base.questionSeeds[$q].id")
                require(question.correctOptionId, "$base.questionSeeds[$q].correctOptionId")
                if (question.optionIds.isEmpty()) error(ContentErrorCode.REQUIRED_STRING_EMPTY, "$base.questionSeeds[$q].optionIds", "Must not be empty")
            }
        }
    }
}
