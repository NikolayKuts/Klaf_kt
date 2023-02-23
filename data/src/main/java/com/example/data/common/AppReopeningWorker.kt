package com.example.data.common

import android.app.Notification
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AppReopeningWorker @AssistedInject constructor(
    @Assisted application: Context,
    @Assisted workerParams: WorkerParameters,
    @AppRestartNotification private val foregroundNotification: Notification,
) : CoroutineWorker(application, workerParams) {

    companion object {

        private const val NOTIFICATION_ID = 43235452
        private const val UNIQUE_RESTART_WORK_NAME = "unique_restart_work_Name"

        fun WorkManager.scheduleAppReopening() {
            enqueueUniqueWork(
                UNIQUE_RESTART_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                makeWorkRequest()
            )
        }

        private fun makeWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<AppReopeningWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NOTIFICATION_ID,
        foregroundNotification,
    )
}