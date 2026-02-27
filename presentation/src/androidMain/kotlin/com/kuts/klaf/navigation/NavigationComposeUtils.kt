package com.kuts.klaf.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

internal const val AUTHENTICATION_RESULT_KEY = "authentication_result_key"

@Composable
internal fun ObserveAudioLifecycle(
    onCreate: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDestroy: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(key1 = lifecycleOwner) {
        onCreate()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_STOP -> onStop()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onDestroy()
        }
    }
}

@Composable
internal fun <T> CollectFlowWithLifecycle(
    flow: Flow<T>,
    onEach: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(key1 = flow, key2 = lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
            flow.collect { value -> onEach(value) }
        }
    }
}
