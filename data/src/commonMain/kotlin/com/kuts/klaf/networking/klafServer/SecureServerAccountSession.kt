package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountEnrollmentKind
import com.kuts.domain.managers.AccountEnrollmentStatus
import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.managers.AccountPendingEnrollment
import com.kuts.domain.managers.AccountSignInResult
import com.kuts.domain.managers.AccountSignUpResult
import com.kuts.domain.managers.IAccountSession
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Keeps pending approval in guest mode and switches profiles only after bound credentials are stored. */
internal class SecureServerAccountSession(
    private val authProvider: suspend () -> SecureAccountFlow,
    private val localDatabase: ActiveLocalRoomDatabase,
    private val guestTransfer: GuestAccountDataTransfer,
    private val deviceProvider: suspend () -> AccountDevice,
    private val pendingStore: PendingSignUpAttemptStore,
    private val removeLocalCredentials: suspend (String) -> Unit,
) : IAccountSession {

    private val sessionMutex = Mutex()
    private val pendingState = MutableStateFlow(pendingStore.read()?.toDomain())
    private val mutableSameAccountSignInEpoch = MutableStateFlow(0L)

    override val selectedAccountEmail: Flow<String?> = localDatabase.selection
        .map { it.accountEmail }
        .distinctUntilChanged()
    override val sameAccountSignInEpoch: StateFlow<Long> = mutableSameAccountSignInEpoch
    override val pendingEnrollment: StateFlow<AccountPendingEnrollment?> = pendingState

    init {
        val pending = pendingStore.read()
        if (pending != null && pending.email == localDatabase.selection.value.accountEmail) {
            pendingStore.clear()
            pendingState.value = null
        }
    }

    override suspend fun signUp(email: String): Unit = throw AccountOperationException(AccountFailure.INVALID_REQUEST)

    override suspend fun signIn(email: String): AccountSignInResult =
        throw AccountOperationException(AccountFailure.INVALID_REQUEST)

    override suspend fun registerDevice(email: String): Unit =
        throw AccountOperationException(AccountFailure.INVALID_REQUEST)

    override suspend fun signUp(email: String, password: String): AccountSignUpResult = sessionMutex.withLock {
        check(localDatabase.selection.value.accountEmail == null) { "Sign-up requires guest mode" }
        val normalized = email.trim().lowercase()
        val device = deviceProvider()
        val previous = pendingStore.read()
        if (previous != null && (previous.email != normalized || previous.deviceId != device.id ||
                previous.kind != AccountEnrollmentKind.ACCOUNT.name)) {
            throw AccountOperationException(AccountFailure.PENDING_SIGNUP)
        }
        val request = authProvider().submitRegistration(normalized, password, device)
        PendingSignUpAttempt(normalized, device.id, request.requestId,
            AccountEnrollmentKind.ACCOUNT.name).also {
            pendingStore.write(it)
            pendingState.value = it.toDomain()
        }
        AccountSignUpResult.PendingApproval(request.requestId)
    }

    override suspend fun signIn(email: String, password: String): AccountSignInResult = sessionMutex.withLock {
        val normalized = email.trim().lowercase()
        val wasSelected = localDatabase.selection.value.accountEmail == normalized
        val device = deviceProvider()
        val previous = pendingStore.read()
        if (previous != null && (previous.email != normalized || previous.deviceId != device.id ||
                previous.kind != AccountEnrollmentKind.DEVICE.name)) {
            throw AccountOperationException(AccountFailure.PENDING_SIGNUP)
        }
        when (val result = authProvider().signIn(normalized, password, device)) {
            is SecureSignInResult.Authenticated -> {
                localDatabase.selectAccount(normalized)
                if (wasSelected) mutableSameAccountSignInEpoch.value++
                pendingStore.clear()
                pendingState.value = null
                AccountSignInResult.SignedIn
            }
            is SecureSignInResult.Pending -> {
                PendingSignUpAttempt(normalized, device.id, result.requestId,
                    AccountEnrollmentKind.DEVICE.name).also {
                    pendingStore.write(it)
                    pendingState.value = it.toDomain()
                }
                AccountSignInResult.PendingApproval(result.requestId)
            }
        }
    }

    override suspend fun completePendingEnrollment(password: String): AccountEnrollmentKind = sessionMutex.withLock {
        val pending = pendingStore.read() ?: throw AccountOperationException(AccountFailure.INVALID_REQUEST)
        val kind = AccountEnrollmentKind.entries.firstOrNull { it.name == pending.kind }
            ?: throw AccountOperationException(AccountFailure.INVALID_REQUEST)
        val device = deviceProvider()
        check(device.id == pending.deviceId) { "Pending enrollment belongs to another installation" }
        if (kind == AccountEnrollmentKind.ACCOUNT) {
            check(localDatabase.selection.value.accountEmail == null) { "Sign-up completion requires guest mode" }
        }
        val remoteKind = when (kind) {
            AccountEnrollmentKind.ACCOUNT -> EnrollmentKind.ACCOUNT
            AccountEnrollmentKind.DEVICE -> EnrollmentKind.DEVICE
        }
        authProvider().completeEnrollment(pending.email, pending.deviceId, pending.requestId, password, remoteKind)
        if (kind == AccountEnrollmentKind.ACCOUNT) {
            guestTransfer.transfer(pending.email)
        }
        localDatabase.selectAccount(pending.email)
        pendingStore.clear()
        pendingState.value = null
        kind
    }

    override suspend fun checkPendingEnrollment(): AccountEnrollmentStatus = sessionMutex.withLock {
        val pending = pendingStore.read() ?: throw AccountOperationException(AccountFailure.INVALID_REQUEST)
        val current = authProvider().enrollmentStatus(pending.requestId)
        AccountEnrollmentStatus.entries.first { it.name == current.name }
    }

    override suspend fun signOut(): Unit = sessionMutex.withLock {
        val accountEmail = localDatabase.selection.value.accountEmail
        try {
            if (accountEmail != null) {
                try {
                    authProvider().logout(accountEmail)
                } catch (failure: Exception) {
                    withContext(NonCancellable) { removeLocalCredentials(accountEmail) }
                    if (failure is CancellationException) throw failure
                }
            }
        } finally {
            withContext(NonCancellable) { localDatabase.selectAccount(null) }
        }
    }

    override suspend fun resetPassword(email: String, token: String, newPassword: String): Unit =
        sessionMutex.withLock {
            authProvider().resetPassword(email.trim().lowercase(), token, newPassword)
        }

    private fun PendingSignUpAttempt.toDomain(): AccountPendingEnrollment = AccountPendingEnrollment(
        email = email,
        requestId = requestId,
        kind = AccountEnrollmentKind.entries.firstOrNull { it.name == kind }
            ?: throw AccountOperationException(AccountFailure.INVALID_REQUEST),
    )
}
