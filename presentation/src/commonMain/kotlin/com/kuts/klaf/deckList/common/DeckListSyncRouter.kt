package com.kuts.klaf.deckList.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

enum class AccountSyncOutcome { APPLIED, NEEDS_RESOLUTION }

enum class DeckListSyncStart { NEEDS_SIGN_IN, NETWORK_UNAVAILABLE, LEGACY_STARTED, APPLIED, NEEDS_RESOLUTION }

interface AccountDeckListGateway {

    val selectedAccountEmail: Flow<String?>

    suspend fun synchronize(): AccountSyncOutcome

    suspend fun signOut()
}

class DeckListSyncRouter(
    private val accountGateway: AccountDeckListGateway?,
    private val legacySignedIn: () -> Boolean,
    private val networkConnected: () -> Boolean,
    private val startLegacyWorker: () -> Unit,
) {

    suspend fun synchronize(): DeckListSyncStart {
        if (accountGateway == null) {
            if (!legacySignedIn()) return DeckListSyncStart.NEEDS_SIGN_IN
            if (!networkConnected()) return DeckListSyncStart.NETWORK_UNAVAILABLE
            startLegacyWorker()
            return DeckListSyncStart.LEGACY_STARTED
        }
        if (accountGateway.selectedAccountEmail.first() == null) return DeckListSyncStart.NEEDS_SIGN_IN
        // Localhost/USB reverse may work without an Android active network.
        return when (accountGateway.synchronize()) {
            AccountSyncOutcome.APPLIED -> DeckListSyncStart.APPLIED
            AccountSyncOutcome.NEEDS_RESOLUTION -> DeckListSyncStart.NEEDS_RESOLUTION
        }
    }
}
