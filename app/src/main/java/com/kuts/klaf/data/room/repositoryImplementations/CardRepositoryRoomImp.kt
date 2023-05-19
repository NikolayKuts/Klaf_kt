package com.kuts.klaf.data.room.repositoryImplementations

import com.kuts.domain.common.simplifiedItemMap
import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.CardRepository
import com.kuts.klaf.data.room.databases.KlafRoomDatabase
import com.kuts.klaf.data.room.entities.RoomCard
import com.kuts.klaf.data.room.toDomainEntity
import com.kuts.klaf.data.room.toRoomEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepositoryRoomImp @Inject constructor(
    private val roomDatabase: KlafRoomDatabase,
) : CardRepository {

    override suspend fun fetchCardQuantityByDeckId(deckId: Int): Int {
        return roomDatabase.cardDao().getCardQuantityInDeckAsInt(deckId = deckId)
    }

    override suspend fun fetchAllCards(): List<Card> {
        return roomDatabase.cardDao()
            .getAllCards()
            .map { roomCard -> roomCard.toDomainEntity() }
    }

    override suspend fun insertCard(card: Card) {
        roomDatabase.cardDao().insetCard(card = card.toRoomEntity())
    }

    override fun fetchObservableCardById(cardId: Int): Flow<Card?> {
        return roomDatabase.cardDao()
            .getObservableCardById(cardId = cardId)
            .map { roomCard: RoomCard? -> roomCard?.toDomainEntity() }
    }

    override fun fetchObservableCardsByDeckId(deckId: Int): Flow<List<Card>> {
        return roomDatabase.cardDao()
            .getObservableCardsByDeckId(deckId = deckId)
            .simplifiedItemMap { roomCard: RoomCard -> roomCard.toDomainEntity() }
    }

    override fun fetchCardsByDeckId(deckId: Int): List<Card> {
        return roomDatabase.cardDao()
            .getCardsByDeckId(deckId = deckId)
            .map { roomCard -> roomCard.toDomainEntity() }
    }

    override suspend fun deleteCard(cardId: Int) {
        roomDatabase.cardDao().deleteCard(cardId = cardId)
    }

    override suspend fun removeCardsOfDeck(deckId: Int) {
        roomDatabase.cardDao().deleteCardsByDeckId(deckId = deckId)
    }
}