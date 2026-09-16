package com.kuts.klaf.networking.klafServer

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import java.util.concurrent.TimeUnit

private const val KLAF_SERVER_WEBSOCKET_PING_INTERVAL_SECONDS = 20L

class KlafServerHttpClientFactory {

    fun create(): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    pingInterval(
                        KLAF_SERVER_WEBSOCKET_PING_INTERVAL_SECONDS,
                        TimeUnit.SECONDS,
                    )
                }
            }
            install(WebSockets)
        }
    }
}
