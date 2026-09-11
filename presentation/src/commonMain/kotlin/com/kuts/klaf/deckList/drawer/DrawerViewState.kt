package com.kuts.klaf.deckList.drawer

import com.kuts.domain.entities.AgentDriverConnectionState

data class DrawerViewState(
    val signedIn: Boolean,
    val userEmail: String?,
    val agentDriverConnectionState: AgentDriverConnectionState = AgentDriverConnectionState.Disconnected,
)
