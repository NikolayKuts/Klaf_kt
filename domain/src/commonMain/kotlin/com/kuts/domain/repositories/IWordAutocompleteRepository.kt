package com.kuts.domain.repositories

import com.kuts.domain.entities.AutocompleteWord

interface IWordAutocompleteRepository {

    val isEnabled: Boolean get() = true

    suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord>
}
