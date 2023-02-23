package com.example.domain.common

import com.example.domain.common.DeckRepetitionSuccessMark.FAILURE
import com.example.domain.common.DeckRepetitionSuccessMark.SUCCESS
import com.example.domain.entities.Deck
import com.example.domain.entities.DeckRepetitionInfo
import com.example.domain.ipa.LetterInfo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext

val DeckRepetitionInfo.isCurrentDurationUnassigned: Boolean
    get() = currentDuration == UNASSIGNED_LONG_VALUE

val Deck.lastIterationSuccessMark: DeckRepetitionSuccessMark
    get() = if (this.isLastIterationSucceeded) SUCCESS else FAILURE

fun <T> MutableList<T>.update(newData: List<T>) {
    clear()
    addAll(newData)
}

fun CoroutineScope.launchWithExceptionHandler(
    context: CoroutineContext = this.coroutineContext,
    onException: (CoroutineContext, Throwable) -> Unit,
    task: suspend CoroutineScope.() -> Unit,
): Job {
    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        onException(coroutineContext, throwable)
    }

    return launch(context = context + exceptionHandler) { task() }
}

fun <T, R> Flow<List<T>>.simplifiedItemMap(transform: suspend (T) -> R): Flow<List<R>> {
    return this.map { list: List<T> ->
        list.map { value: T -> transform(value) }
    }
}

fun <T> Flow<List<T>>.simplifiedItemFilter(predicate: (T) -> Boolean): Flow<List<T>> {
    return this.onEach { list: List<T> ->
        list.filter(predicate = predicate)
    }
}

fun <T> Flow<List<T>>.simplifiedItemFilterNot(predicate: (T) -> Boolean): Flow<List<T>> {
    return this.map { list: List<T> ->
        list.filterNot(predicate = predicate)
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

inline fun <R> Boolean.ifTrue(block: () -> R): R? {
    return if (this) block() else null
}

inline fun Boolean.ifFalse(block: () -> Unit) {
    if (!this) block()
}

fun <T> T.isNotNull(): Boolean = this != null

inline fun <T> T.ifNull(block: () -> Unit) {
    if (this == null) block()
}

inline fun <T> T.ifNotNull(block: (T) -> Unit) {
    if (this != null) {
        block(this)
    }
}

fun Int.toFloatPercents(): Float = this / 100.0F

fun <T> List<T>.updatedAt(index: Int, newValue: T): List<T> {
    return this.toMutableList().apply { this[index] = newValue }
}

fun getCurrentDateAsLong(): Long {
    return Calendar.getInstance().time.time
}