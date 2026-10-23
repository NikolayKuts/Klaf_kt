package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import com.kuts.klaf.server.contract.SyncRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

private const val TEST_REQUEST_LABEL = "PUT /api/v1/images"
private val TEST_IMAGE_BYTES = byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10)

class KlafServerRemoteRequestTest {

    @Test
    fun `owned timeout reports operation and keeps original cause`() = runBlocking {
        val failure = assertFailsWith<AccountOperationException> {
            klafServerRequest(30L, TEST_REQUEST_LABEL) { awaitCancellation() }
        }

        assertEquals(AccountFailure.TIMEOUT, failure.failure)
        assertEquals("$TEST_REQUEST_LABEL: TIMEOUT", failure.message)
        assertIs<TimeoutCancellationException>(failure.cause)
        Unit
    }

    @Test
    fun `caller timeout is not translated into request failure`() = runBlocking {
        assertFailsWith<TimeoutCancellationException> {
            withTimeout(30L) {
                klafServerRequest(5_000L, TEST_REQUEST_LABEL) { awaitCancellation() }
            }
        }
        Unit
    }

    @Test
    fun `explicit cancellation propagates and typed account errors retain identity`() = runBlocking {
        val cancelled = CancellationException("caller cancelled")
        val denied = SyncHttpException(403, "ACCOUNT_ACCESS_DENIED")

        val cancellation = assertFailsWith<CancellationException> {
            klafServerRequest(5_000L, TEST_REQUEST_LABEL) { throw cancelled }
        }
        assertEquals(cancelled.message, cancellation.message)
        assertSame(denied, assertFailsWith<SyncHttpException> {
            klafServerRequest(5_000L, TEST_REQUEST_LABEL) { throw denied }
        })
    }

    @Test
    fun `nested authentication timeout receives outer operation without losing failure metadata`() = runBlocking {
        val original = AccountOperationException(
            AccountFailure.TIMEOUT,
            message = "Nested details must not become the request label",
            retryAfterSeconds = 30,
        )

        val failure = assertFailsWith<AccountOperationException> {
            klafServerRequest(5_000L, TEST_REQUEST_LABEL) { throw original }
        }

        assertEquals(AccountFailure.TIMEOUT, failure.failure)
        assertEquals("$TEST_REQUEST_LABEL: TIMEOUT", failure.message)
        assertSame(original, failure.cause)
        assertEquals(30, failure.retryAfterSeconds)
    }

    @Test
    fun `unlabelled request preserves existing timeout message`() = runBlocking {
        val failure = assertFailsWith<AccountOperationException> {
            klafServerRequest(30L) { awaitCancellation() }
        }

        assertEquals("TIMEOUT", failure.message)
    }

    @Test
    fun `sync endpoints identify failing operation without account or device details`() = runBlocking {
        val http = HttpClient(MockEngine { throw IOException("connection lost") })
        try {
            val client = KlafServerSyncRestClient("https://server.test", http)
            val operations: List<Pair<String, suspend () -> Unit>> = listOf(
                "GET /api/v1/sync/bootstrap" to { client.bootstrap("alice@example.test", "private-device") },
                "POST /api/v1/sync" to { client.sync(SyncRequest("alice@example.test", "private-device", 3, 2L, emptyList())) },
                "GET /api/v1/sync/history" to { client.recentHistory("alice@example.test", "private-device") },
                "POST /api/v1/sync/confirm" to { client.confirmAppliedRevision("alice@example.test", "private-device", 2L) },
            )
            for ((label, operation) in operations) {
                val failure = assertFailsWith<AccountOperationException> { operation() }
                assertEquals(AccountFailure.CONNECTION, failure.failure)
                assertEquals("$label: CONNECTION", failure.message)
            }
        } finally {
            http.close()
        }
    }

    @Test
    fun `image endpoints distinguish upload from download without asset identifiers`() = runBlocking {
        val http = HttpClient(MockEngine { throw IOException("connection lost") })
        try {
            val client = KlafServerImageRestClient("https://server.test", http)
            val upload = assertFailsWith<AccountOperationException> {
                client.upload("alice@example.test", "private-device", "private-image", TEST_IMAGE_BYTES)
            }
            val download = assertFailsWith<AccountOperationException> {
                client.download("alice@example.test", "private-device", "private-image")
            }

            assertEquals("PUT /api/v1/images: CONNECTION", upload.message)
            assertEquals("GET /api/v1/images: CONNECTION", download.message)
        } finally {
            http.close()
        }
    }
}
