package com.kuts.klaf.di

import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.repositories.IWordAutocompleteRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class WordAutocompleteRepositorySelectionTest {

    @Test
    fun `account scoped storage uses server autocomplete without creating Firestore repository`() = runBlocking {
        var legacyFactoryCalls = 0
        val accountRepository = object : IWordAutocompleteRepository {
            override suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord> = emptyList()
        }
        val repository = selectWordAutocompleteRepository(
            useAccountScopedStorage = true,
            accountRepository = { accountRepository },
            legacyRepository = {
                legacyFactoryCalls++
                error("Firestore autocomplete must not be created in Room mode")
            },
        )

        assertSame(accountRepository, repository)
        assertTrue(repository.isEnabled)
        assertEquals(emptyList(), repository.fetchAutocomplete("word"))
        assertEquals(0, legacyFactoryCalls)
    }

    @Test
    fun `legacy storage keeps its existing autocomplete repository`() {
        val legacy = object : IWordAutocompleteRepository {
            override suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord> = emptyList()
        }

        assertSame(legacy, selectWordAutocompleteRepository(false, { error("Server repository must not be used") }) { legacy })
        assertTrue(legacy.isEnabled)
    }
}
