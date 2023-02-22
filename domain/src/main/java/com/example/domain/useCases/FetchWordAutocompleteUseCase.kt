package com.example.domain.useCases

import com.example.domain.entities.AutocompleteWord
import com.example.domain.repositories.WordAutocompleteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FetchWordAutocompleteUseCase @Inject constructor(
    private val wordAutocompleteRepository: WordAutocompleteRepository,
) {

    suspend operator fun invoke(prefix: String): List<AutocompleteWord> {
        return withContext(Dispatchers.IO) {
            wordAutocompleteRepository.fetchAutocomplete(prefix = prefix)
        }
    }
}