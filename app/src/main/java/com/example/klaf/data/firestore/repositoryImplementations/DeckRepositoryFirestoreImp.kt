package com.example.klaf.data.firestore.repositoryImplementations

import android.util.Log
import com.example.klaf.data.firestore.MAIN_COLLECTION_NAME
import com.example.klaf.data.firestore.entities.FirestoreDeck
import com.example.klaf.data.firestore.mapToDomainEntity
import com.example.klaf.data.firestore.mapToFirestoreEntity
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.presentation.common.log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeckRepositoryFirestoreImp @Inject constructor(
    private val firestore: FirebaseFirestore
) : DeckRepository {

    companion object {

        private const val DECK_DOCUMENT_NAME = "decks"
        private const val DECK_SUB_COLLECTION_NAME = "deck_collection"
    }

    override fun fetchDeckSource(): Flow<List<Deck>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchAllDecks(): List<Deck> {
        return firestore.collection(MAIN_COLLECTION_NAME)
            .document(DECK_DOCUMENT_NAME)
            .collection(DECK_SUB_COLLECTION_NAME)
            .get()
            .await()
            .documents
            .mapNotNull { documentSnapshot -> documentSnapshot.toObject<FirestoreDeck>() }
            .map { it.mapToDomainEntity()  }
    }

    override fun fetchObservableDeckById(deckId: Int): Flow<Deck?> {
        TODO("Not yet implemented")
    }

    override suspend fun insertDeck(deck: Deck) {
        firestore.collection(MAIN_COLLECTION_NAME)
            .document(DECK_DOCUMENT_NAME)
            .collection(DECK_SUB_COLLECTION_NAME)
            .document(deck.id.toString())
            .set(deck.mapToFirestoreEntity())
            .await()
    }

    override suspend fun removeDeck(deckId: Int) {
        firestore.collection(MAIN_COLLECTION_NAME)
            .document(DECK_DOCUMENT_NAME)
            .collection(DECK_SUB_COLLECTION_NAME)
            .document(deckId.toString())
            .delete()
            .await()
    }

    override suspend fun getDeckById(deckId: Int): Deck? {
        TODO("Not yet implemented")
    }

    override suspend fun getCardQuantityInDeck(deckId: Int): Int {
        TODO("Not yet implemented")
    }
}