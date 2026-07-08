package com.kuts.domain.repositories

import com.kuts.domain.entities.MnemonicAssociation

interface IMnemonicAssociationRepository {

    suspend fun fetchMnemonicAssociation(word: String): MnemonicAssociation
}
