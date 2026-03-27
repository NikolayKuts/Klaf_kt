package com.kuts.klaf.networking.wordInsights

import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE

internal actual object WordInsightsRequestLogger {

    actual fun d(tag: String, message: String) {
        logD("[$tag] $message")
    }

    actual fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val payload = buildString {
            append("[$tag] ")
            append(message)

            throwable?.let { error ->
                append('\n')
                append(error.stackTraceToString())
            }
        }

        logE(payload)
    }
}
