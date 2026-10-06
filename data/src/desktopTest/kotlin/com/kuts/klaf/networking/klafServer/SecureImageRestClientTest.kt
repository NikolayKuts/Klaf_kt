package com.kuts.klaf.networking.klafServer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class SecureImageRestClientTest {

    @Test
    fun `image upload uses account token proof and retries nonce challenge`() = runBlocking {
        val seen = mutableListOf<String>()
        val signer = object : KlafAuthenticatedRequestSigner {
            override suspend fun headers(email: String, method: String, url: String, nonce: String?): AccessProofHeaders {
                assertEquals("alice@example.test", email)
                assertEquals("PUT", method)
                assertEquals("https://server.test/api/v1/images", url)
                return AccessProofHeaders("DPoP access-token", "image-proof-${seen.size + 1}-${nonce.orEmpty()}")
            }
        }
        val http = HttpClient(MockEngine { request ->
            assertEquals("DPoP access-token", request.headers["Authorization"])
            seen += requireNotNull(request.headers["DPoP"])
            if (seen.size == 1) {
                respond("""{"code":"DPOP_NONCE_REQUIRED"}""", HttpStatusCode.Unauthorized,
                    headersOf("DPoP-Nonce", "image-server-nonce-123456789"))
            } else {
                respond("", HttpStatusCode.Created)
            }
        })
        try {
            KlafServerImageRestClient("https://server.test", http, signer).upload(
                "alice@example.test", "desktop-a", "valid-image-id", PNG_BYTES)
            assertEquals(listOf("image-proof-1-", "image-proof-2-image-server-nonce-123456789"), seen)
        } finally {
            http.close()
        }
    }

    private companion object {
        val PNG_BYTES = byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10)
    }
}
