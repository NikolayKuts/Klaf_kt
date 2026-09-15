package com.kuts.klaf.networking.klafServer

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets

class KlafServerHttpClientFactory {

    fun create(): HttpClient {
        return HttpClient(OkHttp) {
            install(WebSockets)
        }
    }
}
