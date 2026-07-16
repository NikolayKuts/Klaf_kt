package com.kuts.klaf.mnemonic

import com.kuts.domain.repositories.IMnemonicImageRemoteRepository

class IosNoOpMnemonicImageRemoteRepository : IMnemonicImageRemoteRepository {

    override suspend fun uploadImage(assetId: String, imageBytes: ByteArray) = Unit

    override suspend fun uploadImageAtPath(
        assetId: String,
        imageBytes: ByteArray,
        rootEmailPath: String,
    ) = Unit

    override suspend fun downloadImage(assetId: String): ByteArray? = null

    override suspend fun deleteImage(assetId: String) = Unit
}
