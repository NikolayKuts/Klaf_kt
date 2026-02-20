package com.kuts.domain.common

import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


class DebouncedMutableStateFlow<T>(
    value: T,
    private val timeoutMillis: Long = 300,
) : MutableStateFlow<T> by MutableStateFlow(value = value) {

    private var job: Job? = null

    fun launchUpdate(
        scope: CoroutineScope,
        timeoutMillis: Long = this.timeoutMillis,
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend (T) -> T
    ) {
        job?.cancel()
        job = scope.launch(context) {
            delay(timeoutMillis)
            value = block(value)
        }
    }

    fun launchUpdateWithState(
        scope: CoroutineScope,
        timeoutMillis: Long = this.timeoutMillis,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend (T) -> T
    ): CoroutineStateHolder {
        job?.cancel()

        return scope.launchWithState(context = context, start = start) {
            delay(timeoutMillis)
            value = block(value)
        }.also {
            job = it.job
        }
    }
}