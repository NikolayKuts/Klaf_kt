package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.common.ifNull
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.coroutines.withContext

class CreateInterimDeckUseCase(
    private val deckRepository: IDeckRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke() {
        withContext(context = coroutineContextProvider.io) {
            val interimDeck = deckRepository.getDeckById(deckId = Deck.INTERIM_DECK_ID)

            interimDeck.ifNull {
                localStorageTransactionRepository.performWithTransaction {
                    deckRepository.insertDeck(
                        deck = Deck(
                            name = Deck.INTERIM_DECK_NAME,
                            creationDate = getCurrentDateAsLong(),
                            id = Deck.INTERIM_DECK_ID
                        )
                    )

                    localStorageSaveVersionRepository.insertVersion(version = StorageSaveVersion())
                }
            }
        }
    }
}
