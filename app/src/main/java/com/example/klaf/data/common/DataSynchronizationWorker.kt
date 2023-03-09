package com.example.klaf.data.common

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.asFlow
import androidx.work.*
import com.example.domain.common.ifTrue
import com.example.domain.useCases.SynchronizeLocalAndRemoteDataUseCase
import com.example.klaf.data.common.DataSynchronizationState.*
import com.example.klaf.data.common.notifications.DataSynchronizationNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@HiltWorker
class DataSynchronizationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val synchronizeLocalAndRemoteData: SynchronizeLocalAndRemoteDataUseCase,
    private val dataSynchronizationNotifier: DataSynchronizationNotifier,
) : CoroutineWorker(appContext = appContext, params = params) {

    companion object {

        private const val UNIQUE_WORK_NAME = "data_synchronization"
        private const val WORK_REQUEST_TAG = "request_tag"
        private const val PROGRESS_STATE_KEY = "sync_progress_key"

        private const val NOTIFICATION_ID = 43523

        fun WorkManager.getDataSynchronizationProgressState(): Flow<DataSynchronizationState> {
            val progressState = getWorkInfosByTagLiveData(WORK_REQUEST_TAG)
                .asFlow()
                .distinctUntilChanged()

            var wasRunning = false

            return progressState.map { workInfos ->
                val workInfo = workInfos.firstOrNull()
                val synchronizationData = workInfo?.progress?.getString(PROGRESS_STATE_KEY)
                val isWorkFinished = workInfos.firstOrNull()?.state?.isFinished ?: false

                (workInfo?.state == WorkInfo.State.RUNNING).ifTrue { wasRunning = true }

                when {
                    isWorkFinished && wasRunning -> {
                        wasRunning = false
                        FinishedState
                    }
                    synchronizationData != null -> {
                        wasRunning = true
                        SynchronizingState(synchronizationData = synchronizationData)
                    }
                    !isWorkFinished && wasRunning -> {
                        SynchronizingState(synchronizationData = "")
                    }
                    else -> {
                        wasRunning = false
                        UncertainState
                    }
                }
            }
        }

        fun WorkManager.performDataSynchronization() {
            this.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                makeRequest()
            )
        }

        private fun makeRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<DataSynchronizationWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(WORK_REQUEST_TAG)
                .build()
        }
    }

    override suspend fun doWork(): Result = try {
        synchronizeLocalAndRemoteData()
            .catch { TODO("implement error handling")}
            .collect { progress ->
                setProgress(workDataOf(PROGRESS_STATE_KEY to progress))
            }
        Result.success()
    } catch (exception: Exception) {
        Result.failure()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NOTIFICATION_ID,
        dataSynchronizationNotifier.createNotification()
    )
}