package com.kuts.klaf.networking.klafServer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException

class SecureAccountFlowTest {

    @Test
    fun `approved sign in persists tokens before returning success`() = runBlocking {
        val gateway = FakeGateway().apply { signInResult = SecureSignInResult.Authenticated(
            SecureLoginTokens("access", "refresh")) }
        val store = FakeStore()
        val flow = SecureAccountFlow("https://one.test", gateway, store)

        assertIs<SecureSignInResult.Authenticated>(flow.signIn("alice@example.test", "long-secret-phrase",
            AccountDevice("desktop-a", "Laptop", "DESKTOP")))
        assertEquals("refresh", store.saved?.refreshToken)
        assertEquals("desktop-a", store.saved?.deviceId)
        store.failWrites = true
        assertFailsWith<ProtectedSessionUnavailableException> {
            flow.signIn("bob@example.test", "long-secret-phrase", AccountDevice("desktop-a", "Laptop", "DESKTOP"))
        }
        assertEquals(1, gateway.logoutCalls)
        Unit
    }

    @Test
    fun `pending approval never stores login credentials`() = runBlocking {
        val gateway = FakeGateway().apply { signInResult = SecureSignInResult.Pending("pending-id-123456789") }
        val store = FakeStore()
        val flow = SecureAccountFlow("https://one.test", gateway, store)
        assertIs<SecureSignInResult.Pending>(flow.signIn("alice@example.test", "long-secret-phrase",
            AccountDevice("desktop-a", "Laptop", "DESKTOP")))
        assertNull(store.saved)
    }

    @Test
    fun `offline logout still clears local credentials`() = runBlocking {
        val gateway = FakeGateway().apply { failLogout = true }
        val store = FakeStore().apply { saved = ProtectedAuthSession("https://one.test", "alice@example.test",
            "desktop-a", "access", "refresh") }
        SecureAccountFlow("https://one.test", gateway, store).logout("alice@example.test")
        assertNull(store.saved)
    }

    @Test
    fun `cancelled credential write does not start a revocation request`() = runBlocking {
        val gateway = FakeGateway().apply { signInResult = SecureSignInResult.Authenticated(
            SecureLoginTokens("access", "refresh")) }
        val store = FakeStore().apply { cancelWrites = true }
        val flow = SecureAccountFlow("https://one.test", gateway, store)

        assertFailsWith<CancellationException> {
            flow.signIn("alice@example.test", "long-secret-phrase", AccountDevice("desktop-a", "Laptop", "DESKTOP"))
        }
        assertEquals(0, gateway.logoutCalls)
    }

    private class FakeGateway : SecureAccountGateway {
        var signInResult: SecureSignInResult = SecureSignInResult.Pending("pending-id-123456789")
        var logoutCalls = 0
        var failLogout = false

        override suspend fun submitRegistration(email: String, password: String, device: AccountDevice) =
            PendingEnrollment("pending-id-123456789")
        override suspend fun signIn(email: String, password: String, device: AccountDevice) = signInResult
        override suspend fun enrollmentStatus(requestId: String) = EnrollmentApprovalStatus.AWAITING_APPROVAL
        override suspend fun completeEnrollment(requestId: String, password: String, kind: EnrollmentKind) =
            SecureLoginTokens("access", "refresh")
        override suspend fun refresh(refreshToken: String, operationId: String) = SecureLoginTokens("access", "refresh")
        override suspend fun logout(accessToken: String) {
            logoutCalls++
            if (failLogout) throw IllegalStateException("offline")
        }
    }

    private class FakeStore : ProtectedAuthSessionStore {
        var saved: ProtectedAuthSession? = null
        var failWrites = false
        var cancelWrites = false
        override suspend fun read(origin: String, email: String): ProtectedAuthSession? = saved
        override suspend fun write(session: ProtectedAuthSession) {
            if (cancelWrites) throw CancellationException("cancelled")
            if (failWrites) throw ProtectedSessionUnavailableException("locked")
            saved = session
        }
        override suspend fun remove(origin: String, email: String) { saved = null }
        override suspend fun <T> withRefreshLock(origin: String, email: String, action: suspend () -> T): T = action()
    }
}
