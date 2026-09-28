package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.MnemonicAssociationResult
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.repositories.IMnemonicAssociationRepository
import kotlinx.coroutines.withContext

class FetchMnemonicAssociationUseCase(
    private val mnemonicAssociationRepository: IMnemonicAssociationRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(
        word: String,
        comment: String? = null,
        excludedSoundAnchors: List<String> = emptyList(),
        launchSource: MnemonicGenerationSource? = null,
    ): MnemonicAssociationResult = withContext(
        context = coroutineContextProvider.io,
    ) {
        mnemonicAssociationRepository.fetchMnemonicAssociation(
            word = word,
            comment = comment,
            excludedSoundAnchors = excludedSoundAnchors,
            launchSource = launchSource,
        )
    }
}
