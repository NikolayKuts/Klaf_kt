package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.MnemonicAssociation
import com.kuts.domain.repositories.IMnemonicAssociationRepository
import kotlinx.coroutines.withContext

class FetchMnemonicAssociationUseCase(
    private val mnemonicAssociationRepository: IMnemonicAssociationRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(word: String): MnemonicAssociation = withContext(
        context = coroutineContextProvider.io,
    ) {
        mnemonicAssociationRepository.fetchMnemonicAssociation(word = word)
    }
}
