package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.repositories.IWordAutocompleteRepository
import kotlinx.coroutines.withContext

class FetchWordAutocompleteUseCase(
    private val wordAutocompleteRepository: IWordAutocompleteRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(prefix: String): List<AutocompleteWord> {
        return withContext(context = coroutineContextProvider.io) {
            wordAutocompleteRepository.fetchAutocomplete(prefix = prefix)
        }
    }
}
