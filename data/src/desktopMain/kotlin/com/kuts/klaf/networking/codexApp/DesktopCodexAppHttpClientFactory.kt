package com.kuts.klaf.networking.codexApp

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets

class DesktopCodexAppHttpClientFactory : ICodexAppHttpClientFactory {

    companion object {

        private const val REQUEST_TIMEOUT = 90_000L
        private const val CONNECT_TIMEOUT = 20_000L
        private const val SOCKET_TIMEOUT = 90_000L
    }

    override fun create(): HttpClient {
        return HttpClient(CIO) {
            install(WebSockets)
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT
                connectTimeoutMillis = CONNECT_TIMEOUT
                socketTimeoutMillis = SOCKET_TIMEOUT
            }
        }
    }
}
