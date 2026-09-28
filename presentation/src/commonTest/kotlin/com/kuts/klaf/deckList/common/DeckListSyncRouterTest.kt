package com.kuts.klaf.deckList.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

class DeckListSyncRouterTest {

    @Test
    fun `account sync uses manual gateway and never starts legacy worker`() = runBlocking {
        var legacyCalls = 0
        var manualCalls = 0
        val gateway = FakeGateway("alice@example.test") {
            manualCalls++
            AccountSyncOutcome.APPLIED
        }
        val router = DeckListSyncRouter(
            accountGateway = gateway,
            legacySignedIn = { true },
            networkConnected = { true },
            startLegacyWorker = { legacyCalls++ },
        )

        assertEquals(DeckListSyncStart.APPLIED, router.synchronize())
        assertEquals(1, manualCalls)
        assertEquals(0, legacyCalls)
    }

    @Test
    fun `manual local server sync is attempted without an Android active network`() = runBlocking {
        var manualCalls = 0
        val router = DeckListSyncRouter(
            accountGateway = FakeGateway("alice@example.test") {
                manualCalls++
                AccountSyncOutcome.APPLIED
            },
            legacySignedIn = { error("Not a Firebase account") },
            networkConnected = { false },
            startLegacyWorker = { error("Legacy worker must not run") },
        )
        assertEquals(DeckListSyncStart.APPLIED, router.synchronize())
        assertEquals(1, manualCalls)

        val unavailable = DeckListSyncRouter(
            accountGateway = FakeGateway("alice@example.test") {
                throw AccountOperationException(AccountFailure.CONNECTION)
            },
            legacySignedIn = { error("Not a Firebase account") },
            networkConnected = { false },
            startLegacyWorker = { error("Legacy worker must not run") },
        )
        assertEquals(AccountFailure.CONNECTION,
            assertFailsWith<AccountOperationException> { unavailable.synchronize() }.failure)
    }

    @Test
    fun `legacy mode still checks network before starting Firebase worker`() = runBlocking {
        val router = DeckListSyncRouter(
            accountGateway = null,
            legacySignedIn = { true },
            networkConnected = { false },
            startLegacyWorker = { error("Offline legacy worker must not run") },
        )
        assertEquals(DeckListSyncStart.NETWORK_UNAVAILABLE, router.synchronize())
    }

    @Test
    fun `guest account mode requests sign in without starting either sync`() = runBlocking {
        var legacyCalls = 0
        var manualCalls = 0
        val router = DeckListSyncRouter(
            accountGateway = FakeGateway(null) { manualCalls++; AccountSyncOutcome.APPLIED },
            legacySignedIn = { true },
            networkConnected = { true },
            startLegacyWorker = { legacyCalls++ },
        )

        assertEquals(DeckListSyncStart.NEEDS_SIGN_IN, router.synchronize())
        assertEquals(0, manualCalls)
        assertEquals(0, legacyCalls)
    }

    @Test
    fun `static mode keeps the legacy worker`() = runBlocking {
        var legacyCalls = 0
        val router = DeckListSyncRouter(
            accountGateway = null,
            legacySignedIn = { true },
            networkConnected = { true },
            startLegacyWorker = { legacyCalls++ },
        )

        assertEquals(DeckListSyncStart.LEGACY_STARTED, router.synchronize())
        assertEquals(1, legacyCalls)
    }

    @Test
    fun `manual conflict result is not reported as a successful sync`() = runBlocking {
        val router = DeckListSyncRouter(
            accountGateway = FakeGateway("alice@example.test") { AccountSyncOutcome.NEEDS_RESOLUTION },
            legacySignedIn = { true },
            networkConnected = { true },
            startLegacyWorker = { error("Legacy worker must not run") },
        )

        assertEquals(DeckListSyncStart.NEEDS_RESOLUTION, router.synchronize())
    }

    private class FakeGateway(
        email: String?,
        private val onSync: suspend () -> AccountSyncOutcome,
    ) : AccountDeckListGateway {

        override val selectedAccountEmail: Flow<String?> = flowOf(email)

        override suspend fun synchronize(): AccountSyncOutcome = onSync()

        override suspend fun signOut() = Unit
    }
}
