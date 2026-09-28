package com.kuts.klaf.di

import com.kuts.domain.managers.IAccountSession
import com.kuts.klaf.deckList.common.AccountDeckListGateway
import com.kuts.klaf.deckList.common.AccountSyncOutcome
import com.kuts.klaf.room.repositoryImplementations.ManualRoomSyncCoordinator
import com.kuts.klaf.room.repositoryImplementations.ManualSyncResult
import kotlinx.coroutines.flow.Flow

internal class RoomAccountDeckListGateway(
    private val accountSession: IAccountSession,
    private val coordinator: ManualRoomSyncCoordinator,
) : AccountDeckListGateway {

    override val selectedAccountEmail: Flow<String?> = accountSession.selectedAccountEmail

    override suspend fun synchronize(): AccountSyncOutcome = when (coordinator.synchronize()) {
        is ManualSyncResult.Applied -> AccountSyncOutcome.APPLIED
        is ManualSyncResult.NeedsResolution -> AccountSyncOutcome.NEEDS_RESOLUTION
    }

    override suspend fun signOut() = accountSession.signOut()
}
