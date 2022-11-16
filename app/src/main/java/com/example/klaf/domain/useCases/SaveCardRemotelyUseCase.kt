package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryFirestoreImp
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SaveCardRemotelyUseCase @Inject constructor(
    @CardRepositoryFirestoreImp
    private val cardRepository: CardRepository,
) {

    suspend operator fun invoke(card: Card) {
        withContext(Dispatchers.IO) { cardRepository.insertCard(card = card) }
    }
}