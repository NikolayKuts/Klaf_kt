package com.example.klaf.presentation.common

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect

val EditText.textAsString: String get() = this.text.toString()

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