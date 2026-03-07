package com.kuts.klaf.common

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> SavedStateHandle.create(
    key: String,
    default: T,
    encode: ((T) -> String)? = null,
    decode: ((String) -> T)? = null,
): ReadWriteProperty<Any, T> = object : ReadWriteProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        val restoredValue = if (decode != null) {
            this@create.get<String>(key = key)
                ?.let { value -> runCatching { decode(value) }.getOrNull() }
        } else {
            null
        }

        return restoredValue ?: this@create.get(key = key) ?: default
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this@create[key] = encode?.invoke(value) ?: value
    }
}

fun <T> SavedStateHandle.mutStateFlow(
    key: String,
    default: T,
    encode: ((T) -> String)? = null,
    decode: ((String) -> T)? = null,
): ReadOnlyProperty<Any?, MutableStateFlow<T>> {
    val initial = if (decode != null) {
        this.get<String>(key = key)
            ?.let { value -> runCatching { decode(value) }.getOrNull() }
            ?: default
    } else {
        this.get(key = key) ?: default
    }

    val inner = MutableStateFlow(initial)

    return object : ReadOnlyProperty<Any?, MutableStateFlow<T>>, MutableStateFlow<T> by inner {

        override var value: T
            get() = inner.value
            set(v) {
                inner.value = v
                this@mutStateFlow[key] = encode?.invoke(v) ?: v
            }

        override fun getValue(thisRef: Any?, property: KProperty<*>): MutableStateFlow<T> = this

        override fun tryEmit(value: T): Boolean {
            val result = inner.tryEmit(value)
            if (result) {
                this@mutStateFlow[key] = encode?.invoke(value) ?: value
            }
            return result
        }

        override suspend fun emit(value: T) {
            this@mutStateFlow[key] = encode?.invoke(value) ?: value
            inner.emit(value)
        }

        override fun compareAndSet(expect: T, update: T): Boolean {
            val result = inner.compareAndSet(expect, update)
            if (result) {
                this@mutStateFlow[key] = encode?.invoke(update) ?: update
            }
            return result
        }
    }
}

fun <T> SavedStateHandle.mutSharedFlow(
    key: String,
    replay: Int = 0,
    extraBufferCapacity: Int = 0,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    encode: ((T) -> String)? = null,
    decode: ((String) -> T)? = null,
): ReadOnlyProperty<Any?, MutableSharedFlow<T>> {
    val handle = this
    val inner = MutableSharedFlow<T>(replay, extraBufferCapacity, onBufferOverflow)
        .apply {
            val restoredValue: T? = if (decode != null) {
                handle.get<String>(key = key)
                    ?.let { value -> runCatching { decode(value) }.getOrNull() }
            } else {
                handle.get(key)
            }
            restoredValue?.let { tryEmit(it) }
        }

    return object : ReadOnlyProperty<Any?, MutableSharedFlow<T>>, MutableSharedFlow<T> by inner {

        override fun getValue(thisRef: Any?, property: KProperty<*>): MutableSharedFlow<T> = this

        override fun tryEmit(value: T): Boolean {
            val result = inner.tryEmit(value)
            if (result) {
                handle[key] = encode?.invoke(value) ?: value
            }

            return result
        }

        override suspend fun emit(value: T) {
            handle[key] = encode?.invoke(value) ?: value
            inner.emit(value)
        }
    }
}
