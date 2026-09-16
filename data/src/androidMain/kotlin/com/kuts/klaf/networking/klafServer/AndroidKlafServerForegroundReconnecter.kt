package com.kuts.klaf.networking.klafServer

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.domain.managers.IKlafServerConnectionManager
import com.kuts.klaf.mnemonic.AndroidApplicationVisibilityTracker
import com.kuts.klaf.mnemonic.AndroidMnemonicGenerationDiagnostics
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AndroidKlafServerForegroundReconnecter(
    applicationVisibilityTracker: AndroidApplicationVisibilityTracker,
    private val connectionManager: IKlafServerConnectionManager,
    coroutineContextProvider: ICoroutineContextProvider,
    private val diagnostics: AndroidMnemonicGenerationDiagnostics,
) {

    private val scope = CoroutineScope(coroutineContextProvider.io + SupervisorJob())
    private var reconnectJob: Job? = null

    init {
        applicationVisibilityTracker.addVisibilityListener { isVisible ->
            if (isVisible) {
                reconnectIfNeeded()
            }
        }
    }

    private fun reconnectIfNeeded() {
        val state = connectionManager.state.value
        if (state == KlafServerConnectionState.Ready) {
            logD("Klaf Server foreground reconnect skipped: state=$state; ${diagnostics.snapshot()}")
            return
        }

        logD("Klaf Server foreground reconnect requested: state=$state; ${diagnostics.snapshot()}")
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            try {
                connectionManager.retry()
                logD(
                    "Klaf Server foreground reconnect completed: " +
                        "state=${connectionManager.state.value}; ${diagnostics.snapshot()}",
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                logE(
                    "Klaf Server foreground reconnect failed: " +
                        "state=${connectionManager.state.value}, failure=$throwable; ${diagnostics.snapshot()}",
                )
            }
        }
    }
}
