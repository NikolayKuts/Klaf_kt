package com.example.domain.common

sealed class LoadingResult {

    class Success <T> (val data: T) : LoadingResult()

    class Loading : LoadingResult()

    class Error(value: LoadingError) : LoadingResult()
}

sealed interface LoadingError