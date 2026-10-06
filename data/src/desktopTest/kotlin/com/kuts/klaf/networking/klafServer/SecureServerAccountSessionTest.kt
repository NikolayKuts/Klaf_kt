package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.Deck
import com.kuts.domain.managers.AccountEnrollmentKind
import com.kuts.domain.managers.AccountSignInResult
import com.kuts.domain.managers.AccountSignUpResult
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.repositoryImplementations.DeckRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

class SecureServerAccountSessionTest {

    @Test
    fun `signing in again to selected account emits AI session refresh`() = runBlocking {
        val root = Files.createTempDirectory("klaf-secure-reauth-")
        val database = ActiveLocalRoomDatabase(
            ScopedKlafRoomDatabaseFactory(root.toFile()),
            DesktopSelectedAccountStore(root.toFile()),
        )
        try {
            val credentials = MemorySessions()
            val session = SecureServerAccountSession(
                authProvider = { SecureAccountFlow("https://one.test", FakeGateway(), credentials) },
                localDatabase = database,
                guestTransfer = GuestAccountDataTransfer(database),
                deviceProvider = { AccountDevice("laptop", "Laptop", "DESKTOP") },
                pendingStore = MemoryAttempts(),
                removeLocalCredentials = { email -> credentials.remove("https://one.test", email) },
            )

            assertEquals(AccountSignInResult.SignedIn, session.signIn("alice@example.test", "long-secret-phrase"))
            assertEquals(0L, session.sameAccountSignInEpoch.first())
            assertEquals(AccountSignInResult.SignedIn, session.signIn("alice@example.test", "long-secret-phrase"))
            assertEquals(1L, session.sameAccountSignInEpoch.first())
            assertEquals("alice@example.test", database.selection.value.accountEmail)
        } finally {
            database.close()
        }
    }

    @Test
    fun `sign up awaits approval in guest mode and transfers only after password reentry`() = runBlocking {
        val root = Files.createTempDirectory("klaf-secure-signup-")
        val database = ActiveLocalRoomDatabase(ScopedKlafRoomDatabaseFactory(root.toFile()),
            DesktopSelectedAccountStore(root.toFile()))
        try {
            val decks = DeckRepositoryRoom(database)
            decks.insertDeck(Deck(name = "guest", creationDate = 1L))
            val attempts = MemoryAttempts()
            val credentials = MemorySessions()
            val gateway = FakeGateway()
            val session = SecureServerAccountSession(
                authProvider = { SecureAccountFlow("https://one.test", gateway, credentials) },
                localDatabase = database,
                guestTransfer = GuestAccountDataTransfer(database),
                deviceProvider = { AccountDevice("laptop", "Laptop", "DESKTOP") },
                pendingStore = attempts,
                removeLocalCredentials = { email -> credentials.remove("https://one.test", email) },
            )

            assertIs<AccountSignUpResult.PendingApproval>(session.signUp("alice@example.test", "long-secret-phrase"))
            assertEquals(null, database.selection.value.accountEmail)
            assertEquals("guest", decks.fetchAllDecks().single().name)
            assertEquals("pending-id-123456789", attempts.read()?.requestId)
            assertEquals(AccountEnrollmentKind.ACCOUNT, session.pendingEnrollment.first()?.kind)
            assertEquals(null, credentials.saved)

            gateway.status = EnrollmentApprovalStatus.APPROVED
            assertEquals(AccountEnrollmentKind.ACCOUNT, session.completePendingEnrollment("long-secret-phrase"))
            assertEquals("alice@example.test", database.selection.value.accountEmail)
            assertEquals("guest", decks.fetchAllDecks().single().name)
            assertEquals(null, attempts.read())
            assertEquals(null, session.pendingEnrollment.first())
            assertEquals("refresh", credentials.saved?.refreshToken)
        } finally {
            database.close()
        }
    }

    @Test
    fun `pending sign in leaves guest data untouched and offline logout returns to guest`() = runBlocking {
        val root = Files.createTempDirectory("klaf-secure-signin-")
        val database = ActiveLocalRoomDatabase(ScopedKlafRoomDatabaseFactory(root.toFile()),
            DesktopSelectedAccountStore(root.toFile()))
        try {
            val decks = DeckRepositoryRoom(database)
            decks.insertDeck(Deck(name = "guest", creationDate = 1L))
            val gateway = FakeGateway().apply { signInResult = SecureSignInResult.Pending("pending-id-123456789") }
            val credentials = MemorySessions()
            val session = SecureServerAccountSession(
                authProvider = { SecureAccountFlow("https://one.test", gateway, credentials) },
                localDatabase = database,
                guestTransfer = GuestAccountDataTransfer(database),
                deviceProvider = { AccountDevice("laptop", "Laptop", "DESKTOP") },
                pendingStore = MemoryAttempts(),
                removeLocalCredentials = { email -> credentials.remove("https://one.test", email) },
            )

            assertIs<AccountSignInResult.PendingApproval>(session.signIn("alice@example.test", "long-secret-phrase"))
            assertEquals(null, database.selection.value.accountEmail)
            assertEquals(AccountEnrollmentKind.DEVICE, session.pendingEnrollment.first()?.kind)
            gateway.status = EnrollmentApprovalStatus.APPROVED
            assertEquals(AccountEnrollmentKind.DEVICE, session.completePendingEnrollment("long-secret-phrase"))
            assertEquals("alice@example.test", database.selection.value.accountEmail)
            assertEquals(emptyList(), decks.fetchAllDecks())

            gateway.failLogout = true
            session.signOut()
            assertEquals(null, database.selection.value.accountEmail)
            assertEquals("guest", decks.fetchAllDecks().single().name)
            assertEquals(null, credentials.saved)
        } finally {
            database.close()
        }
    }

    @Test
    fun `logout returns to guest when lazy auth provider cannot initialize`() = runBlocking {
        val root = Files.createTempDirectory("klaf-secure-logout-provider-")
        val database = ActiveLocalRoomDatabase(
            ScopedKlafRoomDatabaseFactory(root.toFile()),
            DesktopSelectedAccountStore(root.toFile()),
        )
        try {
            val credentials = MemorySessions()
            val auth = SecureAccountFlow("https://one.test", FakeGateway(), credentials)
            var providerAvailable = true
            val session = SecureServerAccountSession(
                authProvider = {
                    if (!providerAvailable) error("Device key is unavailable")
                    auth
                },
                localDatabase = database,
                guestTransfer = GuestAccountDataTransfer(database),
                deviceProvider = { AccountDevice("laptop", "Laptop", "DESKTOP") },
                pendingStore = MemoryAttempts(),
                removeLocalCredentials = { email -> credentials.remove("https://one.test", email) },
            )
            assertEquals(AccountSignInResult.SignedIn, session.signIn("alice@example.test", "long-secret-phrase"))
            assertEquals("alice@example.test", database.selection.value.accountEmail)

            providerAvailable = false
            session.signOut()

            assertEquals(null, database.selection.value.accountEmail)
            assertEquals(null, credentials.saved)
        } finally {
            database.close()
        }
    }

    @Test
    fun `cancelled logout still clears credentials and selects guest`() = runBlocking {
        val root = Files.createTempDirectory("klaf-secure-logout-cancelled-")
        val database = ActiveLocalRoomDatabase(
            ScopedKlafRoomDatabaseFactory(root.toFile()),
            DesktopSelectedAccountStore(root.toFile()),
        )
        try {
            val credentials = MemorySessions()
            val auth = SecureAccountFlow("https://one.test", FakeGateway(), credentials)
            var cancelProvider = false
            val session = SecureServerAccountSession(
                authProvider = {
                    if (cancelProvider) throw CancellationException("Logout was cancelled")
                    auth
                },
                localDatabase = database,
                guestTransfer = GuestAccountDataTransfer(database),
                deviceProvider = { AccountDevice("laptop", "Laptop", "DESKTOP") },
                pendingStore = MemoryAttempts(),
                removeLocalCredentials = { email -> credentials.remove("https://one.test", email) },
            )
            assertEquals(AccountSignInResult.SignedIn, session.signIn("alice@example.test", "long-secret-phrase"))

            cancelProvider = true
            assertFailsWith<CancellationException> { session.signOut() }

            assertEquals(null, database.selection.value.accountEmail)
            assertEquals(null, credentials.saved)
        } finally {
            database.close()
        }
    }

    private class MemoryAttempts : PendingSignUpAttemptStore {
        private var attempt: PendingSignUpAttempt? = null
        override fun read() = attempt
        override fun write(attempt: PendingSignUpAttempt) { this.attempt = attempt }
        override fun clear() { attempt = null }
    }

    private class MemorySessions : ProtectedAuthSessionStore {
        var saved: ProtectedAuthSession? = null
        override suspend fun read(origin: String, email: String) = saved
        override suspend fun write(session: ProtectedAuthSession) { saved = session }
        override suspend fun remove(origin: String, email: String) { saved = null }
        override suspend fun <T> withRefreshLock(origin: String, email: String, action: suspend () -> T): T = action()
    }

    private class FakeGateway : SecureAccountGateway {
        var status = EnrollmentApprovalStatus.AWAITING_APPROVAL
        var signInResult: SecureSignInResult = SecureSignInResult.Authenticated(SecureLoginTokens("access", "refresh"))
        var failLogout = false
        override suspend fun submitRegistration(email: String, password: String, device: AccountDevice) =
            PendingEnrollment("pending-id-123456789")
        override suspend fun signIn(email: String, password: String, device: AccountDevice) = signInResult
        override suspend fun enrollmentStatus(requestId: String) = status
        override suspend fun completeEnrollment(requestId: String, password: String, kind: EnrollmentKind) =
            SecureLoginTokens("access", "refresh")
        override suspend fun refresh(refreshToken: String, operationId: String) = SecureLoginTokens("access", "refresh")
        override suspend fun logout(accessToken: String) {
            if (failLogout) error("offline")
        }
    }
}
