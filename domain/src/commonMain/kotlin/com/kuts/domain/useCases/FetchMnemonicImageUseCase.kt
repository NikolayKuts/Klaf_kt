package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.MnemonicSelection
import com.kuts.domain.repositories.IMnemonicImageRepository
import kotlinx.coroutines.withContext

class FetchMnemonicImageUseCase(
    private val mnemonicImageRepository: IMnemonicImageRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(
        selection: MnemonicSelection,
        comment: String? = null,
    ): ByteArray = withContext(
        context = coroutineContextProvider.io,
    ) {
        mnemonicImageRepository.fetchMnemonicImage(
            selection = selection,
            comment = comment,
        )
    }
}
