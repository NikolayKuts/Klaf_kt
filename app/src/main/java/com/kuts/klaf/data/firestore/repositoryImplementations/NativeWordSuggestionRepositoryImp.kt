package com.kuts.klaf.data.firestore.repositoryImplementations

import com.kuts.domain.repositories.NativeWordSuggestionRepository
import com.kuts.klaf.data.networking.NativeWordSuggestionClient
import javax.inject.Inject

class NativeWordSuggestionRepositoryImp @Inject constructor(
    private val nativeWordSuggestionClient: NativeWordSuggestionClient,
) : NativeWordSuggestionRepository {

    override suspend fun fetchSuggestions(word: String): List<String> {
        return nativeWordSuggestionClient.retrieveSuggestions(word = word)
    }
}