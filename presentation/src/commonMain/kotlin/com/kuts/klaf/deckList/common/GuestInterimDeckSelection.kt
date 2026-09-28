package com.kuts.klaf.deckList.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

internal fun Flow<String?>.ensureGuestInterimDeck(
    ensureDeck: suspend () -> Unit,
    reportFailure: (Exception) -> Unit,
): Flow<String?> = distinctUntilChanged().onEach { email ->
    if (email == null) {
        try {
            ensureDeck()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            reportFailure(failure)
        }
    }
}
