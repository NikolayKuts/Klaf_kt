package com.kuts.klaf.di

import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.repositories.IWordAutocompleteRepository

internal object DisabledWordAutocompleteRepository : IWordAutocompleteRepository {

    override val isEnabled: Boolean = false

    override suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord> = emptyList()
}

internal fun selectWordAutocompleteRepository(
    useAccountScopedStorage: Boolean,
    legacyRepository: () -> IWordAutocompleteRepository,
): IWordAutocompleteRepository = if (useAccountScopedStorage) {
    DisabledWordAutocompleteRepository
} else {
    legacyRepository()
}
