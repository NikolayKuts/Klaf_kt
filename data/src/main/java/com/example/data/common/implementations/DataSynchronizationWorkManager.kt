package com.example.data.common.implementations

import androidx.work.WorkManager
import com.example.data.common.workers.DataSynchronizationWorker.Companion.getDataSynchronizationProgressState
import com.example.data.common.workers.DataSynchronizationWorker.Companion.performDataSynchronization
import com.example.domain.common.DataSynchronizationState
import com.example.domain.repositories.ObservationRepository
import com.example.domain.repositories.PerformanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DataSynchronizationWorkManager @Inject constructor(
    private val workManager: WorkManager,
) : PerformanceRepository, ObservationRepository<DataSynchronizationState> {

    override fun perform() {
        workManager.performDataSynchronization()
    }

    override fun observe(): Flow<DataSynchronizationState> {
        return workManager.getDataSynchronizationProgressState()
    }
}