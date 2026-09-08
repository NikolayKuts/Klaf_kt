package com.kuts.klaf.mnemonic

import android.app.Activity
import android.app.Application
import android.os.Bundle

class AndroidApplicationVisibilityTracker(
    application: Application,
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
        if (configurationChangeCount > 0) {
            configurationChangeCount--
        } else {
            startedActivityCount++
        }
        isApplicationVisible = true
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity.isChangingConfigurations) {
            configurationChangeCount++
        } else {
            startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
            isApplicationVisible = startedActivityCount > 0
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
