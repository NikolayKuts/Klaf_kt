package com.kuts.klaf.networking.codexApp

import com.kuts.klaf.SecretConstants
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header

class AndroidCodexAppHttpClientFactory : ICodexAppHttpClientFactory {

    companion object {

        private const val REQUEST_TIMEOUT = 180_000L
        private const val CONNECT_TIMEOUT = 20_000L
        private const val SOCKET_TIMEOUT = 180_000L
    }

    override fun create(): HttpClient {
        return HttpClient(OkHttp) {
            install(WebSockets)
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT
                connectTimeoutMillis = CONNECT_TIMEOUT
                socketTimeoutMillis = SOCKET_TIMEOUT
            }
            install(DefaultRequest) {
                header("CF-Access-Client-Id", SecretConstants.CodexApp.CLIENT_ID)
                header("CF-Access-Client-Secret", SecretConstants.CodexApp.CLIENT_SECRET)
            }
        }
    }
}
