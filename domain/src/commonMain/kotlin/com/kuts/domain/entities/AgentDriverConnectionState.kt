package com.kuts.domain.entities

sealed interface AgentDriverConnectionState {

    object Disconnected : AgentDriverConnectionState
    data class Reconnecting(val attempt: Int? = null) : AgentDriverConnectionState
    object Ready : AgentDriverConnectionState
    data class Error(val message: String) : AgentDriverConnectionState
}

val AgentDriverConnectionState.canSendRequests: Boolean
    get() = this == AgentDriverConnectionState.Ready
