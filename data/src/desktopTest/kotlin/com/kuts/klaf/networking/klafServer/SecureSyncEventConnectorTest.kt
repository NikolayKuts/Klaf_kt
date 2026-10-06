package com.kuts.klaf.networking.klafServer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class SecureSyncEventConnectorTest {

    @Test
    fun `preflight obtains server nonce with account-bound access proof`() = runBlocking {
        val signer = object : KlafAuthenticatedRequestSigner {
            override suspend fun headers(email: String, method: String, url: String, nonce: String?): AccessProofHeaders {
                assertEquals("alice@example.test", email)
                assertEquals("GET", method)
                assertEquals("https://server.test/sync-events?email=alice%40example.test&deviceId=desktop-a", url)
                assertEquals(null, nonce)
                return AccessProofHeaders("DPoP access-token", "fresh-preflight-proof")
            }
        }
        val http = HttpClient(MockEngine { request ->
            assertEquals("DPoP access-token", request.headers["Authorization"])
            assertEquals("fresh-preflight-proof", request.headers["DPoP"])
            respond("""{"code":"DPOP_NONCE_REQUIRED"}""", HttpStatusCode.Unauthorized,
                headersOf("DPoP-Nonce", "server-nonce-123456789"))
        })
        try {
            val connector = KlafServerSyncEventConnector("https://server.test", http, signer)
            assertEquals("server-nonce-123456789",
                connector.requestConnectionNonce("alice@example.test", "desktop-a"))
        } finally {
            http.close()
        }
    }
}
