package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SecureAccountRestClientTest {

    @Test
    fun `password reset sends token and new password without creating a session`() = runBlocking {
        var body = ""
        val http = HttpClient(MockEngine { request ->
            body = (request.body as TextContent).text
            respond("", HttpStatusCode.NoContent)
        })
        try {
            val client = SecureAccountRestClient("https://example.test", http, testProofFactory())
            client.resetPassword("alice@example.test", "reset-token", "replacement-passphrase")
            val fields = Json.parseToJsonElement(body).jsonObject
            assertEquals("alice@example.test", fields["email"]?.jsonPrimitive?.content)
            assertEquals("reset-token", fields["token"]?.jsonPrimitive?.content)
            assertEquals("replacement-passphrase", fields["password"]?.jsonPrimitive?.content)
        } finally {
            http.close()
        }
    }

    @Test
    fun `source admission response maps to throttled retry`() = runBlocking {
        val http = HttpClient(MockEngine {
            respond("""{"code":"AUTH_RATE_LIMITED"}""", HttpStatusCode.TooManyRequests,
                headersOf("Retry-After", "60"))
        })
        try {
            val client = SecureAccountRestClient("https://example.test", http, testProofFactory())
            val failure = assertFailsWith<AccountHttpException> {
                client.signIn("alice@example.test", "long-secret-phrase",
                    AccountDevice("desktop", "Desktop", "DESKTOP"))
            }
            assertEquals(AccountFailure.THROTTLED, failure.failure)
            assertEquals(60, failure.retryAfterSeconds)
        } finally {
            http.close()
        }
    }

    @Test
    fun `sign in throttle preserves server retry delay for actionable UI`() = runBlocking {
        val http = HttpClient(MockEngine {
            respond("""{"code":"AUTH_THROTTLED"}""", HttpStatusCode.TooManyRequests,
                headersOf("Retry-After", "30"))
        })
        try {
            val client = SecureAccountRestClient("https://example.test", http, testProofFactory())
            val failure = assertFailsWith<AccountHttpException> {
                client.signIn("alice@example.test", "long-secret-phrase",
                    AccountDevice("desktop", "Desktop", "DESKTOP"))
            }
            assertEquals(AccountFailure.THROTTLED, failure.failure)
            assertEquals(30, failure.retryAfterSeconds)
        } finally {
            http.close()
        }
    }

    @Test
    fun `registration retries nonce challenge with a fresh proof and returns pending request`() = runBlocking {
        val proofs = mutableListOf<String>()
        val http = HttpClient(MockEngine { request ->
            val proof = requireNotNull(request.headers["DPoP"])
            proofs += proof
            if (proofs.size == 1) {
                respond("""{"code":"DPOP_NONCE_REQUIRED"}""", HttpStatusCode.Unauthorized,
                    headersOf("DPoP-Nonce", "new-server-nonce"))
            } else {
                respond("""{"requestId":"pending-request-id-123456789","status":"AWAITING_APPROVAL"}""",
                    HttpStatusCode.Accepted)
            }
        })
        try {
            val client = SecureAccountRestClient("https://example.test", http, testProofFactory())
            val result = client.submitRegistration("alice@example.test", "long-secret-phrase",
                AccountDevice("phone", "Phone", "ANDROID"))
            assertEquals("pending-request-id-123456789", result.requestId)
            assertEquals(2, proofs.size)
            val first = claims(proofs[0])
            val second = claims(proofs[1])
            assertNull(first["nonce"])
            assertEquals("new-server-nonce", second["nonce"]?.jsonPrimitive?.content)
            assertNotEquals(first["jti"], second["jti"])
            assertEquals("https://example.test/api/v1/auth/registrations", second["htu"]?.jsonPrimitive?.content)
        } finally {
            http.close()
        }
    }

    @Test
    fun `new device sign in awaits approval and completion issues tokens only after approval`() = runBlocking {
        val calls = mutableListOf<String>()
        val http = HttpClient(MockEngine { request ->
            calls += request.url.encodedPath
            when (request.url.encodedPath) {
                "/api/v1/auth/sign-in" -> respond(
                    """{"requestId":"device-request-id-123456789","status":"AWAITING_APPROVAL"}""",
                    HttpStatusCode.Accepted)
                "/api/v1/auth/enrollments/status" -> respond("""{"status":"APPROVED"}""", HttpStatusCode.OK)
                "/api/v1/auth/devices/complete" -> respond(
                    """{"accessToken":"access-123","refreshToken":"refresh-123"}""", HttpStatusCode.OK)
                else -> error("Unexpected auth path: ${request.url.encodedPath}")
            }
        })
        try {
            val client = SecureAccountRestClient("https://example.test", http, testProofFactory())
            val signIn = client.signIn("alice@example.test", "long-secret-phrase",
                AccountDevice("laptop", "Laptop", "DESKTOP"))
            assertIs<SecureSignInResult.Pending>(signIn)
            assertEquals("device-request-id-123456789", signIn.requestId)
            assertEquals(EnrollmentApprovalStatus.APPROVED, client.enrollmentStatus(signIn.requestId))
            val tokens = client.completeEnrollment(signIn.requestId, "long-secret-phrase",
                EnrollmentKind.DEVICE)
            assertEquals("access-123", tokens.accessToken)
            assertEquals("refresh-123", tokens.refreshToken)
            assertEquals(listOf("/api/v1/auth/sign-in", "/api/v1/auth/enrollments/status",
                "/api/v1/auth/devices/complete"), calls)
        } finally {
            http.close()
        }
    }

    @Test
    fun `auth origin cannot hide a remote HTTP host in userinfo or path`() {
        val http = HttpClient(MockEngine { respond("{}") })
        try {
            for (origin in listOf("http://localhost:80@evil.example", "http://127.0.0.1:80@evil.example",
                "http://localhost.evil.example:80", "https://example.test@evil.example",
                "https://example.test/path")) {
                assertFailsWith<IllegalArgumentException> {
                    SecureAccountRestClient(origin, http, testProofFactory())
                }
            }
        } finally {
            http.close()
        }
    }

    @Test
    fun `refresh keeps one operation ID across nonce retry and logout binds access token`() = runBlocking {
        val seen = mutableListOf<Pair<String, String?>>()
        val refreshBodies = mutableListOf<String>()
        val http = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            seen += path to request.headers["Authorization"]
            if (path.endsWith("/refresh")) refreshBodies += (request.body as TextContent).text
            when {
                path.endsWith("/refresh") && seen.size == 1 -> respond(
                    """{"code":"DPOP_NONCE_REQUIRED"}""", HttpStatusCode.Unauthorized,
                    headersOf("DPoP-Nonce", "refresh-nonce-123456"))
                path.endsWith("/refresh") -> respond(
                    """{"accessToken":"new-access","refreshToken":"new-refresh"}""", HttpStatusCode.OK)
                path.endsWith("/logout") -> {
                    val proof = requireNotNull(request.headers["DPoP"])
                    assertEquals(Base64.UrlSafe.encode(ByteArray(32)).trimEnd('='),
                        claims(proof)["ath"]?.jsonPrimitive?.content)
                    respond("", HttpStatusCode.NoContent)
                }
                else -> error("Unexpected request")
            }
        })
        try {
            val client = SecureAccountRestClient("https://example.test", http, testProofFactory())
            val tokens = client.refresh("old-refresh-token", "operation-id-123456")
            assertEquals("new-access", tokens.accessToken)
            assertEquals("new-refresh", tokens.refreshToken)
            client.logout("new-access")
            assertEquals(listOf(null, null, "DPoP new-access"), seen.map { it.second })
            assertEquals(2, refreshBodies.size)
            assertEquals(refreshBodies[0], refreshBodies[1])
            assertEquals("operation-id-123456", Json.parseToJsonElement(refreshBodies[0])
                .jsonObject["operationId"]?.jsonPrimitive?.content)
        } finally {
            http.close()
        }
    }

    private fun testProofFactory(): DpopProofFactory {
        var sequence = 0
        val publicKey = Json.parseToJsonElement(
            """{"kty":"EC","crv":"P-256","x":"x-coordinate","y":"y-coordinate"}""",
        ).jsonObject
        return DpopProofFactory(object : DpopDeviceKey {
            override val publicJwk = publicKey
            override suspend fun sign(input: ByteArray) = ByteArray(64)
        }, object : DpopProofPrimitives {
            override fun nowEpochSeconds() = 1_800_000_000L
            override fun nextJti() = "fresh-request-proof-${++sequence}-123"
            override fun sha256(input: ByteArray) = ByteArray(32)
        })
    }

    private fun claims(proof: String) = Json.parseToJsonElement(Base64.UrlSafe.decode(
        proof.split('.')[1].let { it + "=".repeat((4 - it.length % 4) % 4) },
    ).decodeToString()).jsonObject
}
