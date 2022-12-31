package com.example.klaf.data.firestore.repositoryImplementations

import com.example.klaf.data.firestore.entities.FirestoreAutocompleteWord
import com.example.klaf.data.firestore.mapToDomainEntity
import com.example.klaf.domain.entities.AutocompleteWord
import com.example.klaf.domain.repositories.WordAutocompleteRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class WordAutocompleteFirestoreImp @Inject constructor(
    private val firestore: FirebaseFirestore,
) : WordAutocompleteRepository {

    companion object {

        private const val AUTOCOMPLETE_WORD_COLLECTION_NAME = "autocomplete_word_collection"
        private const val WORD_GROUP_SUFFIX = "_words"
        private const val WORD_SUB_COLLECTION_SUFFIX = "_subCollection"
        private const val UNICODE_RANGE = "\uf8ff"
        private const val RESULT_LIMIT = 10L
        private const val MIN_WORD_LENGTH = 2
    }

    override suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord> {
        firestore.collection(AUTOCOMPLETE_WORD_COLLECTION_NAME)
        val wordFieldName = FirestoreAutocompleteWord::word.name
        val groupLetter = prefix.firstOrNull()

        return if (groupLetter == null) {
            emptyList()
        } else {
            firestore.collection(AUTOCOMPLETE_WORD_COLLECTION_NAME)
                .document("${groupLetter}$WORD_GROUP_SUFFIX")
                .collection("${groupLetter}$WORD_SUB_COLLECTION_SUFFIX")
                .orderBy(wordFieldName)
                .startAt(prefix)
                .endAt("$prefix$UNICODE_RANGE")
                .limit(RESULT_LIMIT)
                .get()
                .await()
                .documents.mapNotNull { document -> document.toObject<FirestoreAutocompleteWord>() }
                .map { firestoreAutocompleteWord -> firestoreAutocompleteWord.mapToDomainEntity() }
                .filter { autocompleteWord -> autocompleteWord.value.length > MIN_WORD_LENGTH }
        }
    }
}