package com.kuts.domain.common

import kotlinx.serialization.Serializable

@Serializable
sealed class LoadingState<out T, out E> {
    @Serializable
    data object Non : LoadingState<Nothing, Nothing>()
    @Serializable
    data class Success<T>(val data: T) : LoadingState<T, Nothing>()
    @Serializable
    data object Loading : LoadingState<Nothing, Nothing>()
    @Serializable
    data class Error <E>(val value: E) : LoadingState<Nothing, E>()
}

@Serializable
object UnitSurrogate