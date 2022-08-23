package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryFirestoreImp
import com.example.klaf.di.CardRepositoryRoomImp
import com.example.klaf.domain.repositories.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteAllCardsOfDeck @Inject constructor(
    @CardRepositoryRoomImp
    private val cardRepository: CardRepository,
) {

    suspend operator fun invoke(deckId: Int) {
        withContext(Dispatchers.IO) {
            cardRepository.removeCardsOfDeck(deckId = deckId)
        }
    }
}