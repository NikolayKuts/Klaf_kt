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

    init {
        application.registerActivityLifecycleCallbacks(this)
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
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
