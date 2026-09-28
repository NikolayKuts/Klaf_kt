package com.kuts.klaf

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

private const val ISOLATED_APPLICATION_ID = "com.kuts.klaf.remote.storage.test"

/** Fail before an activity or test body can touch ordinary application data. */
class IsolatedStorageTestRule : ExternalResource() {

    override fun before() {
        val applicationId = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        check(applicationId == ISOLATED_APPLICATION_ID) {
            "Use -PklafAndroidTestBuildType=remoteStorageTest; ordinary application tests are not allowed"
        }
    }
}
