package com.example.domain.useCases

import com.example.domain.common.LocalDeckRepositoryImp
import com.example.domain.common.LocalStorageSaveVersionRepositoryImp
import com.example.domain.common.getCurrentDateAsLong
import com.example.domain.common.ifNull
import com.example.domain.entities.Deck
import com.example.domain.repositories.DeckRepository
import com.example.domain.repositories.StorageSaveVersionRepository
import com.example.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateInterimDeckUseCase @Inject constructor(
    @LocalDeckRepositoryImp
    private val deckRepository: DeckRepository,
    @LocalStorageSaveVersionRepositoryImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val localStorageTransactionRepository: StorageTransactionRepository,
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

                    localStorageSaveVersionRepository.increaseVersion()
                }
            }
        }
    }
}