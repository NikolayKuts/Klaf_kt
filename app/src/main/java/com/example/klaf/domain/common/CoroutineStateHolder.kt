package com.example.klaf.domain.common

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class CoroutineStateHolder private constructor() {

    var job: Job = Job()
        private set

    private var coroutineExceptionHolders = mutableListOf<CoroutineExceptionHolder>()
    private var onException: ((CoroutineContext, Throwable) -> Unit)? = null
    private val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        if (onException == null) {
            coroutineExceptionHolders.add(CoroutineExceptionHolder(coroutineContext, throwable))
        } else {
            onException?.invoke(coroutineContext, throwable)
        }
    }

    companion object {

        fun CoroutineScope.launchWithState(
            context: CoroutineContext = this.coroutineContext,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            task: suspend CoroutineScope.() -> Unit,
        ): CoroutineStateHolder {
            val holder = CoroutineStateHolder()
            val job = launch(
                context = context + holder.handler,
                start = start,
            ) {
                task()
            }

            return holder.apply { this.job = job }
        }

        infix fun CoroutineStateHolder.onException(
            block: (CoroutineContext, Throwable) -> Unit
        ): Job {
            this.onException = block

            if (coroutineExceptionHolders.isNotEmpty()) {
                coroutineExceptionHolders.forEach { block(it.coroutineContext, it.throwable) }
                coroutineExceptionHolders.clear()
            }

            return job
        }
    }

    private data class CoroutineExceptionHolder(
        val coroutineContext: CoroutineContext,
        val throwable: Throwable,
    )
}