package com.example.domain.common

sealed interface Emptiable <T> {

    class Empty <V> : Emptiable<V>

    data class Content <V> (val data: V) : Emptiable<V>
}
