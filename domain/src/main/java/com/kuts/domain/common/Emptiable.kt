package com.kuts.domain.common

sealed interface Emptiable <out T> {

    class Empty <V> : Emptiable<V>

    data class Content <V> (val data: V) : Emptiable<V>
}
