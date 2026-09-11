package com.kuts.klaf.networking.agentDriver

internal enum class AgentDriverConnectionStatus {
    Connected,
    Connecting,
    Disconnected,
    ResumeAvailable,
}

internal enum class AgentDriverConnectionAction {
    UseCurrent,
    Connect,
    Resume,
    AwaitRecovery,
}

internal fun AgentDriverConnectionStatus.nextAction(
    automaticReconnectActive: Boolean = false,
): AgentDriverConnectionAction {
    return when (this) {
        AgentDriverConnectionStatus.Connected -> AgentDriverConnectionAction.UseCurrent
        AgentDriverConnectionStatus.Connecting -> AgentDriverConnectionAction.AwaitRecovery
        AgentDriverConnectionStatus.Disconnected -> if (automaticReconnectActive) {
            AgentDriverConnectionAction.AwaitRecovery
        } else {
            AgentDriverConnectionAction.Connect
        }

        AgentDriverConnectionStatus.ResumeAvailable -> if (automaticReconnectActive) {
            AgentDriverConnectionAction.AwaitRecovery
        } else {
            AgentDriverConnectionAction.Resume
        }
    }
}
