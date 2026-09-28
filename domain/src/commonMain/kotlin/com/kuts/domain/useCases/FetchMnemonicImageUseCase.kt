package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.MnemonicImageResult
import com.kuts.domain.entities.MnemonicSelection
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.repositories.IMnemonicImageRepository
import kotlinx.coroutines.withContext

class FetchMnemonicImageUseCase(
    private val mnemonicImageRepository: IMnemonicImageRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(
        selection: MnemonicSelection,
        comment: String? = null,
        launchSource: MnemonicGenerationSource? = null,
    ): MnemonicImageResult = withContext(
        context = coroutineContextProvider.io,
    ) {
        mnemonicImageRepository.fetchMnemonicImage(
            selection = selection,
            comment = comment,
            launchSource = launchSource,
        )
    }
}
