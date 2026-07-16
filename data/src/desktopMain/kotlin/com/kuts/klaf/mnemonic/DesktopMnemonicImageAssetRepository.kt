package com.kuts.klaf.mnemonic

import com.kuts.domain.entities.MnemonicImageAsset
import com.kuts.domain.entities.MnemonicImageAssetStorage
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import java.io.File
import java.util.UUID

class DesktopMnemonicImageAssetRepository : IMnemonicImageAssetRepository {

    private val rootDirectory = File(appDirectory(), ROOT_DIRECTORY_NAME).apply { mkdirs() }
    private val draftDirectory = File(rootDirectory, DRAFT_DIRECTORY_NAME).apply { mkdirs() }
    private val savedDirectory = File(rootDirectory, SAVED_DIRECTORY_NAME).apply { mkdirs() }

    override suspend fun createDraftImage(imageBytes: ByteArray): MnemonicImageAsset {
        val assetId = UUID.randomUUID().toString()
        val targetFile = draftFile(
            assetId = assetId,
            imageExtension = detectImageExtension(imageBytes = imageBytes),
        )
        targetFile.writeBytes(imageBytes)
        return targetFile.toMnemonicImageAsset(
            assetId = assetId,
            storage = MnemonicImageAssetStorage.Draft,
        )
    }

    override suspend fun createSavedCopyFromDraft(draftAssetId: String): MnemonicImageAsset {
        val sourceFile = requireNotNull(findDraftFile(assetId = draftAssetId)) {
            "Mnemonic draft image does not exist. assetId=$draftAssetId"
        }
        require(sourceFile.exists()) {
            "Mnemonic draft image does not exist. assetId=$draftAssetId"
        }

        val savedAssetId = UUID.randomUUID().toString()
        val targetFile = savedFile(
            assetId = savedAssetId,
            imageExtension = sourceFile.extension.ifBlank { DEFAULT_IMAGE_EXTENSION },
        )
        sourceFile.copyTo(target = targetFile, overwrite = false)
        return targetFile.toMnemonicImageAsset(
            assetId = savedAssetId,
            storage = MnemonicImageAssetStorage.Saved,
        )
    }

    override suspend fun importSavedImage(assetId: String, imageBytes: ByteArray): MnemonicImageAsset {
        findSavedFile(assetId = assetId)?.delete()

        val targetFile = savedFile(
            assetId = assetId,
            imageExtension = detectImageExtension(imageBytes = imageBytes),
        )

        targetFile.writeBytes(imageBytes)

        return targetFile.toMnemonicImageAsset(
            assetId = assetId,
            storage = MnemonicImageAssetStorage.Saved,
        )
    }

    override suspend fun resolveSavedImage(assetId: String): MnemonicImageAsset? {
        val targetFile = findSavedFile(assetId = assetId)
        return targetFile?.takeIf { file -> file.exists() }?.toMnemonicImageAsset(
            assetId = assetId,
            storage = MnemonicImageAssetStorage.Saved,
        )
    }

    override suspend fun readSavedImageBytes(assetId: String): ByteArray? {
        return findSavedFile(assetId = assetId)
            ?.takeIf { file -> file.exists() }
            ?.readBytes()
    }

    override suspend fun deleteDraftImage(assetId: String) {
        findDraftFile(assetId = assetId)?.delete()
    }

    override suspend fun deleteSavedImage(assetId: String) {
        findSavedFile(assetId = assetId)?.delete()
    }

    override suspend fun clearAllDraftImages() {
        if (draftDirectory.exists()) {
            draftDirectory.deleteRecursively()
        }
        draftDirectory.mkdirs()
    }

    private fun appDirectory(): File {
        return File(System.getProperty("user.home"), ".klaf_kt").apply { mkdirs() }
    }

    private fun draftFile(
        assetId: String,
        imageExtension: String,
    ): File = File(draftDirectory, "$assetId.$imageExtension")

    private fun savedFile(
        assetId: String,
        imageExtension: String,
    ): File = File(savedDirectory, "$assetId.$imageExtension")

    private fun findDraftFile(assetId: String): File? = findImageFile(
        directory = draftDirectory,
        assetId = assetId,
    )

    private fun findSavedFile(assetId: String): File? = findImageFile(
        directory = savedDirectory,
        assetId = assetId,
    )

    private fun findImageFile(
        directory: File,
        assetId: String,
    ): File? {
        return directory.listFiles()
            ?.firstOrNull { file -> file.isFile && file.nameWithoutExtension == assetId }
    }

    private fun detectImageExtension(imageBytes: ByteArray): String {
        if (imageBytes.size >= 8 &&
            imageBytes[0] == 0x89.toByte() &&
            imageBytes[1] == 'P'.code.toByte() &&
            imageBytes[2] == 'N'.code.toByte() &&
            imageBytes[3] == 'G'.code.toByte()
        ) {
            return "png"
        }

        if (imageBytes.size >= 3 &&
            imageBytes[0] == 0xFF.toByte() &&
            imageBytes[1] == 0xD8.toByte() &&
            imageBytes[2] == 0xFF.toByte()
        ) {
            return "jpg"
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
            return "webp"
        }

        return DEFAULT_IMAGE_EXTENSION
    }

    private fun File.toMnemonicImageAsset(
        assetId: String,
        storage: MnemonicImageAssetStorage,
    ): MnemonicImageAsset {
        return MnemonicImageAsset(
            assetId = assetId,
            filePath = absolutePath,
            storage = storage,
        )
    }

    private companion object {
        private const val ROOT_DIRECTORY_NAME = "mnemonic-images"
        private const val DRAFT_DIRECTORY_NAME = "drafts"
        private const val SAVED_DIRECTORY_NAME = "saved"
        private const val DEFAULT_IMAGE_EXTENSION = "png"
    }
}
