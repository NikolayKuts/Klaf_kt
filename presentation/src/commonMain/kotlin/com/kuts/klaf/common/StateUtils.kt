package com.kuts.klaf.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun <T> rememberAsMutableStateOf(value: T): MutableState<T> {
    return remember { mutableStateOf(value = value) }
}
