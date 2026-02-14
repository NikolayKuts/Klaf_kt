package com.kuts.klaf.authentication

import com.kuts.klaf.authentication.IValidator.*

interface IValidator<T, R: IValidationResult> {

    sealed interface IValidationResult

    fun validate(data: T): R
}