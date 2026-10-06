package com.kuts.klaf.deckList.drawer

import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.domain.managers.AccountPendingEnrollment

data class DrawerViewState(
    val signedIn: Boolean,
    val userEmail: String?,
    val klafServerConnectionState: KlafServerConnectionState = KlafServerConnectionState.Disconnected,
    val canDeleteAccount: Boolean = true,
    val canSignOut: Boolean = true,
    val pendingEnrollment: AccountPendingEnrollment? = null,
)
