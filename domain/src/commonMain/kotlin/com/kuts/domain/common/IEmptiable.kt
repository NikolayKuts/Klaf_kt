package com.kuts.domain.common

sealed interface IEmptiable <out T> {

    data object Empty : IEmptiable<Nothing>

    data class Content <V> (val data: V) : IEmptiable<V>
}
