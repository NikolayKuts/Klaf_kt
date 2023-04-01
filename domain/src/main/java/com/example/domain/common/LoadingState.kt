package com.example.domain.common

sealed class LoadingState<out T> {

    class Success<T>(val data: T) : LoadingState<T>()

    object Loading : LoadingState<Nothing>()

    class Error(val value: LoadingError) : LoadingState<Nothing>()
}

interface LoadingError