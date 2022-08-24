package com.example.klaf.domain.common

import com.example.klaf.domain.ipa.LetterInfo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun <T> MutableList<T>.update(newData: List<T>) {
    clear()
    addAll(newData)
}

fun CoroutineScope.launchWithExceptionHandler(
    context: CoroutineContext = this.coroutineContext,
    onException: (CoroutineContext, Throwable) -> Unit,
    onCompletion: () -> Unit = {},
    task: suspend CoroutineScope.() -> Unit,
): Job {
    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        onException(coroutineContext, throwable)
    }

    return launch(context = context + exceptionHandler) { task() }.apply {
        invokeOnCompletion { cause: Throwable? ->
            if (cause == null) {
                onCompletion()
            }
        }
    }
}

fun <T, R> Flow<List<T>>.simplifiedItemMap(transform: suspend (T) -> R): Flow<List<R>> {
    return this.map { list: List<T> ->
        list.map { value: T -> transform(value) }
    }
}

fun CharSequence?.generateLetterInfos(): List<LetterInfo> {
    this ?: return emptyList()

    return this.toString()
        .trim()
        .split("")
        .drop(1)
        .dropLast(1)
        .map { letter -> LetterInfo(letter = letter, isChecked = false) }
}

fun <T> List<T>.addIntoNewInstance(newElement: T): List<T> {
    return this.toMutableList().apply { add(element = newElement) }
}

fun Int.isEven(): Boolean = this % 2 == 0

fun Int.isOdd(): Boolean = this % 2 != 0

fun Long.isEven(): Boolean = this % 2 == 0L

fun Long.isOdd(): Boolean = this % 2 != 0L

inline fun Boolean.ifTrue(block: () -> Unit) {
    if (this) block()
}

inline fun Boolean.ifFalse(block: () -> Unit) {
    if (!this) block()
}

fun <T> T.isNotNull(): Boolean = this != null