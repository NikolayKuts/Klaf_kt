package com.kuts.klaf.di

internal data class KlafServerEndpointConfig(
    val host: String,
    val port: Int,
    val isSecure: Boolean = false,
) {

    init {
        require(host.isNotBlank()) { "Klaf Server host is required" }
        require(port in 1..65535) { "Klaf Server port is invalid" }
    }

    fun restBaseUrl(): String = "${if (isSecure) "https" else "http"}://$host:$port"
}
