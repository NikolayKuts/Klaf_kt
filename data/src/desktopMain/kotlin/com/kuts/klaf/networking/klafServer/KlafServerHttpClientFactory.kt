package com.kuts.klaf.networking.klafServer

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets

class KlafServerHttpClientFactory {

    fun create(): HttpClient {
        return HttpClient(CIO) {
            install(WebSockets)
        }
    }
}
