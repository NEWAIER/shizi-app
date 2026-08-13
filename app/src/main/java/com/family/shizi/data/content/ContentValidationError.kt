package com.family.shizi.data.content

enum class ContentErrorCode {
    CONTENT_STRUCTURE_INVALID,
    CONTENT_ID_DUPLICATE,
    LEARNING_ORDER_INVALID,
    CORRECT_OPTION_INVALID,
    OPTION_REFERENCE_MISSING,
    RESOURCE_PATH_INVALID,
    CONTENT_REACHABILITY_FAILED,
    CONFUSABLE_OPTION_FORBIDDEN,
    CONTENT_REVIEW_INVALID,
    REVIEW_OFFSETS_INVALID,
    REQUIRED_STRING_EMPTY,
    EVIDENCE_MAPPING_INVALID,
}

data class ContentValidationError(
    val code: ContentErrorCode,
    val path: String,
    val message: String,
)

class ContentValidationException(
    val validationError: ContentValidationError,
    cause: Throwable? = null,
) : IllegalArgumentException("${validationError.code}: ${validationError.path}: ${validationError.message}", cause)
