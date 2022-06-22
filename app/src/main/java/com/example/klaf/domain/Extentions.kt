package com.example.klaf.domain

fun <T> MutableList<T>.update(newData: List<T>) {
    this.clear()
    this.addAll(newData)
}