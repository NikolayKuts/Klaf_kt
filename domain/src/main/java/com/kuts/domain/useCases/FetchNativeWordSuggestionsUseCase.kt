package com.kuts.domain.useCases

import com.kuts.domain.repositories.NativeWordSuggestionRepository
import javax.inject.Inject

class FetchNativeWordSuggestionsUseCase @Inject constructor(
    private val nativeWordSuggestionRepository: NativeWordSuggestionRepository,
) {

    suspend operator fun invoke(word: String): List<String> {
        return nativeWordSuggestionRepository.fetchSuggestions(word = word)
    }
}