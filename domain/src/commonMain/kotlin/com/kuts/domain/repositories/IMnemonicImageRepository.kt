package com.kuts.domain.repositories

import com.kuts.domain.entities.MnemonicSelection

interface IMnemonicImageRepository {

    suspend fun fetchMnemonicImage(
        selection: MnemonicSelection,
        comment: String? = null,
    ): ByteArray
}
