package com.kuts.klaf.networking.klafServer

import kotlinx.serialization.Serializable

@Serializable
data class PendingSignUpAttempt(
    val email: String,
    val deviceId: String,
    val requestId: String,
    val kind: String = "ACCOUNT",
)

interface PendingSignUpAttemptStore {

    fun read(): PendingSignUpAttempt?

    fun write(attempt: PendingSignUpAttempt)

    fun clear()
}
