package com.kuts.klaf.networking.wordInsights

internal expect object WordInsightsRequestLogger {

    fun d(tag: String, message: String)

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}
