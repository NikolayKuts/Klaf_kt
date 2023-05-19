package com.kuts.klaf.data.firestore.repositoryImplementations

import com.kuts.domain.repositories.CrashlyticsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

class CrashlyticsRepositoryFirebaseImp @Inject constructor(
    private val firebaseCrashlytics: FirebaseCrashlytics
) : CrashlyticsRepository {

    override fun report(exception: Throwable) {
        firebaseCrashlytics.recordException(exception)
    }
}