package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.common.simplifiedItemMap
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.ICardRepository
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.databases.StaticRoomDatabaseSource
import com.kuts.klaf.room.entities.RoomCard
import com.kuts.klaf.room.toDomainEntity
import com.kuts.klaf.room.toRoomEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class CardRepositoryRoom(
    private val databaseSource: RoomDatabaseSource,
) : ICardRepository {

    constructor(roomDatabase: KlafRoomDatabase) : this(StaticRoomDatabaseSource(roomDatabase))

    private val roomDatabase: KlafRoomDatabase
        get() = databaseSource.current()

    override suspend fun fetchCardQuantityByDeckId(deckId: Int): Int {
        return roomDatabase.cardDao().getCardQuantityInDeckAsInt(deckId = deckId)
    }

    override suspend fun fetchAllCards(): List<Card> {
        return roomDatabase.cardDao()
            .getAllCards()
            .map { roomCard -> roomCard.toDomainEntity() }
    }

    override suspend fun insertCard(card: Card): Int {
        val cardId = roomDatabase.cardDao()
            .insetCard(card = card.toRoomEntity())
            .toInt()

        return card.id.takeIf { it > 0 } ?: cardId
    }

    override suspend fun insertCardAtPath(
        card: Card,
        rootEmailPath: String
    ) {
        TODO("Not yet implemented")
    }

    override fun fetchObservableCardById(cardId: Int): Flow<Card?> {
        return databaseSource.databases.flatMapLatest { database ->
            database.cardDao().getObservableCardById(cardId = cardId)
                .map { roomCard: RoomCard? -> roomCard?.toDomainEntity() }
        }
    }

    override fun fetchObservableCardsByDeckId(deckId: Int): Flow<List<Card>> {
        return databaseSource.databases.flatMapLatest { database ->
            database.cardDao().getObservableCardsByDeckId(deckId = deckId)
                .simplifiedItemMap { roomCard: RoomCard -> roomCard.toDomainEntity() }
        }
    }

    override suspend fun fetchCardsByDeckId(deckId: Int): List<Card> {
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

    override suspend fun checkIfCardExists(foreignWord: String): List<Deck> {
        val database = roomDatabase
        val cards = database.cardDao().getCardsByForeignWord(foreignWord = foreignWord)

        return if (cards.isEmpty()) {
            emptyList()
        } else {
            cards.mapNotNull { roomCard ->
                database.deckDao().getDeckById(deckId = roomCard.deckId)?.toDomainEntity()
            }
        }
    }
}
