package com.kuts.klaf.firestore.repositoryImplementations

import com.kuts.domain.repositories.ICrashlyticsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.lib.lokdroid.core.logE

class CrashlyticsRepositoryFirebase(
    private val firebaseCrashlytics: FirebaseCrashlytics
) : ICrashlyticsRepository {

    override fun report(exception: Throwable) {
        logE("Crashlytics report: ${exception.message}\n${exception.stackTraceToString()}")
        firebaseCrashlytics.recordException(exception)
    }
}
