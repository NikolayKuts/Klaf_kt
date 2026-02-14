package com.kuts.klaf.firestore.repositoryImplementations

import com.kuts.domain.repositories.ICrashlyticsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsRepositoryFirebase(
    private val firebaseCrashlytics: FirebaseCrashlytics
) : ICrashlyticsRepository {

    override fun report(exception: Throwable) {
        firebaseCrashlytics.recordException(exception)
    }
}