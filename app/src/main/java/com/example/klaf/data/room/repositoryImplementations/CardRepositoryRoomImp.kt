package com.example.klaf.data.room.repositoryImplementations

import androidx.lifecycle.LiveData
import com.example.klaf.data.room.databases.KlafRoomDatabase
import com.example.klaf.data.room.entities.RoomCard
import com.example.klaf.data.room.mapToDomainEntity
import com.example.klaf.data.room.mapToRoomEntity
import com.example.klaf.domain.common.simplifiedItemMap
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepositoryRoomImp @Inject constructor(
    private val roomDatabase: KlafRoomDatabase,
) : CardRepository {

    override suspend fun fetchObservableCardQuantityByDeckId(deckId: Int): LiveData<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchCardQuantityByDeckId(deckId: Int): Int {
        return roomDatabase.cardDao().getCardQuantityInDeckAsInt(deckId = deckId)
    }

    override suspend fun fetchAllCards(): List<Card> {
        return roomDatabase.cardDao()
            .getAllCards()
            .map { roomCard -> roomCard.mapToDomainEntity() }
    }

    override suspend fun insertCard(card: Card) {
        roomDatabase.cardDao().insetCard(card = card.mapToRoomEntity())
    }

    override fun fetchObservableCardById(cardId: Int): Flow<Card?> {
        return roomDatabase.cardDao()
            .getObservableCardById(cardId = cardId)
            .map { roomCard: RoomCard? -> roomCard?.mapToDomainEntity() }
    }

    override fun fetchObservableCardsByDeckId(deckId: Int): Flow<List<Card>> {
        return roomDatabase.cardDao()
            .getObservableCardsByDeckId(deckId = deckId)
            .simplifiedItemMap { roomCard: RoomCard -> roomCard.mapToDomainEntity() }
    }

    override fun fetchCardsByDeckId(deckId: Int): List<Card> {
        return roomDatabase.cardDao()
            .getCardsByDeckId(deckId = deckId)
            .map { roomCard -> roomCard.mapToDomainEntity() }
    }

    override suspend fun deleteCard(cardId: Int) {
        roomDatabase.cardDao().deleteCard(cardId = cardId)
    }

    override suspend fun removeCardsOfDeck(deckId: Int) {
        roomDatabase.cardDao().deleteCardsByDeckId(deckId = deckId)
    }
}