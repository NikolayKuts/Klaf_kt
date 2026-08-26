package com.kuts.klaf.networking.agentDriver

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.AgentDriverConnectionState
import com.kuts.domain.managers.IAgentDriverConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.agentdriver.project.ktorclient.external.AssistantClientConnectionState
import org.agentdriver.project.ktorclient.external.AssistantClientDisconnectCause

class AgentDriverConnectionManager(
    private val agentDriverSession: AgentDriverSession,
    coroutineContextProvider: ICoroutineContextProvider,
) : IAgentDriverConnectionManager {

    override val state = MutableStateFlow<AgentDriverConnectionState>(
        value = AgentDriverConnectionState.Disconnected,
    )

    private val scope = CoroutineScope(coroutineContextProvider.io + SupervisorJob())

    init {
        observeConnectionState()
    }

    override suspend fun switchOn() {
        agentDriverSession.switchOn()
    }

    override suspend fun switchOff() {
        agentDriverSession.switchOff()
    }

    private fun observeConnectionState() {
        scope.launch {
            agentDriverSession.connectionState.collect { connectionState ->
                state.value = connectionState.toAgentDriverConnectionState()
            }
        }
    }

    private fun AssistantClientConnectionState.toAgentDriverConnectionState(): AgentDriverConnectionState {
        return when (this) {
            is AssistantClientConnectionState.Connected -> AgentDriverConnectionState.Ready
            is AssistantClientConnectionState.Connecting -> AgentDriverConnectionState.Reconnecting(
                attempt = attempt,
            )
            is AssistantClientConnectionState.ResumeAvailable -> cause.toAgentDriverConnectionState()
            is AssistantClientConnectionState.Disconnected -> cause.toAgentDriverConnectionState()
        }
    }

    private fun AssistantClientDisconnectCause.toAgentDriverConnectionState(): AgentDriverConnectionState {
        return when {
            allowsAutomaticReconnect -> AgentDriverConnectionState.Reconnecting()
            isFault -> AgentDriverConnectionState.Error(message = toShortAgentDriverMessage())
            else -> AgentDriverConnectionState.Disconnected
        }
    }
}
