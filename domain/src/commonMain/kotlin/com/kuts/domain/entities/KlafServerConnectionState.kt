package com.kuts.domain.entities

sealed interface KlafServerConnectionState {

    object Disconnected : KlafServerConnectionState
    data class Reconnecting(val attempt: Int? = null) : KlafServerConnectionState
    object Ready : KlafServerConnectionState
    data class Error(val message: String) : KlafServerConnectionState
}

val KlafServerConnectionState.canSendRequests: Boolean
    get() = this == KlafServerConnectionState.Ready
