package com.example.domain.useCases

import com.example.domain.common.DataSynchronizationState
import com.example.domain.repositories.ObservationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDataSynchronizationProgressStateUseCase @Inject constructor(
    private val dataSynchronizationObserver: ObservationRepository<DataSynchronizationState>
) {

    operator fun invoke(): Flow<DataSynchronizationState> = dataSynchronizationObserver.observe()
}