package com.example.domain.useCases

import com.example.domain.common.RemoteCardRepositoryImp
import com.example.domain.entities.Card
import com.example.domain.repositories.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SaveCardRemotelyUseCase @Inject constructor(
    @RemoteCardRepositoryImp
    private val cardRepository: CardRepository,
) {

    suspend operator fun invoke(card: Card) {
        withContext(Dispatchers.IO) { cardRepository.insertCard(card = card) }
    }
}