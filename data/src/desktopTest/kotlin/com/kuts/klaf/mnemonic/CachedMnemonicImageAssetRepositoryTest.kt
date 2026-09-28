package com.kuts.klaf.mnemonic

import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

private const val IMAGE_ID = "2720b4f1-1a0e-4292-a597-836e86f897ac"
private val PNG_BYTES = byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10)

class CachedMnemonicImageAssetRepositoryTest {

    @Test
    fun `download is cached once and remains available offline after repository recreation`() = withImages { local, cache ->
        var calls = 0
        val repository = CachedMnemonicImageAssetRepository(local, { "alice" }, cache, { _, _ ->
            calls++
            PNG_BYTES
        }, Dispatchers.IO)
        coroutineScope {
            List(5) { async { assertNotNull(repository.resolveSavedImage(IMAGE_ID)) } }.awaitAll()
        }
        assertEquals(1, calls)
        val offline = CachedMnemonicImageAssetRepository(local, { "alice" }, cache, { _, _ -> error("offline") }, Dispatchers.IO)
        assertEquals(PNG_BYTES.toList(), offline.readSavedImageBytes(IMAGE_ID)?.toList())
    }

    @Test
    fun `accounts and guest do not reuse another accounts downloaded file`() = withImages { local, cache ->
        var email: String? = "alice"
        var available = true
        val repository = CachedMnemonicImageAssetRepository(local, { email }, cache, { _, _ ->
            if (available) PNG_BYTES else null
        }, Dispatchers.IO)
        assertNotNull(repository.resolveSavedImage(IMAGE_ID))
        available = false
        email = "bob"
        assertNull(repository.resolveSavedImage(IMAGE_ID))
        email = null
        assertNull(repository.resolveSavedImage(IMAGE_ID))
        email = "alice"
        assertNotNull(repository.resolveSavedImage(IMAGE_ID))
    }

    @Test
    fun `missing offline invalid identifiers and account change do not break card display`() = withImages { local, cache ->
        var email: String? = "alice"
        val offline = CachedMnemonicImageAssetRepository(local, { email }, cache, { _, _ -> error("offline") }, Dispatchers.IO)
        assertNull(offline.resolveSavedImage(IMAGE_ID))
        assertNull(offline.resolveSavedImage("../secret"))
        val switched = CachedMnemonicImageAssetRepository(local, { email }, cache, { _, _ ->
            email = "bob"
            PNG_BYTES
        }, Dispatchers.IO)
        assertNull(switched.resolveSavedImage(IMAGE_ID))
    }

    @Test
    fun `invalid downloaded bytes are not stored and a later retry can succeed`() = withImages { local, cache ->
        var valid = false
        val repository = CachedMnemonicImageAssetRepository(local, { "alice" }, cache, { _, _ ->
            if (valid) PNG_BYTES else "broken response".encodeToByteArray()
        }, Dispatchers.IO)
        assertNull(repository.resolveSavedImage(IMAGE_ID))
        assertNull(cache("alice").resolveSavedImage(IMAGE_ID))
        valid = true
        assertNotNull(repository.resolveSavedImage(IMAGE_ID))
    }

    @Test
    fun `failed file replacement retains the previous complete image`() = withImages { local, _ ->
        val jpg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 1)
        val original = local.importSavedImage(IMAGE_ID, jpg)
        val blockedTarget = java.io.File(original.filePath).resolveSibling("$IMAGE_ID.png")
        blockedTarget.mkdir()
        java.io.File(blockedTarget, "blocker").writeText("prevent replacement of nonempty directory")
        assertFailsWith<java.io.IOException> { local.importSavedImage(IMAGE_ID, PNG_BYTES) }
        assertEquals(jpg.toList(), local.readSavedImageBytes(IMAGE_ID)?.toList())
    }

    @Test
    fun `saved image bytes are discarded if account changes during local or cached read`() = withImages { local, cache ->
        for (fromCache in listOf(false, true)) {
            var email: String? = "alice"
            val storage = if (fromCache) cache("alice") else local
            storage.importSavedImage(IMAGE_ID, PNG_BYTES)
            val switching = object : IMnemonicImageAssetRepository by storage {
                override suspend fun readSavedImageBytes(assetId: String): ByteArray? {
                    val bytes = storage.readSavedImageBytes(assetId)
                    email = "bob"
                    return bytes
                }
            }
            val repository = CachedMnemonicImageAssetRepository(
                local = if (fromCache) cache("empty-local") else switching,
                selectedEmail = { email },
                cacheForAccount = { if (it == "alice") switching else cache(it) },
                download = { _, _ -> error("File already exists") },
                ioContext = Dispatchers.IO,
            )
            assertNull(repository.readSavedImageBytes(IMAGE_ID))
        }
    }

    @Test
    fun `image import rejects unsafe IDs before creating a file`() = withImages { local, _ ->
        for (assetId in listOf("../escaped", "..\\escaped", "/absolute", "", "a".repeat(129))) {
            assertFailsWith<IllegalArgumentException> { local.importSavedImage(assetId, PNG_BYTES) }
        }
    }

    @Test
    fun `local images need no server and cancellation is not swallowed`() = withImages { local, cache ->
        local.importSavedImage(IMAGE_ID, PNG_BYTES)
        val offline = CachedMnemonicImageAssetRepository(local, { null }, cache, { _, _ -> error("not called") }, Dispatchers.IO)
        assertEquals(IMAGE_ID, offline.resolveSavedImage(IMAGE_ID)?.assetId)
        val cancelled = CachedMnemonicImageAssetRepository(local, { "alice" }, cache, { _, _ ->
            throw CancellationException("cancelled")
        }, Dispatchers.IO)
        assertFailsWith<CancellationException> { cancelled.resolveSavedImage("another-id") }
    }

    private fun withImages(block: suspend (IMnemonicImageAssetRepository, (String) -> IMnemonicImageAssetRepository) -> Unit) = runBlocking {
        val root = Files.createTempDirectory("klaf-image-cache-test-").toFile()
        try {
            val local = DesktopMnemonicImageAssetRepository(root.resolve("local"))
            val caches = mutableMapOf<String, IMnemonicImageAssetRepository>()
            block(local) { email -> caches.getOrPut(email) { DesktopMnemonicImageAssetRepository(root.resolve(email)) } }
        } finally {
            root.deleteRecursively()
        }
    }
}
