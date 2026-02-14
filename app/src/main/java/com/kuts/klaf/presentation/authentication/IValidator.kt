package com.kuts.klaf.presentation.authentication

import com.kuts.klaf.presentation.authentication.IValidator.*

interface IValidator<T, R: IValidationResult> {

    sealed interface IValidationResult

    fun validate(data: T): R
}