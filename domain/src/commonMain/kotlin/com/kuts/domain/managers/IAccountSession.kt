package com.kuts.domain.managers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

sealed interface AccountSignInResult {

    data object SignedIn : AccountSignInResult

    data class DeviceRegistrationRequired(val email: String) : AccountSignInResult

    data class PendingApproval(val requestId: String) : AccountSignInResult
}

sealed interface AccountSignUpResult {

    data class PendingApproval(val requestId: String) : AccountSignUpResult
}

enum class AccountEnrollmentKind { ACCOUNT, DEVICE }

enum class AccountEnrollmentStatus { AWAITING_APPROVAL, APPROVED, EXPIRED }

data class AccountPendingEnrollment(
    val email: String,
    val requestId: String,
    val kind: AccountEnrollmentKind,
)

interface IAccountSession {

    val selectedAccountEmail: Flow<String?>

    /** Emits a new value when a fresh login replaces a session for the already-selected account. */
    val sameAccountSignInEpoch: Flow<Long> get() = flowOf(0L)

    val pendingEnrollment: Flow<AccountPendingEnrollment?> get() = flowOf(null)

    suspend fun signUp(email: String)

    suspend fun signUp(email: String, password: String): AccountSignUpResult

    suspend fun signIn(email: String): AccountSignInResult

    suspend fun signIn(email: String, password: String): AccountSignInResult

    suspend fun registerDevice(email: String)

    suspend fun completePendingEnrollment(password: String): AccountEnrollmentKind =
        throw AccountOperationException(AccountFailure.INVALID_REQUEST)

    suspend fun checkPendingEnrollment(): AccountEnrollmentStatus =
        throw AccountOperationException(AccountFailure.INVALID_REQUEST)

    suspend fun signOut()

    suspend fun resetPassword(email: String, token: String, newPassword: String): Unit =
        throw AccountOperationException(AccountFailure.INVALID_REQUEST)
}
