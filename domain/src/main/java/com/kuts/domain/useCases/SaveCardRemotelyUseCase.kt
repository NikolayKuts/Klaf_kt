package com.kuts.domain.useCases

import com.kuts.domain.common.RemoteCardRepositoryImp
import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.CardRepository
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