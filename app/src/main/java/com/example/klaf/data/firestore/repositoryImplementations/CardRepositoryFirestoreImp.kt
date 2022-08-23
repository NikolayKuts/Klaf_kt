package com.example.klaf.data.firestore.repositoryImplementations

import androidx.lifecycle.LiveData
import com.example.klaf.data.firestore.mapToFirestoreEntity
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CardRepositoryFirestoreImp @Inject constructor(
    private val firestore: FirebaseFirestore
) : CardRepository {

    override suspend fun getObservableCardQuantityByDeckId(deckId: Int): LiveData<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun getCardQuantityByDeckId(deckId: Int): Int {
        TODO("Not yet implemented")
    }

    override suspend fun insertCard(card: Card) {
        firestore.collection("old_klaf_collection")
            .document("cards")
            .collection("card_collection")
            .document(card.id.toString())
            .set(card.mapToFirestoreEntity())
            .await()
    }

    override fun getObservableCardById(cardId: Int): Flow<Card?> {
        TODO("Not yet implemented")
    }

    override fun getCardsByDeckId(deckId: Int): Flow<List<Card>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCard(cardId: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun removeCardsOfDeck(deckId: Int) {
        TODO("Not yet implemented")
    }
}