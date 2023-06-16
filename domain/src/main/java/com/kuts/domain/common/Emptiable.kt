package com.kuts.domain.common

sealed interface Emptiable <out T> {

    object Empty : Emptiable<Nothing>

    data class Content <V> (val data: V) : Emptiable<V>
}
