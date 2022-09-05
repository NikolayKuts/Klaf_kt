package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryFirestoreImp
import com.example.klaf.di.CardRepositoryRoomImp
import com.example.klaf.di.DeckRepositoryFirestoreImp
import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.domain.common.ifTrue
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.presentation.common.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

    suspend operator fun invoke(): Flow<Int> = channelFlow {
        withContext(Dispatchers.IO) {
            log("synchronization")

            val localDecks = roomDeckRepository.fetchAllDecks()
            val localCards = roomCardRepository.fetchAllCards()

            val operationCount = localDecks.size + localCards.size
            var count = 1
            val progress: () -> Int = { ++count * 100 / operationCount }

            localDecks.onEach { deck ->
                launch {
                    delay((10..5000).random().toLong())

                    send(progress())
                }
            }

            localCards.onEach { card ->
                launch {
                    delay((10..5000).random().toLong())

                    send(progress())
                }
            }
        }
    }
}