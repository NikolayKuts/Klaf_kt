package com.kuts.domain.useCases

import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.domain.managers.IKlafServerConnectionManager
import kotlinx.coroutines.flow.StateFlow

class ObserveKlafServerConnectionStateUseCase(
    private val klafServerConnectionManager: IKlafServerConnectionManager,
) {

    operator fun invoke(): StateFlow<KlafServerConnectionState> {
        return klafServerConnectionManager.state
    }
}
