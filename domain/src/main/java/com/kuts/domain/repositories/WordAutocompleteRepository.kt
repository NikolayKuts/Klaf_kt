package com.kuts.domain.repositories

import com.kuts.domain.entities.AutocompleteWord

interface WordAutocompleteRepository {

    suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord>
}