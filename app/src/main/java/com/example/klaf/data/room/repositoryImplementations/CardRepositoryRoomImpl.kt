package com.example.klaf.data.room.repositoryImplementations

import androidx.lifecycle.LiveData
import com.example.klaf.data.room.databases.KlafRoomDatabase
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import kotlinx.coroutines.Dispatchers
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
        TODO("Not yet implemented")
    }

    override suspend fun getCardById(cardId: Int): Card {
        TODO("Not yet implemented")
    }

    override fun getCardsByDeckId(deckId: Int): LiveData<List<Card>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCard(cardId: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun removeCardsOfDeck(deckId: Int) {
        withContext(Dispatchers.IO) { roomDatabase.cardDao().deleteCardsByDeckId(deckId = deckId) }
    }

}