package com.kuts.klaf.presentation.authentication

import android.util.Patterns
import com.kuts.klaf.presentation.authentication.EmailValidator.*
import com.kuts.klaf.presentation.authentication.EmailValidator.IEmailValidationResult.*

class EmailValidator : IValidator<String, IEmailValidationResult> {

    sealed interface IEmailValidationResult : IValidator.IValidationResult {

        object Valid : IEmailValidationResult

        object Empty : IEmailValidationResult

        object WrongFormat : IEmailValidationResult
    }

    override fun validate(data: String): IEmailValidationResult = when {
        data.isEmpty() -> Empty
        !Patterns.EMAIL_ADDRESS.matcher(data).matches() -> WrongFormat
        else -> Valid
    }
}