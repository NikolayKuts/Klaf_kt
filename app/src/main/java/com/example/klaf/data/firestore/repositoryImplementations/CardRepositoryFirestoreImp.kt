package com.example.klaf.data.firestore.repositoryImplementations

import com.example.domain.entities.Card
import com.example.domain.repositories.CardRepository
import com.example.klaf.data.firestore.entities.FirestoreCard
import com.example.klaf.data.firestore.toDomainEntity
import com.example.klaf.data.firestore.toFirestoreEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CardRepositoryFirestoreImp @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : CardRepository {

    companion object {

        private const val CARD_DOCUMENT_NAME = "cards"
        private const val CARD_SUB_COLLECTION_NAME = "card_collection"
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
            .map { firestoreCard -> firestoreCard.toDomainEntity() }
    }

    override suspend fun insertCard(card: Card) {
        getCardSubCollection()
            .document(card.id.toString())
            .set(card.toFirestoreEntity())
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
        val mainCollectionName = auth.currentUser?.email
            ?: throw RuntimeException("There is no authorized user")

        return firestore.collection(mainCollectionName)
            .document(CARD_DOCUMENT_NAME)
            .collection(CARD_SUB_COLLECTION_NAME)
    }
}