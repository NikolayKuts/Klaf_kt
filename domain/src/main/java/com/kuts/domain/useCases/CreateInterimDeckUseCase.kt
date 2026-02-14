package com.kuts.domain.useCases

import com.kuts.domain.common.LocalDeckRepository
import com.kuts.domain.common.LocalStorageSaveVersionRepository
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.common.ifNull
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateInterimDeckUseCase @Inject constructor(
    @LocalDeckRepository
    private val deckRepository: IDeckRepository,
    @LocalStorageSaveVersionRepository
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
) {

    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
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