package com.example.klaf.data.common

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.asFlow
import androidx.work.*
import com.example.klaf.data.common.DataSynchronizationState.*
import com.example.klaf.domain.common.ifTrue
import com.example.klaf.domain.useCases.SynchronizeLocalAndRemoteDataUseCase
import com.example.klaf.presentation.deckList.dataSynchronization.DataSynchronizationNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
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

        private const val PERCENTAGE_LIMIT = 97
        private const val NOTIFICATION_ID = 43523
        private const val UNDEFINED_VALUE = -1

        fun WorkManager.getDataSynchronizationProgressState(): Flow<DataSynchronizationState> {
            val progressState = getWorkInfosByTagLiveData(WORK_REQUEST_TAG)
                .asFlow()
                .distinctUntilChanged()

            return progressState.map { workInfos ->
                val retrievedProgress: WorkInfo?.() -> Int? = {
                    this?.progress?.getInt(PROGRESS_STATE_KEY, UNDEFINED_VALUE)
                }

                val progress = workInfos.firstOrNull { workInfo: WorkInfo? ->
                    workInfo.retrievedProgress() != UNDEFINED_VALUE
                }.retrievedProgress()

                when {
                    progress == null -> UncertainState
                    progress < PERCENTAGE_LIMIT -> SynchronizingState(progress = progress)
                    else -> FinishedState
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
        synchronizeLocalAndRemoteData().distinctUntilChanged()
            .collect { progress ->
                val info = ForegroundInfo(
                    NOTIFICATION_ID,
                    dataSynchronizationNotifier.createNotification(progress = progress)
                )

                (progress % 4 == 0).ifTrue { setForeground(info) }
                setProgress(workDataOf(PROGRESS_STATE_KEY to progress))
            }

        Result.success()
    } catch (exception: Exception) {
        Result.failure()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NOTIFICATION_ID,
        dataSynchronizationNotifier.createNotification(progress = UNDEFINED_VALUE)
    )
}