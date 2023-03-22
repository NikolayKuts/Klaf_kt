package com.example.klaf.presentation.authentication

import android.util.Patterns
import com.example.klaf.presentation.authentication.EmailValidator.*
import com.example.klaf.presentation.authentication.EmailValidator.EmailValidationResult.*

class EmailValidator : Validator<String, EmailValidationResult> {

    sealed interface EmailValidationResult : Validator.ValidationResult {

        object Valid : EmailValidationResult

        object Empty : EmailValidationResult

        object WrongFormat : EmailValidationResult
    }

    override fun validate(data: String): EmailValidationResult = when {
        data.isEmpty() -> Empty
        !Patterns.EMAIL_ADDRESS.matcher(data).matches() -> WrongFormat
        else -> Valid
    }
}