package com.kuts.klaf.presentation.authentication

import com.kuts.klaf.presentation.authentication.PasswordConfirmationValidator.*
import com.kuts.klaf.presentation.authentication.PasswordConfirmationValidator.IPasswordConfirmationValidationResult.*

class PasswordConfirmationValidator :
    IValidator<PasswordConfirmationSate, IPasswordConfirmationValidationResult> {

    sealed interface IPasswordConfirmationValidationResult : IValidator.IValidationResult {

        object Empty : IPasswordConfirmationValidationResult

        object NotIdentical : IPasswordConfirmationValidationResult

        object Valid : IPasswordConfirmationValidationResult
    }

    override fun validate(data: PasswordConfirmationSate): IPasswordConfirmationValidationResult = when {
        data.confirmation.isEmpty() -> Empty
        data.password != data.confirmation -> NotIdentical
        else -> Valid
    }
}