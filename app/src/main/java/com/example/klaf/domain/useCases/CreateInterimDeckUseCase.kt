package com.example.klaf.domain.useCases

import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.di.StorageSaveVersionRepositoryRoomImp
import com.example.klaf.domain.common.getCurrentDateAsLong
import com.example.klaf.domain.common.ifNull
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import com.example.klaf.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateInterimDeckUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
    @StorageSaveVersionRepositoryRoomImp
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