package com.kuts.domain.repositories

import com.kuts.domain.entities.MnemonicSelection
import com.kuts.domain.entities.MnemonicImageResult
import com.kuts.domain.managers.MnemonicGenerationSource

interface IMnemonicImageRepository {

    suspend fun fetchMnemonicImage(
        selection: MnemonicSelection,
        comment: String? = null,
        launchSource: MnemonicGenerationSource? = null,
    ): MnemonicImageResult
}
