package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class AccessProofHeaders(val authorization: String, val dpop: String)

/** Adds a per-request proof for a locally selected account; tokens never authorize another origin. */
interface KlafAuthenticatedRequestSigner {
    suspend fun headers(email: String, method: String, url: String, nonce: String?): AccessProofHeaders
}

internal class SecureRequestAuthorizer(
    private val serverOrigin: String,
    private val sessions: ProtectedAuthSessionStore,
    private val proofs: DpopProofFactory,
    private val gateway: SecureAccountGateway? = null,
    private val nowEpochSeconds: () -> Long = { 0L },
    private val nextOperationId: () -> String = { error("Refresh operation ID source unavailable") },
) : KlafAuthenticatedRequestSigner {

    init { requireSafeAuthOrigin(serverOrigin) }

    override suspend fun headers(email: String, method: String, url: String, nonce: String?): AccessProofHeaders {
        require(url.startsWith("$serverOrigin/")) { "Protected request must target its configured server origin" }
        val session = freshSession(email)
        if (session.serverOrigin != serverOrigin || session.accountEmail != email || session.accessToken.isBlank()) {
            throw ProtectedSessionUnavailableException("Protected account session does not match its profile")
        }
        return AccessProofHeaders(
            authorization = "DPoP ${session.accessToken}",
            dpop = proofs.create(method, url, nonce, session.accessToken),
        )
    }

    private suspend fun freshSession(email: String): ProtectedAuthSession {
        val initial = sessions.read(serverOrigin, email)
            ?: throw AccountOperationException(AccountFailure.SIGN_IN_REQUIRED)
        if (gateway == null || !needsRefresh(initial)) return initial
        return sessions.withRefreshLock(serverOrigin, email) {
            val current = sessions.read(serverOrigin, email)
                ?: throw AccountOperationException(AccountFailure.SIGN_IN_REQUIRED)
            if (!needsRefresh(current)) return@withRefreshLock current
            val operationId = current.pendingRefreshOperationId ?: nextOperationId().also {
                require(it.length in 16..128)
                sessions.write(current.copy(pendingRefreshOperationId = it))
            }
            val replacement = try {
                gateway.refresh(current.refreshToken, operationId)
            } catch (failure: AccountOperationException) {
                if (failure.failure == AccountFailure.SIGN_IN_REQUIRED) {
                    sessions.remove(serverOrigin, email)
                }
                throw failure
            }
            current.copy(
                accessToken = replacement.accessToken,
                refreshToken = replacement.refreshToken,
                pendingRefreshOperationId = null,
            ).also { sessions.write(it) }
        }
    }

    private fun needsRefresh(session: ProtectedAuthSession): Boolean {
        if (session.pendingRefreshOperationId != null) return true
        val expiration = runCatching {
            val payload = session.accessToken.split('.')[1]
            val padded = payload.padEnd((payload.length + 3) / 4 * 4, '=')
            Json.parseToJsonElement(Base64.UrlSafe.decode(padded).decodeToString())
                .jsonObject["exp"]?.jsonPrimitive?.longOrNull
        }.getOrNull()
        return expiration == null || expiration <= nowEpochSeconds() + 30L
    }
}
