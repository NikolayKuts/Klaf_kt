package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryRoomImp
import com.example.klaf.di.StorageSaveVersionRepositoryRoomImp
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateCardUseCase @Inject constructor(
    @CardRepositoryRoomImp
    private val cardRepository: CardRepository,
    @StorageSaveVersionRepositoryRoomImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
) {

    suspend operator fun invoke(newCard: Card) {
        withContext(Dispatchers.IO) {
            cardRepository.insertCard(card = newCard)
            localStorageSaveVersionRepository.increaseVersion()
        }
    }
}