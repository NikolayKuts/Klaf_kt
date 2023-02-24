package com.example.domain.repositories

import com.example.domain.entities.AutocompleteWord

interface WordAutocompleteRepository {

    suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord>
}