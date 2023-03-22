package com.example.klaf.presentation.authentication

interface Validator<T> {

    fun validate(data: T): ValidationResult

    sealed interface ValidationResult
}