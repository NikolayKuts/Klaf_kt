package com.kuts.klaf.networking.wordInsights

internal actual object WordInsightsRequestLogger {

    actual fun d(tag: String, message: String) {
        println("D/$tag: $message")
    }

    actual fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val payload = buildString {
            append("E/")
            append(tag)
            append(": ")
            append(message)

            throwable?.let { error ->
                append('\n')
                append(error.stackTraceToString())
            }
        }

        println(payload)
    }
}
