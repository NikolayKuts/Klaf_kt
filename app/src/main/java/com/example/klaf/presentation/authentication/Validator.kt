package com.example.klaf.presentation.authentication

import com.example.klaf.presentation.authentication.Validator.*

interface Validator<T, R: ValidationResult> {

    sealed interface ValidationResult

    fun validate(data: T): R
}