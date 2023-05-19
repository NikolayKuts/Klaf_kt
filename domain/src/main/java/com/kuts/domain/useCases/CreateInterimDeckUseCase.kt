package com.kuts.domain.useCases

import com.kuts.domain.common.LocalDeckRepositoryImp
import com.kuts.domain.common.LocalStorageSaveVersionRepositoryImp
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.common.ifNull
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.DeckRepository
import com.kuts.domain.repositories.StorageSaveVersionRepository
import com.kuts.domain.repositories.StorageTransactionRepository
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

                    localStorageSaveVersionRepository.insertVersion(version = StorageSaveVersion())
                }
            }
        }
    }
}