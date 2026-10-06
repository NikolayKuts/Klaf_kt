package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SecureRequestAuthorizerTest {

    @Test
    fun `only matching account and origin receive a fresh access-bound proof`() = runBlocking {
        val sessions = MemorySessions(ProtectedAuthSession("https://server.test", "alice@example.test",
            "desktop-a", "access-secret", "refresh-secret"))
        val signer = SecureRequestAuthorizer("https://server.test", sessions, proofFactory())

        val first = signer.headers("alice@example.test", "GET", "https://server.test/api/v1/sync/bootstrap", null)
        val second = signer.headers("alice@example.test", "GET", "https://server.test/api/v1/sync/bootstrap",
            "nonce-from-server")
        assertEquals("DPoP access-secret", first.authorization)
        assertNotEquals(first.dpop, second.dpop)
        val claims = Json.parseToJsonElement(decode(second.dpop.split('.')[1]).decodeToString()).jsonObject
        assertEquals("nonce-from-server", claims["nonce"]?.jsonPrimitive?.content)
        assertEquals("https://server.test/api/v1/sync/bootstrap", claims["htu"]?.jsonPrimitive?.content)
        assertEquals("GET", claims["htm"]?.jsonPrimitive?.content)

        assertEquals(AccountFailure.SIGN_IN_REQUIRED, assertFailsWith<AccountOperationException> {
            signer.headers("bob@example.test", "GET", "https://server.test/api/v1/sync/bootstrap", null)
        }.failure)
        assertFailsWith<IllegalArgumentException> {
            signer.headers("alice@example.test", "GET", "https://server.test.evil/api/v1/sync", null)
        }
        Unit
    }

    private fun proofFactory() = DpopProofFactory(object : DpopDeviceKey {
        override val publicJwk = Json.parseToJsonElement(
            """{"kty":"EC","crv":"P-256","x":"x-coordinate","y":"y-coordinate"}""",
        ).jsonObject
        override suspend fun sign(input: ByteArray) = ByteArray(64)
    }, object : DpopProofPrimitives {
        private var counter = 0
        override fun nowEpochSeconds() = 1_800_000_000L
        override fun nextJti() = "proof-${++counter}-unique-123456"
        override fun sha256(input: ByteArray) = ByteArray(32) { 42 }
    })

    private fun decode(value: String): ByteArray = Base64.UrlSafe.decode(
        value + "=".repeat((4 - value.length % 4) % 4),
    )

    private class MemorySessions(private val saved: ProtectedAuthSession?) : ProtectedAuthSessionStore {
        override suspend fun read(origin: String, email: String) = saved?.takeIf {
            it.serverOrigin == origin && it.accountEmail == email
        }
        override suspend fun write(session: ProtectedAuthSession) = Unit
        override suspend fun remove(origin: String, email: String) = Unit
        override suspend fun <T> withRefreshLock(origin: String, email: String, action: suspend () -> T): T = action()
    }
}
