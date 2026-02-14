package com.kuts.klaf.firestore.repositoryImplementations

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.repositories.IWordAutocompleteRepository
import com.kuts.klaf.firestore.entities.FirestoreAutocompleteWord
import com.kuts.klaf.firestore.toDomainEntity
import com.lib.lokdroid.core.logD
import kotlinx.coroutines.tasks.await

class WordAutocompleteFirestore(
    private val firestore: FirebaseFirestore,
) : IWordAutocompleteRepository {

    companion object {

        private const val AUTOCOMPLETE_WORD_COLLECTION_NAME = "autocomplete_word_collection"
        private const val WORD_GROUP_SUFFIX = "_words"
        private const val WORD_SUB_COLLECTION_SUFFIX = "_subCollection"
        private const val UNICODE_RANGE = "\uf8ff"
        private const val RESULT_LIMIT = 10L
        private const val MIN_WORD_LENGTH = 1
    }

    override suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord> {
        logD("fetchAutocomplete() called")

        val groupLetter = prefix.trim().firstOrNull()?.lowercase()

        return if (groupLetter.isNullOrEmpty()) {
            emptyList()
        } else {
            val wordFieldName = FirestoreAutocompleteWord::word.name
            val formattedPrefix = prefix.trim().lowercase()

            firestore.collection(AUTOCOMPLETE_WORD_COLLECTION_NAME)
                .document("${groupLetter}$WORD_GROUP_SUFFIX")
                .collection("${groupLetter}$WORD_SUB_COLLECTION_SUFFIX")
                .orderBy(wordFieldName)
                .startAt(formattedPrefix)
                .endAt("$formattedPrefix$UNICODE_RANGE")
                .limit(RESULT_LIMIT)
                .get()
                .await()
                .documents.mapNotNull { document -> document.toObject<FirestoreAutocompleteWord>() }
                .map { firestoreAutocompleteWord -> firestoreAutocompleteWord.toDomainEntity() }
                .map { autocompleteWord -> AutocompleteWord(value = autocompleteWord.word().trim()) }
                .filter { autocompleteWord -> autocompleteWord.word().length > MIN_WORD_LENGTH }
        }
    }
}