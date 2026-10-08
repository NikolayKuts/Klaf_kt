package com.kuts.klaf.mnemonic

import com.kuts.domain.entities.MnemonicImageAsset
import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Existing local images stay local; downloaded images are isolated by account. */
class CachedMnemonicImageAssetRepository(
    private val local: IMnemonicImageAssetRepository,
    private val selectedEmail: () -> String?,
    private val cacheForAccount: (String) -> IMnemonicImageAssetRepository,
    private val download: suspend (email: String, assetId: String) -> ByteArray?,
    private val ioContext: CoroutineContext,
) : IMnemonicImageAssetRepository by local {

    private val mutex = Mutex()

    /** Manual sync completes only after each referenced saved image is present locally. */
    suspend fun cacheMissingImages(email: String, assetIds: List<String>) = withContext(ioContext) {
        check(selectedEmail() == email) { "Selected account changed during image synchronization" }
        for (assetId in assetIds.distinct()) {
            require(isSafeMnemonicImageId(assetId)) { "Invalid mnemonic image asset ID" }
            mutex.withLock {
                check(selectedEmail() == email) { "Selected account changed during image synchronization" }
                if (local.resolveSavedImage(assetId) != null) return@withLock
                val cache = cacheForAccount(email)
                if (cache.resolveSavedImage(assetId) != null) return@withLock
                val bytes = downloadWithRetry(email, assetId)
                    ?: error("Referenced saved image is missing on the server")
                check(bytes.size in 1..MAX_MNEMONIC_IMAGE_BYTES && mnemonicImageContentType(bytes) != null) {
                    "Invalid saved image response"
                }
                check(selectedEmail() == email) { "Selected account changed during image synchronization" }
                cache.importSavedImage(assetId, bytes)
            }
        }
    }

    suspend fun hasMissingImages(email: String, assetIds: List<String>): Boolean = withContext(ioContext) {
        check(selectedEmail() == email) { "Selected account changed during image status check" }
        val cache = cacheForAccount(email)
        assetIds.any { assetId ->
            !isSafeMnemonicImageId(assetId) ||
                local.resolveSavedImage(assetId) == null && cache.resolveSavedImage(assetId) == null
        }
    }

    private suspend fun downloadWithRetry(email: String, assetId: String): ByteArray? {
        repeat(2) { attempt ->
            try {
                return download(email, assetId)
            } catch (failure: AccountOperationException) {
                if (attempt == 1 || failure.failure !in setOf(AccountFailure.TIMEOUT, AccountFailure.CONNECTION)) {
                    throw failure
                }
                delay(500L)
            }
        }
        error("Image download attempts exhausted")
    }

    override suspend fun resolveSavedImage(assetId: String): MnemonicImageAsset? = withContext(ioContext) {
        if (!isSafeMnemonicImageId(assetId)) return@withContext null
        val email = selectedEmail()
        mutex.withLock {
            try {
                if (selectedEmail() != email) return@withLock null
                local.resolveSavedImage(assetId)?.let { return@withLock it.takeIf { selectedEmail() == email } }
                if (email == null || selectedEmail() != email) return@withLock null
                val image = cacheForAccount(email).resolveSavedImage(assetId)
                image.takeIf { selectedEmail() == email }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Missing/offline images do not make the card or review screen unusable.
                null
            }
        }
    }

    override suspend fun readSavedImageBytes(assetId: String): ByteArray? = withContext(ioContext) {
        val email = selectedEmail()
        resolveSavedImage(assetId) ?: return@withContext null
        if (selectedEmail() != email) return@withContext null
        val bytes = local.readSavedImageBytes(assetId) ?: email?.let { cacheForAccount(it).readSavedImageBytes(assetId) }
        bytes.takeIf { selectedEmail() == email }
    }
}
