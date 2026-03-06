package com.kuts.klaf.common

import android.os.Bundle
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
    encode: ((T) -> Bundle)? = null,
    decode: ((Bundle) -> T)? = null,
): ReadWriteProperty<Any, T> = object : ReadWriteProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return decode?.invoke(this@create[key] ?: Bundle()) ?: this@create[key] ?: default
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this@create[key] = encode?.invoke(value) ?: value
    }
}

fun <T> SavedStateHandle.mutStateFlow(
    key: String,
    default: T,
    encode: ((T) -> Bundle)? = null,
    decode: ((Bundle) -> T)? = null,
): ReadOnlyProperty<Any?, MutableStateFlow<T>> {
    val initial = decode?.invoke(this[key] ?: Bundle()) ?: this[key] ?: default
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

fun <T> SavedStateHandle.mutList(
    key: String,
    default: MutableList<T>,
    encode: ((List<T>) -> Bundle)? = null,
    decode: ((Bundle) -> List<T>)? = null,
): ReadOnlyProperty<Any?, MutableList<T>> {
    val inner: MutableList<T> = decode?.invoke(this@mutList[key] ?: Bundle())
        ?.toMutableList()
        ?: this@mutList[key]
        ?: default.asObservable { updatedList ->
            this@mutList[key] = encode?.invoke(updatedList) ?: updatedList
        }

    return object : ReadOnlyProperty<Any?, MutableList<T>>, MutableList<T> by inner {

        override fun getValue(thisRef: Any?, property: KProperty<*>): MutableList<T> = this
    }
}

fun <T> SavedStateHandle.mutSharedFlow(
    key: String,
    replay: Int = 0,
    extraBufferCapacity: Int = 0,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    encode: ((T) -> Bundle)? = null,
    decode: ((Bundle) -> T)? = null,
): ReadOnlyProperty<Any?, MutableSharedFlow<T>> {
    val handle = this
    val inner = MutableSharedFlow<T>(replay, extraBufferCapacity, onBufferOverflow)
        .apply {
            val restoredValue: T? = if (decode != null) {
                val storedValue = handle.get<Any?>(key)
                val storedBundle = storedValue as? Bundle ?: Bundle()
                decode.invoke(storedBundle)
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

fun <T> MutableList<T>.asObservable(
    onChange: (updatedList: List<T>) -> Unit
): MutableList<T> = object : MutableList<T> by this {
    val original = this@asObservable

    override fun add(element: T): Boolean =
        original.add(element).also { if (it) onChange(this) }

    override fun add(index: Int, element: T) =
        original.add(index, element).also { onChange(this) }

    override fun addAll(elements: Collection<T>): Boolean =
        original.addAll(elements).also { if (it) onChange(this) }

    override fun addAll(index: Int, elements: Collection<T>): Boolean =
        original.addAll(index, elements).also { if (it) onChange(this) }

    override fun remove(element: T): Boolean =
        original.remove(element).also { if (it) onChange(this) }

    override fun removeAt(index: Int): T = original.removeAt(index).also { onChange(this) }
    override fun removeAll(elements: Collection<T>): Boolean =
        original.removeAll(elements).also { if (it) onChange(this) }

    override fun retainAll(elements: Collection<T>): Boolean =
        original.retainAll(elements).also { if (it) onChange(this) }

    override fun clear() = original.clear().also { onChange(this) }
    override fun set(index: Int, element: T): T =
        original.set(index, element).also { onChange(this) }

    override fun iterator(): MutableIterator<T> {
        val originalIterator = original.iterator()

        return object : MutableIterator<T> by originalIterator {
            override fun remove() {
                originalIterator.remove()
                onChange(original)
            }
        }
    }

    override fun listIterator(): MutableListIterator<T> {
        val it = original.listIterator()
        return object : MutableListIterator<T> by it {
            override fun remove() {
                it.remove()
                onChange(original)
            }

            override fun set(element: T) {
                it.set(element)
                onChange(original)
            }

            override fun add(element: T) {
                it.add(element)
                onChange(original)
            }
        }
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        val it = original.listIterator(index)
        return object : MutableListIterator<T> by it {
            override fun remove() {
                it.remove()
                onChange(original)
            }

            override fun set(element: T) {
                it.set(element)
                onChange(original)
            }

            override fun add(element: T) {
                it.add(element)
                onChange(original)
            }
        }
    }
}
