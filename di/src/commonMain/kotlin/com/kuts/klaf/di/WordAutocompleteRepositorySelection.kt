package com.kuts.klaf.di

import com.kuts.domain.repositories.IWordAutocompleteRepository

internal fun selectWordAutocompleteRepository(
    useAccountScopedStorage: Boolean,
    accountRepository: () -> IWordAutocompleteRepository,
    legacyRepository: () -> IWordAutocompleteRepository,
): IWordAutocompleteRepository = if (useAccountScopedStorage) {
    accountRepository()
} else {
    legacyRepository()
}
