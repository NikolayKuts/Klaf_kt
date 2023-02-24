package com.example.data.common.implementations

import androidx.work.WorkManager
import com.example.data.common.workers.AppReopeningWorker.Companion.scheduleAppReopening
import com.example.domain.repositories.PerformanceRepository
import javax.inject.Inject

class AppRestartWorkPerformer @Inject constructor(
    private val workManager: WorkManager
) : PerformanceRepository {

    override fun perform() {
        workManager.scheduleAppReopening()
    }
}