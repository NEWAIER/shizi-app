package com.family.shizi.data.content

data class G2ResourceValidationResult(val errors: List<G2ResourceValidationError>) {
    val isValid: Boolean get() = errors.isEmpty()
    val childTrialEnabled: Boolean get() = isValid
    fun has(code: G2ResourceErrorCode): Boolean = errors.any { it.code == code }
}
