package com.kuts.domain.common

sealed class LoadingState<out T> {

    object Non : LoadingState<Nothing>()

    class Success<T>(val data: T) : LoadingState<T>()

    object Loading : LoadingState<Nothing>()

    class Error(val value: LoadingError) : LoadingState<Nothing>()
}

interface LoadingError