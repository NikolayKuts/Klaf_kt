package com.kuts.klaf.networking.agentDriver

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.klaf.mnemonic.AndroidMnemonicGenerationDiagnostics
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Closes idle sockets in background while leaving active requests to SDK reconnect handling. */
class AndroidAgentDriverSessionLifecycleManager(
    application: Application,
    private val agentDriverSession: AgentDriverSession,
    private val diagnostics: AndroidMnemonicGenerationDiagnostics,
    coroutineContextProvider: ICoroutineContextProvider,
) : Application.ActivityLifecycleCallbacks {

    private val scope = CoroutineScope(coroutineContextProvider.io + SupervisorJob())
    private var lifecycleJob: Job? = null
    private var startedActivityCount = 0
    private var configurationChangeCount = 0
    private var hasEnteredBackground = false
    private var nextOperationId = 0L

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        if (configurationChangeCount > 0) {
            configurationChangeCount--
        } else {
            startedActivityCount++
        }

        if (startedActivityCount == 1 && hasEnteredBackground) {
            hasEnteredBackground = false
            logD(
                "Agent Driver app lifecycle detected foreground: " +
                    "activity=${activity::class.simpleName}; ${diagnostics.snapshot()}",
            )
            enqueueLifecycleOperation(
                operationName = "foreground refresh",
                operation = agentDriverSession::onApplicationForegrounded,
            )
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.isChangingConfigurations) {
            configurationChangeCount++
            return
        }

        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
        if (startedActivityCount == 0) {
            hasEnteredBackground = true
            logD(
                "Agent Driver app lifecycle detected background: " +
                    "activity=${activity::class.simpleName}; ${diagnostics.snapshot()}",
            )
            enqueueLifecycleOperation(
                operationName = "background transition",
                operation = agentDriverSession::onApplicationBackgrounded,
            )
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun enqueueLifecycleOperation(
        operationName: String,
        operation: suspend () -> Unit,
    ) {
        val operationId = nextOperationId++
        val previousJob = lifecycleJob
        logD("Agent Driver lifecycle operation queued: id=$operationId, name=$operationName")
        lifecycleJob = scope.launch {
            previousJob?.join()
            logD(
                "Agent Driver lifecycle operation starting: id=$operationId, name=$operationName, " +
                    "connection=${agentDriverSession.connectionState.value::class.simpleName}; " +
                    diagnostics.snapshot(),
            )
            runCatching { operation() }
                .onSuccess {
                    logD(
                        "Agent Driver lifecycle operation completed: id=$operationId, " +
                            "name=$operationName, " +
                            "connection=${agentDriverSession.connectionState.value::class.simpleName}",
                    )
                }
                .onFailure { failure ->
                    logE("Agent Driver $operationName failed: ${failure.stackTraceToString()}")
                }
        }
    }
}
