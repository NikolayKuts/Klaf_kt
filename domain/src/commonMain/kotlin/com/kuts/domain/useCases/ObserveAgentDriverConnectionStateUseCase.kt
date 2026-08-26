package com.kuts.domain.useCases

import com.kuts.domain.entities.AgentDriverConnectionState
import com.kuts.domain.managers.IAgentDriverConnectionManager
import kotlinx.coroutines.flow.StateFlow

class ObserveAgentDriverConnectionStateUseCase(
    private val agentDriverConnectionManager: IAgentDriverConnectionManager,
) {

    operator fun invoke(): StateFlow<AgentDriverConnectionState> {
        return agentDriverConnectionManager.state
    }
}
