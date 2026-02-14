package com.kuts.klaf.presentation.authentication

import com.kuts.klaf.presentation.authentication.PasswordValidator.*
import com.kuts.klaf.presentation.authentication.PasswordValidator.IPasswordValidationResult.*

class PasswordValidator : IValidator<String, IPasswordValidationResult> {

    sealed interface IPasswordValidationResult : IValidator.IValidationResult {

        data object Empty : IPasswordValidationResult

        data object ToShort : IPasswordValidationResult

        data object ToLong : IPasswordValidationResult

        data object Valid : IPasswordValidationResult
    }

    companion object {

        const val MAX_LENGTH = 100
        const val MIN_LENGTH = 8
    }

    override fun validate(data: String): IPasswordValidationResult = when {
        data.isEmpty() -> Empty
        data.length >= MAX_LENGTH -> ToLong
        data.length < MIN_LENGTH -> ToShort
        else -> Valid
    }
}