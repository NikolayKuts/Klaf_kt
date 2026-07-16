package com.kuts.klaf.firebaseStorage

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.kuts.domain.repositories.IMnemonicImageRemoteRepository
import com.kuts.klaf.firestore.ROOT_COLLECTION_NAME_PREFIX
import kotlinx.coroutines.tasks.await

class AndroidMnemonicImageRemoteRepository(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
) : IMnemonicImageRemoteRepository {

    override suspend fun uploadImage(assetId: String, imageBytes: ByteArray) {
        imageReference(assetId = assetId)
            .putBytes(
                imageBytes,
                StorageMetadata.Builder()
                    .setContentType(detectContentType(imageBytes = imageBytes))
                    .build(),
            )
            .await()
    }

    override suspend fun uploadImageAtPath(
        assetId: String,
        imageBytes: ByteArray,
        rootEmailPath: String,
    ) {
        imageReference(
            assetId = assetId,
            rootEmailPath = rootEmailPath,
        ).putBytes(
            imageBytes,
            StorageMetadata.Builder()
                .setContentType(detectContentType(imageBytes = imageBytes))
                .build(),
        ).await()
    }

    override suspend fun downloadImage(assetId: String): ByteArray? {
        return try {
            imageReference(assetId = assetId)
                .getBytes(MAX_DOWNLOAD_BYTES)
                .await()
        } catch (exception: StorageException) {
            if (exception.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                null
            } else {
                throw exception
            }
        }
    }

    override suspend fun deleteImage(assetId: String) {
        try {
            imageReference(assetId = assetId)
                .delete()
                .await()
        } catch (exception: StorageException) {
            if (exception.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) {
                throw exception
            }
        }
    }

    private fun imageReference(assetId: String): StorageReference {
        val userEmail = auth.currentUser?.email
            ?: throw RuntimeException("There is no authorized user")

        return imageReference(
            assetId = assetId,
            rootEmailPath = userEmail,
        )
    }

    private fun imageReference(
        assetId: String,
        rootEmailPath: String,
    ): StorageReference {
        return storage.reference
            .child("$ROOT_COLLECTION_NAME_PREFIX$rootEmailPath")
            .child(MNEMONIC_IMAGES_DIRECTORY_NAME)
            .child(assetId)
    }

    private fun detectContentType(imageBytes: ByteArray): String {
        if (imageBytes.size >= 8 &&
            imageBytes[0] == 0x89.toByte() &&
            imageBytes[1] == 'P'.code.toByte() &&
            imageBytes[2] == 'N'.code.toByte() &&
            imageBytes[3] == 'G'.code.toByte()
        ) {
            return "image/png"
        }

        if (imageBytes.size >= 3 &&
            imageBytes[0] == 0xFF.toByte() &&
            imageBytes[1] == 0xD8.toByte() &&
            imageBytes[2] == 0xFF.toByte()
        ) {
            return "image/jpeg"
        }

        if (imageBytes.size >= 12 &&
            imageBytes[0] == 'R'.code.toByte() &&
            imageBytes[1] == 'I'.code.toByte() &&
            imageBytes[2] == 'F'.code.toByte() &&
            imageBytes[3] == 'F'.code.toByte() &&
            imageBytes[8] == 'W'.code.toByte() &&
            imageBytes[9] == 'E'.code.toByte() &&
            imageBytes[10] == 'B'.code.toByte() &&
            imageBytes[11] == 'P'.code.toByte()
        ) {
            return "image/webp"
        }

        return DEFAULT_CONTENT_TYPE
    }

    private companion object {
        private const val MNEMONIC_IMAGES_DIRECTORY_NAME = "mnemonic-images"
        private const val DEFAULT_CONTENT_TYPE = "image/png"
        private const val MAX_DOWNLOAD_BYTES = 5L * 1024L * 1024L
    }
}
