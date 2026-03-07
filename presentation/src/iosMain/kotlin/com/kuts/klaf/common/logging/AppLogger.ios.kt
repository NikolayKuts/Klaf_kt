package com.kuts.klaf.common.logging

actual object AppLogger {

    actual fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val logMessage = buildString {
            append("E/")
            append(tag)
            append(": ")
            append(message)

            throwable?.let { error ->
                append('\n')
                append(error.stackTraceToString())
            }
        }

        println(logMessage)
    }
}
