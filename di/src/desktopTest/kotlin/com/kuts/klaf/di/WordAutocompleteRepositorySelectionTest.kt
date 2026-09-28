package com.kuts.klaf.di

import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.repositories.IWordAutocompleteRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class WordAutocompleteRepositorySelectionTest {

    @Test
    fun `account scoped storage disables autocomplete without creating Firestore repository`() = runBlocking {
        var legacyFactoryCalls = 0
        val repository = selectWordAutocompleteRepository(useAccountScopedStorage = true) {
            legacyFactoryCalls++
            error("Firestore autocomplete must not be created in Room mode")
        }

        assertSame(DisabledWordAutocompleteRepository, repository)
        assertFalse(repository.isEnabled)
        assertEquals(emptyList(), repository.fetchAutocomplete("word"))
        assertEquals(0, legacyFactoryCalls)
    }

    @Test
    fun `legacy storage keeps its existing autocomplete repository`() {
        val legacy = object : IWordAutocompleteRepository {
            override suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord> = emptyList()
        }

        assertSame(legacy, selectWordAutocompleteRepository(useAccountScopedStorage = false) { legacy })
        assertTrue(legacy.isEnabled)
    }
}
