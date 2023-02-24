package com.example.domain.common

sealed class DataSynchronizationState {

    object UncertainState: DataSynchronizationState()
    object InitialState : DataSynchronizationState()
    data class SynchronizingState(val synchronizationData: String) : DataSynchronizationState()
    object FinishedState : DataSynchronizationState()
}