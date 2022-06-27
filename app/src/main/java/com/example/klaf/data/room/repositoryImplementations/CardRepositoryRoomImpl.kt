package com.example.klaf.data.room.repositoryImplementations

import androidx.lifecycle.LiveData
import com.example.klaf.data.room.databases.KlafRoomDatabase
import com.example.klaf.data.room.entities.RoomCard
import com.example.klaf.data.room.mapToCard
import com.example.klaf.data.room.mapToRoomEntity
import com.example.klaf.domain.common.simplifiedMap
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepositoryRoomImpl @Inject constructor(
    private val roomDatabase: KlafRoomDatabase
) : CardRepository {

    override suspend fun getCardQuantityByDeckId(deckId: Int): LiveData<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun insertCard(card: Card) {
        withContext(Dispatchers.IO) {
            roomDatabase.cardDao().insetCard(card = card.mapToRoomEntity())
        }
    }

    override fun getObservableCardById(cardId: Int): Flow<Card?> {
        return roomDatabase.cardDao()
            .getObservableCardById(cardId = cardId)
            .map { roomCard: RoomCard? -> roomCard?.mapToCard() }
    }

    override fun getCardsByDeckId(deckId: Int): Flow<List<Card>> {
        return roomDatabase.cardDao()
            .getCardsByDeckId(deckId = deckId)
            .simplifiedMap { roomCard: RoomCard -> roomCard.mapToCard() }
    }

    override suspend fun deleteCard(cardId: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun removeCardsOfDeck(deckId: Int) {
        withContext(Dispatchers.IO) { roomDatabase.cardDao().deleteCardsByDeckId(deckId = deckId) }
    }
}