package com.kuts.domain.managers

enum class AccountFailure {
    CONNECTION, TIMEOUT, ACCOUNT_NOT_FOUND, ACCOUNT_EXISTS, DEVICE_REGISTRATION,
    INVALID_REQUEST, INVALID_RESPONSE, SERVER, PENDING_SIGNUP, SIGN_IN_REQUIRED,
    INVALID_CREDENTIALS, RESET_TOKEN_INVALID, APPROVAL_PENDING, DEVICE_PROOF, SERVER_BUSY, THROTTLED, UNKNOWN,
}

open class AccountOperationException(
    val failure: AccountFailure,
    message: String = failure.name,
    cause: Throwable? = null,
    val retryAfterSeconds: Int? = null,
) : IllegalStateException(message, cause)
