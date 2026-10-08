package com.kuts.klaf.networking.klafServer

private val SERVER_HOST_PATTERN = Regex("[A-Za-z0-9.-]+")

fun klafServerHttpOrigin(
    host: String,
    port: Int,
    isSecure: Boolean,
): String {
    require(host.matches(SERVER_HOST_PATTERN)) { "Klaf Server host is invalid" }
    require(port in 1..65535) { "Klaf Server port is invalid" }

    val scheme = if (isSecure) "https" else "http"
    val defaultPort = if (isSecure) 443 else 80
    return "$scheme://$host${if (port == defaultPort) "" else ":$port"}"
}

internal fun klafServerWebSocketBaseUrl(origin: String): String = when {
    origin.startsWith("https://") -> "wss://${origin.removePrefix("https://").trimEnd('/')}"
    origin.startsWith("http://") -> "ws://${origin.removePrefix("http://").trimEnd('/')}"
    else -> throw IllegalArgumentException("Klaf Server origin must use HTTP or HTTPS")
}
