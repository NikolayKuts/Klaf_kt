package com.kuts.klaf.presentation.authentication

import com.kuts.klaf.presentation.authentication.Validator.*

interface Validator<T, R: ValidationResult> {

    sealed interface ValidationResult

    fun validate(data: T): R
}