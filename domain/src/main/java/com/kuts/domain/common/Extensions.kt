package com.kuts.domain.common

import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.repositories.CrashlyticsRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar
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

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
fun <Input, Output> Flow<Input>.flatDebounce(
    timeoutMillis: Long = 300,
    execute: suspend (Input) -> Flow<Output>,
    onEach: suspend (Input, Output) -> Unit
): Flow<Input> = this.debounce(timeoutMillis)
    .distinctUntilChanged()
    .flatMapLatest { input ->
        execute(input).map { value -> input to value }
    }
    .onEach { inputOutputPair -> onEach(inputOutputPair.first, inputOutputPair.second) }
    .mapLatest { it.first }

fun Float.invertedCoerceIn(min: Float, max: Float): Float {
    return max - coerceIn(min, max)
}