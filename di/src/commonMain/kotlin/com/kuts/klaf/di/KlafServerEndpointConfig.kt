package com.kuts.klaf.di

import com.kuts.klaf.networking.klafServer.klafServerHttpOrigin

internal data class KlafServerEndpointConfig(
    val host: String,
    val port: Int,
    val isSecure: Boolean = false,
) {

    init {
        require(host.isNotBlank()) { "Klaf Server host is required" }
        require(port in 1..65535) { "Klaf Server port is invalid" }
    }

    fun restBaseUrl(): String = klafServerHttpOrigin(host, port, isSecure)
}
