package com.kuts.klaf.deckList.conflictResolution

import com.kuts.domain.common.ConflictResolutionAction
import com.kuts.domain.common.ConflictResolutionDecision
import com.kuts.klaf.deckList.common.AccountSyncOutcome
import com.kuts.klaf.server.contract.SyncResponse

data class ConflictDestination(
    val syncId: String,
    val name: String,
)

data class AccountConflictSnapshot(
    val accountEmail: String,
    val response: SyncResponse,
    val destinations: List<ConflictDestination> = emptyList(),
    val deckNames: Map<String, String> = emptyMap(),
    val cardNames: Map<String, String> = emptyMap(),
)

interface AccountConflictGateway {

    suspend fun current(): AccountConflictSnapshot?

    suspend fun resolve(accountEmail: String, action: ConflictResolutionAction): AccountSyncOutcome

    suspend fun resolveSelected(accountEmail: String, decisions: List<ConflictResolutionDecision>): AccountSyncOutcome

    suspend fun resolveMovedCard(
        accountEmail: String,
        targetDeckSyncId: String? = null,
        newDeckName: String? = null,
    ): AccountSyncOutcome
}
