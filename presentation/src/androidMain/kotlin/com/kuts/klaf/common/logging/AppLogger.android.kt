package com.kuts.klaf.common.logging

import com.lib.lokdroid.core.logE

actual object AppLogger {

    actual fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val logMessage = buildString {
            append("[$tag] ")
            append(message)

            throwable?.let { error ->
                append('\n')
                append(error.stackTraceToString())
            }
        }

        logE(logMessage)
    }
}
