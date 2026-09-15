package com.kuts.klaf.deckList.drawer

import com.kuts.domain.entities.KlafServerConnectionState

data class DrawerViewState(
    val signedIn: Boolean,
    val userEmail: String?,
    val klafServerConnectionState: KlafServerConnectionState = KlafServerConnectionState.Disconnected,
)
