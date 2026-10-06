package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class SecureRequestAuthorizerRefreshTest {

    @Test
    fun `lost refresh response reuses journaled operation and stores recovered pair`() = runBlocking {
        val store = MemorySessions(ProtectedAuthSession("https://server.test", "alice@example.test",
            "desktop-a", token(exp = 10), "old-refresh"))
        val gateway = LostResponseGateway()
        val signer = SecureRequestAuthorizer("https://server.test", store, proofFactory(), gateway,
            nowEpochSeconds = { 100L }, nextOperationId = { "stable-operation-id-123456" })

        assertFailsWith<IllegalStateException> {
            signer.headers("alice@example.test", "GET", "https://server.test/api/v1/sync/bootstrap", null)
        }
        assertEquals("stable-operation-id-123456", store.saved?.pendingRefreshOperationId)
        assertEquals("old-refresh", store.saved?.refreshToken)

        val headers = signer.headers("alice@example.test", "GET", "https://server.test/api/v1/sync/bootstrap", null)
        assertEquals("DPoP ${token(exp = 1_000)}", headers.authorization)
        assertEquals(listOf("stable-operation-id-123456", "stable-operation-id-123456"), gateway.operations)
        assertEquals("new-refresh", store.saved?.refreshToken)
        assertEquals(null, store.saved?.pendingRefreshOperationId)
    }

    @Test
    fun `parallel requests share one rotation and use the persisted new token`() = runBlocking {
        val store = MemorySessions(ProtectedAuthSession("https://server.test", "alice@example.test",
            "desktop-a", token(exp = 10), "old-refresh"))
        val gateway = LostResponseGateway(failFirst = false)
        val signer = SecureRequestAuthorizer("https://server.test", store, proofFactory(), gateway,
            nowEpochSeconds = { 100L }, nextOperationId = { "stable-operation-id-123456" })

        val headers = (1..6).map {
            async { signer.headers("alice@example.test", "GET", "https://server.test/api/v1/sync/bootstrap", null) }
        }.awaitAll()

        assertEquals(1, gateway.operations.size)
        assertEquals(List(6) { "DPoP ${token(exp = 1_000)}" }, headers.map { it.authorization })
        assertEquals(null, store.saved?.pendingRefreshOperationId)
    }

    @Test
    fun `rejected refresh clears unusable credentials`() = runBlocking {
        val store = MemorySessions(ProtectedAuthSession("https://server.test", "alice@example.test",
            "desktop-a", token(exp = 10), "old-refresh"))
        val gateway = LostResponseGateway(reject = true)
        val signer = SecureRequestAuthorizer("https://server.test", store, proofFactory(), gateway,
            nowEpochSeconds = { 100L }, nextOperationId = { "stable-operation-id-123456" })

        val failure = assertFailsWith<AccountOperationException> {
            signer.headers("alice@example.test", "GET", "https://server.test/api/v1/sync/bootstrap", null)
        }
        assertEquals(AccountFailure.SIGN_IN_REQUIRED, failure.failure)
        assertEquals(null, store.saved)
    }

    @Test
    fun `failed replacement write keeps journal and can recover receipt`() = runBlocking {
        val store = MemorySessions(ProtectedAuthSession("https://server.test", "alice@example.test",
            "desktop-a", token(exp = 10), "old-refresh"))
        store.failReplacementWrite = true
        val gateway = LostResponseGateway(failFirst = false)
        val signer = SecureRequestAuthorizer("https://server.test", store, proofFactory(), gateway,
            nowEpochSeconds = { 100L }, nextOperationId = { "stable-operation-id-123456" })

        assertFailsWith<IllegalStateException> {
            signer.headers("alice@example.test", "GET", "https://server.test/api/v1/sync/bootstrap", null)
        }
        assertEquals("stable-operation-id-123456", store.saved?.pendingRefreshOperationId)
        assertEquals("old-refresh", store.saved?.refreshToken)

        val headers = signer.headers("alice@example.test", "GET", "https://server.test/api/v1/sync/bootstrap", null)
        assertEquals("DPoP ${token(exp = 1_000)}", headers.authorization)
        assertEquals(listOf("stable-operation-id-123456", "stable-operation-id-123456"), gateway.operations)
        assertEquals("new-refresh", store.saved?.refreshToken)
    }

    private fun token(exp: Long): String = "x." + Base64.UrlSafe.encode("""{"exp":$exp}""".encodeToByteArray())
        .trimEnd('=') + ".y"

    private fun proofFactory() = DpopProofFactory(object : DpopDeviceKey {
        override val publicJwk = Json.parseToJsonElement(
            """{"kty":"EC","crv":"P-256","x":"x","y":"y"}""",
        ).jsonObject
        override suspend fun sign(input: ByteArray) = ByteArray(64)
    }, object : DpopProofPrimitives {
        override fun nowEpochSeconds() = 100L
        override fun nextJti() = "fresh-request-jti-123456"
        override fun sha256(input: ByteArray) = ByteArray(32)
    })

    private class MemorySessions(var saved: ProtectedAuthSession?) : ProtectedAuthSessionStore {
        private val mutex = Mutex()
        var failReplacementWrite = false
        override suspend fun read(origin: String, email: String) = saved
        override suspend fun write(session: ProtectedAuthSession) {
            if (failReplacementWrite && session.refreshToken == "new-refresh") {
                failReplacementWrite = false
                throw IllegalStateException("protected write failed")
            }
            saved = session
        }
        override suspend fun remove(origin: String, email: String) { saved = null }
        override suspend fun <T> withRefreshLock(origin: String, email: String, action: suspend () -> T): T =
            mutex.withLock { action() }
    }

    private class LostResponseGateway(
        private val failFirst: Boolean = true,
        private val reject: Boolean = false,
    ) : SecureAccountGateway {
        val operations = mutableListOf<String>()
        override suspend fun submitRegistration(email: String, password: String, device: AccountDevice) =
            PendingEnrollment("pending-id-123456789")
        override suspend fun signIn(email: String, password: String, device: AccountDevice) =
            SecureSignInResult.Pending("pending-id-123456789")
        override suspend fun enrollmentStatus(requestId: String) = EnrollmentApprovalStatus.APPROVED
        override suspend fun completeEnrollment(requestId: String, password: String, kind: EnrollmentKind) =
            SecureLoginTokens("unused", "unused")
        override suspend fun refresh(refreshToken: String, operationId: String): SecureLoginTokens {
            assertEquals("old-refresh", refreshToken)
            operations += operationId
            if (reject) throw AccountOperationException(AccountFailure.SIGN_IN_REQUIRED)
            if (failFirst && operations.size == 1) throw IllegalStateException("response lost")
            return SecureLoginTokens("x.eyJleHAiOjEwMDB9.y", "new-refresh")
        }
        override suspend fun logout(accessToken: String) = Unit
    }
}
