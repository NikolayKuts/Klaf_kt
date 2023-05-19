package com.kuts.klaf.presentation.authentication

import com.kuts.klaf.presentation.authentication.PasswordValidator.*
import com.kuts.klaf.presentation.authentication.PasswordValidator.PasswordValidationResult.*

class PasswordValidator : Validator<String, PasswordValidationResult> {

    sealed interface PasswordValidationResult : Validator.ValidationResult {

        object Empty : PasswordValidationResult

        object ToShort : PasswordValidationResult

        object ToLong : PasswordValidationResult

        object Valid : PasswordValidationResult
    }

    companion object {

        const val MAX_LENGTH = 100
        const val MIN_LENGTH = 8
    }

    override fun validate(data: String): PasswordValidationResult = when {
        data.isEmpty() -> Empty
        data.length >= MAX_LENGTH -> ToLong
        data.length < MIN_LENGTH -> ToShort
        else -> Valid
    }
}