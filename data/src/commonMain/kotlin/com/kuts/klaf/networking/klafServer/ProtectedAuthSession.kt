package com.kuts.klaf.networking.klafServer

import kotlinx.serialization.Serializable

@Serializable
internal data class ProtectedAuthSession(
    val serverOrigin: String,
    val accountEmail: String,
    val deviceId: String,
    val accessToken: String,
    val refreshToken: String,
    val pendingRefreshOperationId: String? = null,
)

internal class ProtectedSessionUnavailableException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

internal interface ProtectedAuthSessionStore {
    suspend fun read(origin: String, email: String): ProtectedAuthSession?
    suspend fun write(session: ProtectedAuthSession)
    suspend fun remove(origin: String, email: String)
    suspend fun <T> withRefreshLock(origin: String, email: String, action: suspend () -> T): T
}
