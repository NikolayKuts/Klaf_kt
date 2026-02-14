package com.kuts.klaf.data.common

import androidx.work.WorkManager
import com.kuts.domain.common.IDataSynchronizationState
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.klaf.data.common.IDataSynchronizationState.Failed
import com.kuts.klaf.data.common.IDataSynchronizationState.Initial
import com.kuts.klaf.data.common.IDataSynchronizationState.Synchronizing
import com.kuts.klaf.data.common.IDataSynchronizationState.SuccessfullyFinished
import com.kuts.klaf.data.common.IDataSynchronizationState.Uncertain
import com.kuts.klaf.data.common.AppReopeningWorker.Companion.scheduleAppReopening
import com.kuts.klaf.data.common.DataSynchronizationWorker.Companion.getDataSynchronizationProgressState
import com.kuts.klaf.data.common.DataSynchronizationWorker.Companion.performDataSynchronization
import com.kuts.klaf.data.common.DeckRepetitionReminderChecker.Companion.scheduleDeckRepetitionChecking
import com.kuts.klaf.data.common.notifications.NotificationChannelInitializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppMaintenanceManager @Inject constructor(
    private val workManager: WorkManager,
    private val notificationChannelInitializer: NotificationChannelInitializer,
    private val networkConnectivity: NetworkConnectivity,
) : IAppMaintenanceManager {

    override fun initialize() {
        notificationChannelInitializer.initialize()
    }

    override fun isNetworkConnected(): Boolean {
        return networkConnectivity.isNetworkConnected()
    }

    override fun observeDataSynchronizationState(): Flow<IDataSynchronizationState> {
        return workManager.getDataSynchronizationProgressState().map { state ->
            when (state) {
                Failed -> IDataSynchronizationState.Failed
                Initial -> IDataSynchronizationState.Initial
                is Synchronizing -> {
                    IDataSynchronizationState.Synchronizing(
                        synchronizationData = state.synchronizationData,
                    )
                }

                SuccessfullyFinished -> IDataSynchronizationState.SuccessfullyFinished
                Uncertain -> IDataSynchronizationState.Uncertain
            }
        }
    }

    override fun performDataSynchronization() {
        workManager.performDataSynchronization()
    }

    override fun scheduleAppReopening() {
        workManager.scheduleAppReopening()
    }

    override fun scheduleDeckRepetitionChecking() {
        workManager.scheduleDeckRepetitionChecking()
    }
}
