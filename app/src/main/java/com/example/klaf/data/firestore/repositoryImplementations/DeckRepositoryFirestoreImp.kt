package com.example.klaf.data.firestore.repositoryImplementations

import com.example.klaf.data.firestore.mapToFirestoreEntity
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeckRepositoryFirestoreImp @Inject constructor(
    private val firestore: FirebaseFirestore
) : DeckRepository {

    override fun fetchDeckSource(): Flow<List<Deck>> {
        TODO("Not yet implemented")
    }

    override fun fetchObservableDeckById(deckId: Int): Flow<Deck?> {
        TODO("Not yet implemented")
    }

    override suspend fun insertDeck(deck: Deck) {
        firestore.collection("old_klaf_collection")
            .document("decks")
            .collection("deck_collection")
            .document(deck.id.toString())
            .set(deck.mapToFirestoreEntity())
            .await()
    }

    override suspend fun removeDeck(deckId: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun getDeckById(deckId: Int): Deck? {
        TODO("Not yet implemented")
    }

    override suspend fun getCardQuantityInDeck(deckId: Int): Int {
        TODO("Not yet implemented")
    }
}