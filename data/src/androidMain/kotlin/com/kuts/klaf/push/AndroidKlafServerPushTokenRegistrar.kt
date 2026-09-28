package com.kuts.klaf.push

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.klaf.networking.klafServer.IKlafServerSession
import com.kuts.klaf.server.contract.KlafServerErrorMessage
import com.kuts.klaf.server.contract.PushTokenRegisterRequest
import com.kuts.klaf.server.contract.PushTokenRegisteredMessage
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AndroidKlafServerPushTokenRegistrar(
    private val klafServerSession: IKlafServerSession,
    private val pushTokenManager: KlafPushTokenManager,
    coroutineContextProvider: ICoroutineContextProvider,
) {

    private val scope = CoroutineScope(coroutineContextProvider.io + SupervisorJob())
    private val registrationMutex = Mutex()

    init {
        observeConnectionState()
        observeTokenChanges()
        registerIfAlreadyReady()
    }

    private fun observeConnectionState() {
        scope.launch {
            klafServerSession.connectionState.collect { state ->
                if (state == KlafServerConnectionState.Ready) {
                    registerCurrentToken(reason = "connection-ready")
                }
            }
        }
    }

    private fun observeTokenChanges() {
        scope.launch {
            pushTokenManager.token
                .filterNotNull()
                .collect { token ->
                    if (klafServerSession.connectionState.value == KlafServerConnectionState.Ready) {
                        registerToken(
                            token = token,
                            reason = "token-updated",
                        )
                    }
                }
        }
    }

    private fun registerIfAlreadyReady() {
        scope.launch {
            if (klafServerSession.connectionState.value == KlafServerConnectionState.Ready) {
                registerCurrentToken(reason = "initial-ready-state")
            }
        }
    }

    private suspend fun registerCurrentToken(reason: String) {
        val token = pushTokenManager.token.value ?: pushTokenManager.refreshToken() ?: return
        registerToken(
            token = token,
            reason = reason,
        )
    }

    private suspend fun registerToken(
        token: String,
        reason: String,
    ) = registrationMutex.withLock {
        try {
            val requestId = klafServerSession.nextRequestId(prefix = "push-token")
            logD(
                "Klaf push token registration started: requestId=$requestId, " +
                    "reason=$reason, tokenHash=${token.hashCode()}",
            )
            val response = klafServerSession.request(
                message = PushTokenRegisterRequest(
                    requestId = requestId,
                    token = token,
                    clientSessionId = klafServerSession.clientSessionId,
                ),
            )
            when (response) {
                is PushTokenRegisteredMessage -> {
                    logD("Klaf push token registered: requestId=$requestId, reason=$reason")
                }
                is KlafServerErrorMessage -> {
                    logE(
                        "Klaf push token registration failed: requestId=$requestId, " +
                            "code=${response.code}, message=${response.message}",
                    )
                }
                else -> {
                    logE(
                        "Klaf push token registration got unexpected response: " +
                            "requestId=$requestId, response=${response::class.simpleName}",
                    )
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logE("Klaf push token registration failed: reason=$reason, failure=$throwable")
        }
    }
}
