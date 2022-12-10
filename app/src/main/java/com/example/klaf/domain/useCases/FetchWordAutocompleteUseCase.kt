package com.example.klaf.domain.useCases

import com.example.klaf.domain.entities.AutocompleteWord
import com.example.klaf.domain.repositories.WordAutocompleteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FetchWordAutocompleteUseCase @Inject constructor(
    private val wordAutocompleteRepository: WordAutocompleteRepository
) {

    suspend operator fun invoke(prefix: String): List<AutocompleteWord> {
        return withContext(Dispatchers.IO) {
            wordAutocompleteRepository.fetchAutocomplete(prefix = prefix)
        }
    }
}