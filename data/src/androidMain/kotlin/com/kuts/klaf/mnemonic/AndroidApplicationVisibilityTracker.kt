package com.kuts.klaf.mnemonic

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.lib.lokdroid.core.logD

class AndroidApplicationVisibilityTracker(
    application: Application,
    private val diagnostics: AndroidMnemonicGenerationDiagnostics,
) : Application.ActivityLifecycleCallbacks {

    @Volatile
    var isApplicationVisible: Boolean = false
        private set

    private var startedActivityCount = 0
    private var configurationChangeCount = 0
    private val visibilityListenerLock = Any()
    private val visibilityListeners = mutableSetOf<(Boolean) -> Unit>()

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    fun addVisibilityListener(listener: (Boolean) -> Unit) {
        synchronized(visibilityListenerLock) {
            visibilityListeners += listener
        }
    }

    override fun onActivityStarted(activity: Activity) {
        val wasVisible = isApplicationVisible
        if (configurationChangeCount > 0) {
            configurationChangeCount--
        } else {
            startedActivityCount++
        }
        isApplicationVisible = true

        if (!wasVisible) {
            logD(
                "Mnemonic diagnostics: app visible: activity=${activity::class.simpleName}, " +
                    "startedActivities=$startedActivityCount; ${diagnostics.snapshot()}",
            )
            notifyVisibilityChanged(isVisible = true)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.isChangingConfigurations) {
            configurationChangeCount++
        } else {
            startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
            isApplicationVisible = startedActivityCount > 0
            if (!isApplicationVisible) {
                logD(
                    "Mnemonic diagnostics: app backgrounded: activity=${activity::class.simpleName}, " +
                        "startedActivities=$startedActivityCount; ${diagnostics.snapshot()}",
                )
                notifyVisibilityChanged(isVisible = false)
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun notifyVisibilityChanged(isVisible: Boolean) {
        val listeners = synchronized(visibilityListenerLock) {
            visibilityListeners.toList()
        }
        listeners.forEach { listener ->
            listener(isVisible)
        }
    }
}
