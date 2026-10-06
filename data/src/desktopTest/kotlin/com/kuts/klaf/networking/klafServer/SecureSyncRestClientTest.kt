package com.kuts.klaf.networking.klafServer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class SecureSyncRestClientTest {

    @Test
    fun `bootstrap sends access proof and retries a server nonce with a new proof`() = runBlocking {
        val proofs = mutableListOf<String>()
        val signer = object : KlafAuthenticatedRequestSigner {
            override suspend fun headers(email: String, method: String, url: String, nonce: String?): AccessProofHeaders {
                assertEquals("alice@example.test", email)
                assertEquals("GET", method)
                assertEquals("https://server.test/api/v1/sync/bootstrap", url)
                return AccessProofHeaders("DPoP access-token", "proof-${proofs.size + 1}-${nonce.orEmpty()}")
            }
        }
        val http = HttpClient(MockEngine { request ->
            assertEquals("DPoP access-token", request.headers["Authorization"])
            proofs += requireNotNull(request.headers["DPoP"])
            if (proofs.size == 1) {
                respond("""{"code":"DPOP_NONCE_REQUIRED"}""", HttpStatusCode.Unauthorized,
                    headersOf("DPoP-Nonce", "server-nonce-123456789"))
            } else {
                respond("""{"revision":7,"decks":[],"cards":[]}""", HttpStatusCode.OK)
            }
        })
        try {
            val result = KlafServerSyncRestClient("https://server.test", http, signer)
                .bootstrap("alice@example.test", "desktop-a")
            assertEquals(7L, result.revision)
            assertEquals(listOf("proof-1-", "proof-2-server-nonce-123456789"), proofs)
        } finally {
            http.close()
        }
    }
}
