package com.kuts.klaf.mnemonic

import com.kuts.domain.entities.MnemonicImageAsset
import com.kuts.domain.repositories.IMnemonicImageAssetRepository

class IosNoOpMnemonicImageAssetRepository : IMnemonicImageAssetRepository {
    override suspend fun createDraftImage(imageBytes: ByteArray): MnemonicImageAsset {
        throw UnsupportedOperationException("Mnemonic image storage is unavailable on iOS.")
    }

    override suspend fun createSavedCopyFromDraft(draftAssetId: String): MnemonicImageAsset {
        throw UnsupportedOperationException("Mnemonic image storage is unavailable on iOS.")
    }

    override suspend fun resolveSavedImage(assetId: String): MnemonicImageAsset? = null

    override suspend fun deleteDraftImage(assetId: String) = Unit

    override suspend fun deleteSavedImage(assetId: String) = Unit

    override suspend fun clearAllDraftImages() = Unit
}
