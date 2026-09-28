package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountSignInResult
import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.IAccountSession
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Coordinates server account requests with the selected local Room database. */
class ServerAccountSession(
    private val accounts: KlafServerAccountRestClient,
    private val localDatabase: ActiveLocalRoomDatabase,
    private val guestTransfer: GuestAccountDataTransfer,
    private val deviceProvider: suspend () -> AccountDevice,
    private val pendingSignUp: PendingSignUpAttemptStore,
) : IAccountSession {

    constructor(
        accounts: KlafServerAccountRestClient,
        localDatabase: ActiveLocalRoomDatabase,
        guestTransfer: GuestAccountDataTransfer,
        device: AccountDevice,
        pendingSignUp: PendingSignUpAttemptStore,
    ) : this(accounts, localDatabase, guestTransfer, { device }, pendingSignUp)

    private val sessionMutex = Mutex()
    private var pendingDeviceRegistrationEmail: String? = null

    override val selectedAccountEmail: Flow<String?> = localDatabase.selection
        .map { it.accountEmail }
        .distinctUntilChanged()

    init {
        val pending = pendingSignUp.read()
        if (pending != null && pending.email == localDatabase.selection.value.accountEmail) {
            pendingSignUp.clear()
        }
    }

    override suspend fun signUp(email: String) = sessionMutex.withLock {
        check(localDatabase.selection.value.accountEmail == null) { "Sign-up requires guest mode" }
        val device = deviceProvider()
        val normalizedEmail = email.trim().lowercase()
        val previous = pendingSignUp.read()
        check(previous == null || previous.email == normalizedEmail && previous.deviceId == device.id) {
            "Another sign-up needs recovery before creating a different account"
        }
        val attempt = previous ?: PendingSignUpAttempt(
            normalizedEmail,
            device.id,
            newAccountProtocolId(),
        ).also(pendingSignUp::write)
        val account = try {
            accounts.signUp(normalizedEmail, device, attempt.requestId)
        } catch (failure: AccountHttpException) {
            if (failure.errorCode == "ACCOUNT_EXISTS" || failure.errorCode == "INVALID_REQUEST") pendingSignUp.clear()
            throw failure
        }
        guestTransfer.transfer(account.email)
        localDatabase.selectAccount(account.email)
        pendingSignUp.clear()
        pendingDeviceRegistrationEmail = null
    }

    override suspend fun signIn(email: String): AccountSignInResult = sessionMutex.withLock {
        if (pendingSignUp.read() != null) throw AccountOperationException(AccountFailure.PENDING_SIGNUP)
        val device = deviceProvider()
        val normalizedEmail = email.trim().lowercase()
        pendingDeviceRegistrationEmail = null
        try {
            accounts.signIn(normalizedEmail, device.id)
        } catch (failure: AccountHttpException) {
            if (failure.errorCode != "DEVICE_NOT_REGISTERED") throw failure
            pendingDeviceRegistrationEmail = normalizedEmail
            return@withLock AccountSignInResult.DeviceRegistrationRequired(normalizedEmail)
        }
        localDatabase.selectAccount(normalizedEmail)
        AccountSignInResult.SignedIn
    }

    override suspend fun registerDevice(email: String) = sessionMutex.withLock {
        val device = deviceProvider()
        val normalizedEmail = email.trim().lowercase()
        check(pendingDeviceRegistrationEmail == normalizedEmail) {
            "Device registration requires explicit confirmation after sign-in"
        }
        accounts.registerDevice(normalizedEmail, device)
        accounts.signIn(normalizedEmail, device.id)
        localDatabase.selectAccount(normalizedEmail)
        pendingDeviceRegistrationEmail = null
    }

    override suspend fun signOut() = sessionMutex.withLock {
        localDatabase.selectAccount(null)
        pendingDeviceRegistrationEmail = null
    }
}
