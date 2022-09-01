package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryFirestoreImp
import com.example.klaf.di.CardRepositoryRoomImp
import com.example.klaf.di.DeckRepositoryFirestoreImp
import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.presentation.common.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SynchronizeLocalAndRemoteDataUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val roomDeckRepository: DeckRepository,
    @CardRepositoryRoomImp
    private val roomCardRepository: CardRepository,
    @DeckRepositoryFirestoreImp
    private val firestoreDeckRepository: DeckRepository,
    @CardRepositoryFirestoreImp
    private val firestoreCardRepository: CardRepository,
) {

    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
            val localDecks = roomDeckRepository.fetchAllDecks()

            localDecks.onEach { deck ->
                launch {
                    firestoreDeckRepository.insertDeck(deck = deck)

                    val localCards = roomCardRepository.fetchCardsByDeckId(deckId = deck.id)

                    localCards.onEach { card ->
                        launch { firestoreCardRepository.insertCard(card = card) }
                    }
                }
            }
        }
    }

}