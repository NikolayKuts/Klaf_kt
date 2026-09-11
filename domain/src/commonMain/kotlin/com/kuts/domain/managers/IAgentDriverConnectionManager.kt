package com.kuts.domain.managers

import com.kuts.domain.entities.AgentDriverConnectionState
import kotlinx.coroutines.flow.StateFlow

interface IAgentDriverConnectionManager {

    val state: StateFlow<AgentDriverConnectionState>

    suspend fun retry()
}
