package com.kuts.klaf.common.logging

expect object AppLogger {

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}
