package com.example.klaf.data.firestore.repositoryImplementations

import androidx.lifecycle.LiveData
import com.example.klaf.data.firestore.MAIN_COLLECTION_NAME
import com.example.klaf.data.firestore.entities.FirestoreCard
import com.example.klaf.data.firestore.mapToDomainEntity
import com.example.klaf.data.firestore.mapToFirestoreEntity
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CardRepositoryFirestoreImp @Inject constructor(
    private val firestore: FirebaseFirestore,
) : CardRepository {

    companion object {

        private const val CARD_DOCUMENT_NAME = "cards"
        private const val CARD_SUB_COLLECTION_NAME = "card_collection"
    }

    override suspend fun fetchObservableCardQuantityByDeckId(deckId: Int): LiveData<Int> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchCardQuantityByDeckId(deckId: Int): Int {
        TODO("Not yet implemented")
    }

    override suspend fun fetchAllCards(): List<Card> {
        return getCardSubCollection()
            .get()
            .await()
            .documents
            .mapNotNull { documentSnapshot -> documentSnapshot.toObject<FirestoreCard>() }
            .map { firestoreCard -> firestoreCard.mapToDomainEntity() }
    }

    override suspend fun insertCard(card: Card) {
//        delay((1000..3000).random().toLong())
        getCardSubCollection()
            .document(card.id.toString())
            .set(card.mapToFirestoreEntity())
            .await()
    }

    override fun fetchObservableCardById(cardId: Int): Flow<Card?> {
        TODO("Not yet implemented")
    }

    override fun fetchObservableCardsByDeckId(deckId: Int): Flow<List<Card>> {
        TODO("Not yet implemented")
    }

    override fun fetchCardsByDeckId(deckId: Int): List<Card> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCard(cardId: Int) {
        getCardSubCollection()
            .document(cardId.toString())
            .delete()
            .await()
    }

    override suspend fun removeCardsOfDeck(deckId: Int) {
        TODO("Not yet implemented")
    }

    private fun getCardSubCollection(): CollectionReference {
        return firestore.collection(MAIN_COLLECTION_NAME)
            .document(CARD_DOCUMENT_NAME)
            .collection(CARD_SUB_COLLECTION_NAME)
    }
}