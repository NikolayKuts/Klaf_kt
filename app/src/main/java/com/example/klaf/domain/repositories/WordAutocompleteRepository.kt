package com.example.klaf.domain.repositories

import com.example.klaf.domain.entities.AutocompleteWord

interface WordAutocompleteRepository {

    suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord>
}