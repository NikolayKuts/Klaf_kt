package com.kuts.klaf.networking.agentDriver

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.kuts.domain.common.ICoroutineContextProvider
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Keeps an idle Agent Driver socket out of Android's frozen process state. */
class AndroidAgentDriverSessionLifecycleManager(
    application: Application,
    private val agentDriverSession: AgentDriverSession,
    coroutineContextProvider: ICoroutineContextProvider,
) : Application.ActivityLifecycleCallbacks {

    private val scope = CoroutineScope(coroutineContextProvider.io + SupervisorJob())
    private var lifecycleJob: Job? = null
    private var startedActivityCount = 0
    private var configurationChangeCount = 0
    private var hasEnteredBackground = false

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
            enqueueLifecycleOperation(
                operationName = "background disconnect",
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
        val previousJob = lifecycleJob
        lifecycleJob = scope.launch {
            previousJob?.join()
            runCatching { operation() }
                .onFailure { failure ->
                    logE("Agent Driver $operationName failed: ${failure.stackTraceToString()}")
                }
        }
    }
}
