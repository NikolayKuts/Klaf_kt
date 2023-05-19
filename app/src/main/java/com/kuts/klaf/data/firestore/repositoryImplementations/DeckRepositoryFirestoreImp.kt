package com.kuts.klaf.data.firestore.repositoryImplementations

import com.kuts.klaf.data.firestore.entities.FirestoreDeck
import com.kuts.klaf.data.firestore.toDomainEntity
import com.kuts.klaf.data.firestore.toFirestoreEntity
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.DeckRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeckRepositoryFirestoreImp @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : DeckRepository {

    companion object {

        private const val DECK_DOCUMENT_NAME = "decks"
        private const val DECK_SUB_COLLECTION_NAME = "deck_collection"
    }

    override fun fetchDeckSource(): Flow<List<Deck>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchAllDecks(): List<Deck> {
        return getDeckSubCollection()
            .get()
            .await()
            .documents
            .mapNotNull { documentSnapshot -> documentSnapshot.toObject<FirestoreDeck>() }
            .map { firestoreDeck -> firestoreDeck.toDomainEntity() }
    }

    override fun fetchObservableDeckById(deckId: Int): Flow<Deck?> {
        TODO("Not yet implemented")
    }

    override suspend fun insertDeck(deck: Deck) {
        getDeckSubCollection()
            .document(deck.id.toString())
            .set(deck.toFirestoreEntity())
            .await()
    }

    override suspend fun removeDeck(deckId: Int) {
        getDeckSubCollection()
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

    private fun getDeckSubCollection(): CollectionReference {
        val collectionName = auth.currentUser?.email
            ?: throw RuntimeException("There is no authorized user")

        return firestore.collection(collectionName)
            .document(DECK_DOCUMENT_NAME)
            .collection(DECK_SUB_COLLECTION_NAME)
    }
}