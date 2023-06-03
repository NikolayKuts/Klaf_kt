package com.kuts.klaf.data.common

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.asFlow
import androidx.work.*
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.SynchronizeLocalAndRemoteDataUseCase
import com.kuts.klaf.data.common.DataSynchronizationState.*
import com.kuts.klaf.data.common.notifications.DataSynchronizationNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.*

@HiltWorker
class DataSynchronizationWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val synchronizeLocalAndRemoteData: SynchronizeLocalAndRemoteDataUseCase,
    private val dataSynchronizationNotifier: DataSynchronizationNotifier,
    private val crashlytics: CrashlyticsRepository,
) : CoroutineWorker(appContext = appContext, params = params) {

    companion object {

        private const val UNIQUE_WORK_NAME = "data_synchronization"
        private const val WORK_REQUEST_TAG = "request_tag"
        private const val PROGRESS_STATE_KEY = "sync_progress_key"
        private const val EMPTY_SYNCHRONIZING_DATA = ""

        private const val NOTIFICATION_ID = 43523

        fun WorkManager.getDataSynchronizationProgressState(): Flow<DataSynchronizationState> {
            val progressState = getWorkInfosByTagLiveData(WORK_REQUEST_TAG)
                .asFlow()
                .distinctUntilChanged()
            var wasRunning = false

            return progressState.map { workInfos ->
                val workInfo = workInfos.firstOrNull()
                val workInfoState = workInfo?.state
                val synchronizationData =
                    workInfo?.progress?.getString(PROGRESS_STATE_KEY) ?: EMPTY_SYNCHRONIZING_DATA

                val synchronizationState = when {
                    workInfoState == WorkInfo.State.RUNNING -> {
                        Synchronizing(synchronizationData = synchronizationData)
                    }
                    workInfoState == WorkInfo.State.SUCCEEDED && wasRunning -> {
                        SuccessfullyFinished
                    }
                    workInfoState.isFailed && wasRunning -> Failed
                    else -> Uncertain
                }

                wasRunning = workInfoState == WorkInfo.State.RUNNING
                synchronizationState
            }
        }

        fun WorkManager.performDataSynchronization(): UUID = makeRequest().also { request ->
            enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }.id

        private fun makeRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<DataSynchronizationWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(WORK_REQUEST_TAG)
                .build()
        }

        private val WorkInfo.State?.isFailed: Boolean
            get() = when (this) {
                WorkInfo.State.FAILED, WorkInfo.State.BLOCKED, WorkInfo.State.CANCELLED -> true
                else -> false
            }
    }

    override suspend fun doWork(): Result = try {

        delay(5000) ///////////////

        synchronizeLocalAndRemoteData().collect { progress ->
            setProgress(workDataOf(PROGRESS_STATE_KEY to progress))
        }
        Result.success()
    } catch (exception: Throwable) {
        crashlytics.report(exception = exception)
        Result.failure()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NOTIFICATION_ID,
        dataSynchronizationNotifier.createNotification()
    )
}