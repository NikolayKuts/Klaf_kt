package com.kuts.klaf.authentication

import com.kuts.klaf.authentication.EmailValidator.*
import com.kuts.klaf.authentication.EmailValidator.IEmailValidationResult.*

class EmailValidator : IValidator<String, IEmailValidationResult> {

    companion object {

        // RFC-level strict validation is unnecessary here; we only need robust UI-level checks.
        private val EMAIL_REGEX = Regex(
            pattern = "^[A-Za-z0-9+_.%\\-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        )
    }

    sealed interface IEmailValidationResult : IValidator.IValidationResult {

        object Valid : IEmailValidationResult

        object Empty : IEmailValidationResult

        object WrongFormat : IEmailValidationResult
    }

    override fun validate(data: String): IEmailValidationResult = when {
        data.isEmpty() -> Empty
        !EMAIL_REGEX.matches(data) -> WrongFormat
        else -> Valid
    }
}
