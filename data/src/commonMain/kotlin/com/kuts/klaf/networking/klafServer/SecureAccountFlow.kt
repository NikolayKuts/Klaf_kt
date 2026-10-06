package com.kuts.klaf.networking.klafServer

import kotlinx.coroutines.CancellationException

/** Persists accepted credentials before any caller may select an account profile. */
internal class SecureAccountFlow(
    private val serverOrigin: String,
    private val gateway: SecureAccountGateway,
    private val sessions: ProtectedAuthSessionStore,
) {

    init { requireSafeAuthOrigin(serverOrigin) }

    suspend fun submitRegistration(email: String, password: String, device: AccountDevice): PendingEnrollment =
        gateway.submitRegistration(email, password, device)

    suspend fun signIn(email: String, password: String, device: AccountDevice): SecureSignInResult {
        val result = gateway.signIn(email, password, device)
        if (result is SecureSignInResult.Authenticated) {
            persistOrRevoke(email, device.id, result.tokens)
        }
        return result
    }

    suspend fun enrollmentStatus(requestId: String): EnrollmentApprovalStatus =
        gateway.enrollmentStatus(requestId)

    suspend fun completeEnrollment(
        email: String,
        deviceId: String,
        requestId: String,
        password: String,
        kind: EnrollmentKind,
    ): ProtectedAuthSession {
        val tokens = gateway.completeEnrollment(requestId, password, kind)
        return persistOrRevoke(email, deviceId, tokens)
    }

    suspend fun logout(email: String) {
        val active = sessions.read(serverOrigin, email)
        try {
            if (active != null) gateway.logout(active.accessToken)
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            // Offline logout immediately removes local credentials; server revocation is best effort.
        } finally {
            sessions.remove(serverOrigin, email)
        }
    }

    suspend fun resetPassword(email: String, token: String, password: String) {
        gateway.resetPassword(email, token, password)
        sessions.remove(serverOrigin, email)
    }

    private suspend fun persistOrRevoke(
        email: String,
        deviceId: String,
        tokens: SecureLoginTokens,
    ): ProtectedAuthSession {
        val session = ProtectedAuthSession(serverOrigin, email, deviceId, tokens.accessToken, tokens.refreshToken)
        try {
            sessions.write(session)
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            try {
                gateway.logout(tokens.accessToken)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Failed persistence must never be converted into successful local sign-in.
            }
            throw failure
        }
        return session
    }
}
