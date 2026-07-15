package com.kuts.domain.repositories

import com.kuts.domain.entities.MnemonicImageAsset

interface IMnemonicImageAssetRepository {

    suspend fun createDraftImage(imageBytes: ByteArray): MnemonicImageAsset

    suspend fun createSavedCopyFromDraft(draftAssetId: String): MnemonicImageAsset

    suspend fun resolveSavedImage(assetId: String): MnemonicImageAsset?

    suspend fun deleteDraftImage(assetId: String)

    suspend fun deleteSavedImage(assetId: String)

    suspend fun clearAllDraftImages()
}
