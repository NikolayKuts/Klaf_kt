package com.kuts.klaf.common

import com.kuts.domain.common.IDataSynchronizationState
import com.kuts.domain.managers.IAppMaintenanceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class IosAppMaintenanceManager : IAppMaintenanceManager {
    private val state = MutableStateFlow<IDataSynchronizationState>(IDataSynchronizationState.Initial)

    override fun initialize() = Unit

    override fun isNetworkConnected(): Boolean = true

    override fun observeDataSynchronizationState(): Flow<IDataSynchronizationState> = state

    override fun performDataSynchronization() {
        state.value = IDataSynchronizationState.SuccessfullyFinished
    }

    override fun scheduleAppReopening() = Unit

    override fun scheduleDeckRepetitionChecking() = Unit
}
