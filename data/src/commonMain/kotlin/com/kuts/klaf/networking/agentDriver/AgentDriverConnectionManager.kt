package com.kuts.klaf.networking.agentDriver

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.AgentDriverConnectionState
import com.kuts.domain.managers.IAgentDriverConnectionManager
import com.lib.lokdroid.core.logD
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
                connectionState.logTransition()
            }
        }
    }

    /**
     * Says what the connection is doing, and what an opened session actually offers.
     *
     * The session block goes out on every transition into [AssistantClientConnectionState.Connected]
     * rather than only the first: a reconnect opens a new session, which the server is free to back
     * with a different provider or model than the one the previous answers came from.
     */
    private fun AssistantClientConnectionState.logTransition() {
        when (this) {
            is AssistantClientConnectionState.Connected -> {
                logD(session.describeForLog(endpoint = agentDriverSession.endpoint))
            }

            is AssistantClientConnectionState.Connecting -> {
                val kind = if (resuming) "resuming the previous session" else "opening a new session"

                logD("AgentDriver connecting: $kind, attempt $attempt")
            }

            is AssistantClientConnectionState.ResumeAvailable -> {
                logD(
                    "AgentDriver session is resumable for ${session.reconnectGracePeriodSeconds}s: " +
                        cause.toShortAgentDriverMessage(),
                )
            }

            is AssistantClientConnectionState.Disconnected -> {
                logD("AgentDriver disconnected: ${cause.toShortAgentDriverMessage()}")
            }
        }
    }

    private fun AssistantClientConnectionState.toAgentDriverConnectionState(): AgentDriverConnectionState {
        return when (this) {
            is AssistantClientConnectionState.Connected -> AgentDriverConnectionState.Ready
            is AssistantClientConnectionState.Connecting -> AgentDriverConnectionState.Reconnecting(attempt = attempt)
            is AssistantClientConnectionState.ResumeAvailable ->
                cause.toAgentDriverConnectionState(automaticReconnectActive = automaticReconnectActive)
            is AssistantClientConnectionState.Disconnected ->
                cause.toAgentDriverConnectionState(automaticReconnectActive = automaticReconnectActive)
        }
    }

    private fun AssistantClientDisconnectCause.toAgentDriverConnectionState(
        automaticReconnectActive: Boolean,
    ): AgentDriverConnectionState {
        return when {
            automaticReconnectActive -> AgentDriverConnectionState.Reconnecting()
            isFault -> AgentDriverConnectionState.Error(message = toShortAgentDriverMessage())
            else -> AgentDriverConnectionState.Disconnected
        }
    }
}
