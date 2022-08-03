package com.example.klaf.presentation.common

import android.content.Context
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow


private const val TIME_FORMAT_TEMPLATE = "%02d:%02d"
private const val SECOND_QUANTITY_IN_MINUTE = 60

val EditText.textAsString: String get() = this.text.toString()

val Long.timeAsString: String
    get() {
        val seconds = this % SECOND_QUANTITY_IN_MINUTE
        val minutes = this / SECOND_QUANTITY_IN_MINUTE

        return TIME_FORMAT_TEMPLATE.format(minutes, seconds)
    }

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showToast(@StringRes messageId: Int, duration: Int = Toast.LENGTH_SHORT) {
    showToast(message = getString(messageId), duration = duration)
}

inline fun <T> Flow<T>.collectWhenStarted(
    lifecycleScope: LifecycleCoroutineScope,
    crossinline onEach: (T) -> Unit,
): Job {
    return lifecycleScope.launchWhenStarted { this@collectWhenStarted.collect { onEach(it) } }
}

fun MutableSharedFlow<EventMessage>.tryEmit(@StringRes messageId: Int) {
    tryEmit(value = EventMessage(resId = messageId))
}

fun TextView.applyTextColor(@ColorRes colorId: Int) {
    setTextColor(ContextCompat.getColor(context, colorId))
}

fun log(
    message: String,
    tag: String = "app_log",
    pointerMessage: String = "",
    pointer: String =
        if (pointerMessage.isEmpty()) "***********" else "****** $pointerMessage ******",
) {
    Log.i(tag, "$pointer $message")
}

@Composable
fun <T> rememberAsMutableStateOf(value: T): MutableState<T> {
    return remember { mutableStateOf(value = value) }
}