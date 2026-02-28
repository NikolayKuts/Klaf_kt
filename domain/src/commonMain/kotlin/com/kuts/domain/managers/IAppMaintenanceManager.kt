package com.kuts.domain.managers

import com.kuts.domain.common.IDataSynchronizationState
import kotlinx.coroutines.flow.Flow

interface IAppMaintenanceManager {

    fun initialize()

    fun isNetworkConnected(): Boolean

    fun observeDataSynchronizationState(): Flow<IDataSynchronizationState>

    fun performDataSynchronization()

    fun scheduleAppReopening()

    fun scheduleDeckRepetitionChecking()
}
