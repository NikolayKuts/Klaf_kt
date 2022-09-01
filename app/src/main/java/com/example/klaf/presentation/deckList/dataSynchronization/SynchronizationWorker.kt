package com.example.klaf.presentation.deckList.dataSynchronization

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.klaf.domain.useCases.SynchronizeLocalAndRemoteDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SynchronizationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val synchronizeLocalAndRemoteData: SynchronizeLocalAndRemoteDataUseCase,
) : CoroutineWorker(appContext = appContext, params = params) {

    companion object {

        private const val UNIQUE_WORK_NAME = "data_synchronization"

        fun WorkManager.performSynchronization() {
            this.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                makeRequest()
            )
        }

        private fun makeRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<SynchronizationWorker>().build()
        }
    }

    override suspend fun doWork(): Result {
        return try {
            synchronizeLocalAndRemoteData()
            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        }
    }
}