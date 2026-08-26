package com.kuts.domain.repositories

interface IMnemonicImageRemoteRepository {

    val isEnabled: Boolean get() = true

    suspend fun uploadImage(assetId: String, imageBytes: ByteArray)

    suspend fun uploadImageAtPath(assetId: String, imageBytes: ByteArray, rootEmailPath: String)

    suspend fun downloadImage(assetId: String): ByteArray?

    suspend fun deleteImage(assetId: String)
}
