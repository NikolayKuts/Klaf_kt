package com.kuts.klaf.presentation.common

import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch


private const val TIME_FORMAT_TEMPLATE = "%02d:%02d"
private const val SECOND_QUANTITY_IN_MINUTE = 60

val EditText.textAsString: String get() = this.text.toString()

val Long.timeAsString: String
    get() {
        val seconds = this % SECOND_QUANTITY_IN_MINUTE
        val minutes = this / SECOND_QUANTITY_IN_MINUTE

        return TIME_FORMAT_TEMPLATE.format(minutes, seconds)
    }

inline fun <T> Flow<T>.collectWhenStarted(
    lifecycleOwner: LifecycleOwner,
    crossinline onEach: (T) -> Unit,
): Job = lifecycleOwner.lifecycleScope.launch {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        collect { element -> onEach(element) }
    }
}

inline fun <T> Flow<T>.collectWhenResumed(
    lifecycleOwner: LifecycleOwner,
    crossinline onEach: (T) -> Unit,
): Job {
    return lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            collect { element -> onEach(element) }
        }
    }
}

fun MutableSharedFlow<EventMessage>.tryEmit(@StringRes messageId: Int) {
    tryEmit(value = EventMessage(resId = messageId))
}

suspend fun MutableSharedFlow<EventMessage>.emit(@StringRes messageId: Int) {
    emit(value = EventMessage(resId = messageId))
}

fun TextView.applyTextColor(@ColorRes colorId: Int) {
    setTextColor(ContextCompat.getColor(context, colorId))
}

fun <T> log(
    message: T,
    pointerMessage: String = "",
    tag: String = "app_log",
    pointer: String =
        if (pointerMessage.isEmpty()) "------------" else "-------- $pointerMessage ------->",
) {
    Log.i(tag, "$pointer $message")
}

@Composable
fun <T> rememberAsMutableStateOf(value: T): MutableState<T> {
    return remember { mutableStateOf(value = value) }
}

fun MutableSharedFlow<EventMessage>.tryEmitAsNegative(
    @StringRes resId: Int,
    vararg args: Any = emptyArray(),
    duration: EventMessage.Duration = EventMessage.Duration.Long,
) {
    this.tryEmit(
        value = EventMessage(
            resId = resId,
            args = args,
            type = EventMessage.Type.Negative,
            duration = duration
        ))
}

fun MutableSharedFlow<EventMessage>.tryEmitAsPositive(
    @StringRes resId: Int,
    duration: EventMessage.Duration = EventMessage.Duration.Medium,
) {
    this.tryEmit(
        value = EventMessage(resId = resId, type = EventMessage.Type.Positive, duration = duration))
}

fun MutableSharedFlow<EventMessage>.tryEmitAsNeutral(
    @StringRes resId: Int,
    duration: EventMessage.Duration = EventMessage.Duration.Medium,
) {
    tryEmit(
        value = EventMessage(resId = resId, type = EventMessage.Type.Neutral, duration = duration)
    )
}
