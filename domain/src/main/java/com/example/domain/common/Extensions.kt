package com.example.domain.common

import com.example.domain.ipa.LetterInfo
import com.example.domain.repositories.CrashlyticsRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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

fun Int.toFloatPercents(): Float = this / 100.0F

fun <T> List<T>.updatedAt(index: Int, newValue: T): List<T> {
    return this.toMutableList().apply { this[index] = newValue }
}

inline fun <R> List<R>.updatedAt(index: Int, block: (oldValue: R) -> R): List<R> {
    return this.toMutableList().apply { this[index] = block(this[index]) }
}

fun getCurrentDateAsLong(): Long {
    return Calendar.getInstance().time.time
}

sealed class Conditionable<V> {

    abstract val value: V?

    abstract class ValuableCondition<V>(override val value: V) : Conditionable<V>()

    class ElseCondition<V> : Conditionable<V>() {

        override val value: V? = null
    }
}

inline infix fun <T, R> T?.ifNotNull(
    block: (T) -> R,
): ConditionableWithReceiver<T, R> = if (this != null) {
    ConditionableWithReceiver.ValuableCondition(value = block(this), receiver = this)
} else {
    ConditionableWithReceiver.ElseCondition()
}

inline infix fun <T, R> T?.ifNull(
    block: () -> R,
): ConditionableWithReversedReceiver<T, R> = if (this == null) {
    ConditionableWithReversedReceiver.ValuableCondition<T, R>(value = block())
} else {
    ConditionableWithReversedReceiver.ElseCondition(receiver = this)
}

fun String.skipOnNewLineCharacter(): String = if ("\n" in this) {
    replace(oldValue = "\n", newValue = "")
} else {
    this
}

fun <T> Flow<T>.catchWithCrashlyticsReport(
    crashlytics: CrashlyticsRepository,
    action: suspend FlowCollector<T>.(Throwable) -> Unit,
): Flow<T> = this.catch {
    crashlytics.report(exception = it)
    action(it)
}

fun <T> Flow<T>.launchIn(
    scope: CoroutineScope,
    context: CoroutineContext = EmptyCoroutineContext,
): Job = scope.launch(context = context) {
    collect() // tail-call
}