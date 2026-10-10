package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountOperationException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KlafServerWordAutocompleteRepositoryTest {
    @Test
    fun `signed in prefix lookup maps server words`() = runBlocking {
        var requests = 0
        val http = HttpClient(MockEngine { request ->
            requests++
            assertEquals("/api/v1/autocomplete", request.url.encodedPath)
            assertEquals("ap", request.url.parameters["prefix"])
            respond(
                content = """{"words":["apple","application"]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        try {
            val repository = KlafServerWordAutocompleteRepository(
                baseUrl = "https://example.test",
                httpClient = http,
                signer = null,
                selectedAccountEmail = flowOf("user@example.test"),
            )
            assertEquals(listOf("apple", "application"), repository.fetchAutocomplete(" AP ").map { it.word() })
            assertEquals(1, requests)
        } finally {
            http.close()
        }
    }

    @Test
    fun `guest and blank prefixes never call server`() = runBlocking {
        val http = HttpClient(MockEngine { error("Unexpected autocomplete request") })
        try {
            val guest = KlafServerWordAutocompleteRepository(
                baseUrl = "https://example.test",
                httpClient = http,
                signer = null,
                selectedAccountEmail = flowOf(null),
            )
            val signedIn = KlafServerWordAutocompleteRepository(
                baseUrl = "https://example.test",
                httpClient = http,
                signer = null,
                selectedAccountEmail = flowOf("user@example.test"),
            )
            assertEquals(emptyList(), guest.fetchAutocomplete("apple"))
            assertEquals(emptyList(), signedIn.fetchAutocomplete("  "))
        } finally {
            http.close()
        }
    }

    @Test
    fun `server failure is reported rather than treated as no matches`() {
        runBlocking {
            val http = HttpClient(MockEngine {
                respond(
                    content = """{"code":"INVALID_REQUEST"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            })
            try {
                val repository = KlafServerWordAutocompleteRepository(
                    baseUrl = "https://example.test",
                    httpClient = http,
                    signer = null,
                    selectedAccountEmail = flowOf("user@example.test"),
                )
                assertFailsWith<AccountOperationException> { repository.fetchAutocomplete("ap") }
            } finally {
                http.close()
            }
        }
    }

    @Test
    fun `oversized response is rejected`() {
        runBlocking {
            val responseBody = """{"words":[${List(11) { "\"apple\"" }.joinToString(",")}]}"""
            val http = HttpClient(MockEngine {
                respond(
                    content = responseBody,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            })
            try {
                val repository = KlafServerWordAutocompleteRepository(
                    baseUrl = "https://example.test",
                    httpClient = http,
                    signer = null,
                    selectedAccountEmail = flowOf("user@example.test"),
                )
                assertFailsWith<IllegalStateException> { repository.fetchAutocomplete("ap") }
            } finally {
                http.close()
            }
        }
    }
}
