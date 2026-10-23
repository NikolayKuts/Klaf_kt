package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import com.kuts.klaf.mnemonic.MAX_MNEMONIC_IMAGE_BYTES
import com.kuts.klaf.server.contract.IMAGE_SIZE_HEADER
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

class SecureImageRestClientTest {

    @Test
    fun `image upload tolerates a transfer longer than the metadata request deadline`() = runBlocking {
        val http = HttpClient(MockEngine {
            delay(16_000L)
            respond("", HttpStatusCode.Created)
        })
        try {
            KlafServerImageRestClient("https://server.test", http).upload(
                "alice@example.test", "desktop-a", "valid-image-id", PNG_BYTES,
            )
        } finally {
            http.close()
        }
    }

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
            assertEquals(PNG_BYTES.size.toString(), request.headers[IMAGE_SIZE_HEADER])
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

    @Test
    fun `download accepts complete proxy framed data and legacy content length`() = runBlocking {
        for (header in listOf(IMAGE_SIZE_HEADER, HttpHeaders.ContentLength)) {
            val http = HttpClient(MockEngine {
                respond(ByteReadChannel(PNG_BYTES), HttpStatusCode.OK, headersOf(header, PNG_BYTES.size.toString()))
            })
            try {
                val client = KlafServerImageRestClient("https://server.test", http)
                assertContentEquals(PNG_BYTES, client.download("alice@example.test", "desktop-a", "valid-image-id"))
            } finally {
                http.close()
            }
        }
    }

    @Test
    fun `download rejects a cleanly closed prefix instead of returning incomplete image bytes`() = runBlocking {
        val http = HttpClient(MockEngine {
            respond(ByteReadChannel(PNG_BYTES), HttpStatusCode.OK,
                headersOf(IMAGE_SIZE_HEADER, (PNG_BYTES.size + 15_887).toString()))
        })
        try {
            val failure = assertFailsWith<AccountOperationException> {
                KlafServerImageRestClient("https://server.test", http)
                    .download("alice@example.test", "desktop-a", "valid-image-id")
            }
            assertEquals(AccountFailure.INVALID_RESPONSE, failure.failure)
            assertEquals("GET /api/v1/images: INCOMPLETE_RESPONSE", failure.message)
        } finally {
            http.close()
        }
    }

    @Test
    fun `download rejects absent malformed oversized and contradictory declared lengths`() = runBlocking {
        val invalidHeaders = listOf(
            headersOf(),
            headersOf(IMAGE_SIZE_HEADER, "not-a-number"),
            headersOf(IMAGE_SIZE_HEADER, "-1"),
            headersOf(IMAGE_SIZE_HEADER, "0"),
            headersOf(IMAGE_SIZE_HEADER, (MAX_MNEMONIC_IMAGE_BYTES + 1).toString()),
            headersOf(IMAGE_SIZE_HEADER to listOf("7"), HttpHeaders.ContentLength to listOf("8")),
        )
        for (headers in invalidHeaders) {
            val http = HttpClient(MockEngine { respond(ByteReadChannel(PNG_BYTES), HttpStatusCode.OK, headers) })
            try {
                val failure = assertFailsWith<AccountOperationException> {
                    KlafServerImageRestClient("https://server.test", http)
                        .download("alice@example.test", "desktop-a", "valid-image-id")
                }
                assertEquals(AccountFailure.INVALID_RESPONSE, failure.failure)
            } finally {
                http.close()
            }
        }
    }

    @Test
    fun `download propagates channel failure without returning buffered image prefix`() = runBlocking {
        val http = HttpClient(MockEngine {
            val channel = ByteReadChannel(PNG_BYTES)
            channel.cancel(IOException("interrupted test stream"))
            respond(channel, HttpStatusCode.OK, headersOf(IMAGE_SIZE_HEADER, PNG_BYTES.size.toString()))
        })
        try {
            val failure = assertFailsWith<AccountOperationException> {
                KlafServerImageRestClient("https://server.test", http)
                    .download("alice@example.test", "desktop-a", "valid-image-id")
            }
            assertEquals(AccountFailure.CONNECTION, failure.failure)
        } finally {
            http.close()
        }
    }

    private companion object {
        val PNG_BYTES = byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10)
    }
}
