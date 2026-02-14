package com.kuts.klaf.data.common

sealed interface IDataSynchronizationState {

    data object Uncertain : IDataSynchronizationState
    data object Initial : IDataSynchronizationState
    data class Synchronizing(val synchronizationData: String) : IDataSynchronizationState
    data object SuccessfullyFinished : IDataSynchronizationState
    data object Failed : IDataSynchronizationState
}
