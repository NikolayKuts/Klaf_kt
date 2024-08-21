package com.kuts.domain.repositories

interface NativeWordSuggestionRepository {

    suspend fun fetchSuggestions(word: String): List<String>
}