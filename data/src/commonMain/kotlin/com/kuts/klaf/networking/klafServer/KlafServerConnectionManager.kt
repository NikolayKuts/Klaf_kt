package com.kuts.klaf.networking.klafServer

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.domain.managers.IKlafServerConnectionManager
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KlafServerConnectionManager(
    private val klafServerSession: IKlafServerSession,
    coroutineContextProvider: ICoroutineContextProvider,
) : IKlafServerConnectionManager {

    override val state = klafServerSession.connectionState

    private val scope = CoroutineScope(coroutineContextProvider.io + SupervisorJob())

    init {
        startConnection()
    }

    override suspend fun retry() {
        klafServerSession.connect()
    }

    private fun startConnection() {
        scope.launch {
            try {
                retry()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                logE("Initial Klaf Server connection failed: $throwable")
            }
        }
    }
}
