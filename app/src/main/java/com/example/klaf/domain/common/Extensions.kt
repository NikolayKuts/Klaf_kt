package com.example.klaf.domain.common

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun <T> MutableList<T>.update(newData: List<T>) {
    this.clear()
    this.addAll(newData)
}

fun CoroutineScope.launchWithExceptionHandler(
    onException: (CoroutineContext, Throwable) -> Unit,
    onCompletion: () -> Unit = {},
    task: suspend CoroutineScope.() -> Unit
) {
    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        onException(coroutineContext, throwable)
    }

    launch(context = exceptionHandler) { task() }
        .invokeOnCompletion { cause: Throwable? ->
            if (cause == null) {
                onCompletion()
            }
        }

}