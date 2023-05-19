package com.kuts.klaf.data.common

sealed interface DataSynchronizationState {

    object Uncertain: DataSynchronizationState
    object Initial : DataSynchronizationState
    data class Synchronizing(val synchronizationData: String) : DataSynchronizationState
    object SuccessfullyFinished : DataSynchronizationState
    object Failed : DataSynchronizationState
}