package com.kuts.domain.managers

enum class AccountFailure {
    CONNECTION, TIMEOUT, ACCOUNT_NOT_FOUND, ACCOUNT_EXISTS, DEVICE_REGISTRATION,
    INVALID_REQUEST, INVALID_RESPONSE, SERVER, PENDING_SIGNUP, UNKNOWN,
}

open class AccountOperationException(
    val failure: AccountFailure,
    message: String = failure.name,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
