package com.kuts.klaf.firebaseStorage

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.kuts.domain.repositories.IMnemonicImageRemoteRepository

@Suppress("UNUSED_PARAMETER")
class AndroidMnemonicImageRemoteRepository(
    storage: FirebaseStorage,
    auth: FirebaseAuth,
) : IMnemonicImageRemoteRepository {

    override val isEnabled = false

    // Firebase Storage is currently not connected for mnemonic images.
    // This repository works as a mock/no-op remote image store for now.
    override suspend fun uploadImage(assetId: String, imageBytes: ByteArray) = Unit

    override suspend fun uploadImageAtPath(
        assetId: String,
        imageBytes: ByteArray,
        rootEmailPath: String,
    ) = Unit

    override suspend fun downloadImage(assetId: String): ByteArray? = null

    override suspend fun deleteImage(assetId: String) = Unit
}
