package com.kuts.domain.repositories

import com.kuts.domain.entities.MnemonicAssociationResult
import com.kuts.domain.managers.MnemonicGenerationSource

interface IMnemonicAssociationRepository {

    suspend fun fetchMnemonicAssociation(
        word: String,
        comment: String? = null,
        excludedSoundAnchors: List<String> = emptyList(),
        launchSource: MnemonicGenerationSource? = null,
    ): MnemonicAssociationResult
}
