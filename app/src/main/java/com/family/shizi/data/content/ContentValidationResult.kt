package com.family.shizi.data.content

data class ContentValidationResult(val errors: List<ContentValidationError>) {
    val isValid: Boolean get() = errors.isEmpty()
    fun has(code: ContentErrorCode): Boolean = errors.any { it.code == code }

    fun requireValid(): ContentValidationResult {
        if (!isValid) throw ContentValidationException(errors.first())
        return this
    }
}
