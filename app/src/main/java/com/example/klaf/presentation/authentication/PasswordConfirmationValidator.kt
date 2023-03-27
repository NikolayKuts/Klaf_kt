package com.example.klaf.presentation.authentication

import com.example.klaf.presentation.authentication.PasswordConfirmationValidator.*
import com.example.klaf.presentation.authentication.PasswordConfirmationValidator.PasswordConfirmationValidationResult.*

class PasswordConfirmationValidator :
    Validator<PasswordConfirmationSate, PasswordConfirmationValidationResult> {

    sealed interface PasswordConfirmationValidationResult : Validator.ValidationResult {

        object Empty : PasswordConfirmationValidationResult

        object NotIdentical : PasswordConfirmationValidationResult

        object Valid : PasswordConfirmationValidationResult
    }

    override fun validate(data: PasswordConfirmationSate): PasswordConfirmationValidationResult = when {
        data.confirmation.isEmpty() -> Empty
        data.password != data.confirmation -> NotIdentical
        else -> Valid
    }
}