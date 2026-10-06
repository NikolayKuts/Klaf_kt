package com.kuts.klaf.networking.klafServer

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.domain.managers.IKlafServerConnectionManager
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class KlafServerConnectionManager(
    private val klafServerSession: IKlafServerSession,
    coroutineContextProvider: ICoroutineContextProvider,
    selectedAccountEmail: Flow<String?>,
    sameAccountSignInEpoch: Flow<Long> = flowOf(0L),
) : IKlafServerConnectionManager {

    override val state = klafServerSession.connectionState

    private val scope = CoroutineScope(coroutineContextProvider.io + SupervisorJob())

    init {
        scope.launch {
            var previousEmail: String? = null
            var previousEpoch: Long? = null
            combine(selectedAccountEmail.distinctUntilChanged(), sameAccountSignInEpoch) { email, epoch ->
                email to epoch
            }.distinctUntilChanged().collectLatest { (email, epoch) ->
                val replaceSession = email != null && email == previousEmail && epoch != previousEpoch
                previousEmail = email
                previousEpoch = epoch
                if (email != null) {
                    connectAfterAccountSelection(replaceSession)
                }
            }
        }
    }

    override suspend fun retry() {
        klafServerSession.connect()
    }

    private suspend fun connectAfterAccountSelection(replaceSession: Boolean) {
        try {
            if (replaceSession) klafServerSession.endUserSession()
            retry()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logE("Klaf Server connection after account authentication failed: $throwable")
        }
    }
}
