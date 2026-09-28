package com.kuts.klaf.mnemonic

import com.kuts.domain.entities.MnemonicImageAsset
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import kotlinx.coroutines.CancellationException
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

    override suspend fun resolveSavedImage(assetId: String): MnemonicImageAsset? = withContext(ioContext) {
        if (!isSafeMnemonicImageId(assetId)) return@withContext null
        val email = selectedEmail()
        mutex.withLock {
            try {
                if (selectedEmail() != email) return@withLock null
                local.resolveSavedImage(assetId)?.let { return@withLock it.takeIf { selectedEmail() == email } }
                if (email == null || selectedEmail() != email) return@withLock null
                val cache = cacheForAccount(email)
                val image = cache.resolveSavedImage(assetId) ?: run {
                    val bytes = download(email, assetId) ?: return@withLock null
                    if (bytes.size !in 1..MAX_MNEMONIC_IMAGE_BYTES || mnemonicImageContentType(bytes) == null) return@withLock null
                    cache.importSavedImage(assetId, bytes)
                }
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
