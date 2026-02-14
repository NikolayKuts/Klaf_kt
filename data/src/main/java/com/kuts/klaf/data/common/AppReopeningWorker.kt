package com.kuts.klaf.data.common

import android.content.Context
import android.content.ComponentName
import androidx.hilt.work.HiltWorker
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.*
import com.kuts.klaf.data.R
import com.kuts.klaf.data.common.notifications.AppRestartNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AppReopeningWorker @AssistedInject constructor(
    @Assisted application: Context,
    @Assisted workerParams: WorkerParameters,
    private val appRestartNotifier: AppRestartNotifier,
) : CoroutineWorker(application, workerParams) {

    companion object {

        private const val NOTIFICATION_ID = 43235452
        private const val UNIQUE_RESTART_WORK_NAME = "unique_restart_work_Name"
        private const val MAIN_ACTIVITY_CLASS_NAME = "com.kuts.klaf.presentation.common.MainActivity"

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
        NavDeepLinkBuilder(applicationContext)
            .setComponentName(ComponentName(applicationContext, MAIN_ACTIVITY_CLASS_NAME))
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.deckListFragment)
            .createPendingIntent()
            .send()

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        NOTIFICATION_ID,
        appRestartNotifier.createAppRestartNotification()
    )
}
