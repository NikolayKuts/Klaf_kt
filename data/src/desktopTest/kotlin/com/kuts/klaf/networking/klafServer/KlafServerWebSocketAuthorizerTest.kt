package com.kuts.klaf.networking.klafServer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.managers.AccountFailure
import kotlinx.coroutines.runBlocking

class KlafServerWebSocketAuthorizerTest {

    @Test
    fun `revoked device is a terminal auth failure instead of an endless reconnect`() = runBlocking {
        val signer = object : KlafAuthenticatedRequestSigner {
            override suspend fun headers(email: String, method: String, url: String, nonce: String?) =
                AccessProofHeaders("DPoP access-token", "proof")
        }
        val http = HttpClient(MockEngine {
            respond("""{"code":"AUTHENTICATION_REQUIRED"}""", HttpStatusCode.Unauthorized)
        })
        try {
            val failure = assertFailsWith<AccountOperationException> {
                KlafServerWebSocketAuthorizer("https://server.test", http, signer)
                    .authorizationHeaders("alice@example.test")
            }
            assertEquals(AccountFailure.SIGN_IN_REQUIRED, failure.failure)
        } finally {
            http.close()
        }
        Unit
    }

    @Test
    fun `obtains nonce and signs websocket upgrade for selected account`() = runBlocking {
        val calls = mutableListOf<String?>()
        val signer = object : KlafAuthenticatedRequestSigner {
            override suspend fun headers(email: String, method: String, url: String, nonce: String?): AccessProofHeaders {
                assertEquals("alice@example.test", email)
                assertEquals("GET", method)
                assertEquals("https://server.test/ws", url)
                calls += nonce
                return AccessProofHeaders("DPoP access-token", "proof-${nonce ?: "initial"}")
            }
        }
        val http = HttpClient(MockEngine { request ->
            assertEquals("DPoP access-token", request.headers["Authorization"])
            assertEquals("proof-initial", request.headers["DPoP"])
            respond("""{"code":"DPOP_NONCE_REQUIRED"}""", HttpStatusCode.Unauthorized,
                headersOf("DPoP-Nonce", "server-nonce-123456789"))
        })
        try {
            val result = KlafServerWebSocketAuthorizer("https://server.test", http, signer)
                .authorizationHeaders("alice@example.test")
            assertEquals(AccessProofHeaders("DPoP access-token", "proof-server-nonce-123456789"), result)
            assertEquals(listOf(null, "server-nonce-123456789"), calls)
        } finally {
            http.close()
        }
    }
}
