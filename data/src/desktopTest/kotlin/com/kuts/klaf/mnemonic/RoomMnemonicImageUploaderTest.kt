package com.kuts.klaf.mnemonic

import com.kuts.klaf.room.repositoryImplementations.RoomMnemonicImageUploader
import com.kuts.klaf.server.contract.SyncCard
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncRequest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

private val UPLOAD_PNG = byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10)
private fun uploadCard(id: String, image: String) = SyncCard(id, "deck", "родной", "foreign",
    mnemonicJson = """{"selectedIllustration":{"imageUrl":"$image"}}""")

class RoomMnemonicImageUploaderTest {

    @Test
    fun `only saved images in pending card changes are uploaded with account device and deduplication`() = runBlocking {
        val root = Files.createTempDirectory("klaf-upload-test-").toFile()
        try {
            val assets = DesktopMnemonicImageAssetRepository(root)
            assets.importSavedImage("new-image", UPLOAD_PNG)
            assets.createDraftImage(UPLOAD_PNG)
            val calls = mutableListOf<String>()
            val uploader = RoomMnemonicImageUploader(assets) { email, device, asset, bytes ->
                assertEquals(UPLOAD_PNG.toList(), bytes.toList())
                calls += "$email/$device/$asset"
            }
            uploader.prepare(SyncRequest("alice", "desktop", 2, 0, listOf(
                SyncOperation.AddCard("a", uploadCard("a", "new-image")),
                SyncOperation.EditCard("b", "b", uploadCard("b", "new-image")),
                SyncOperation.EditDeck("c", "deck", "renamed"),
                SyncOperation.AddCard("d", uploadCard("d", "missing-legacy-image")),
            )))
            assertEquals(listOf("alice/desktop/new-image"), calls)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `failure and cancellation propagate and retry reuses the original asset id`() = runBlocking {
        val root = Files.createTempDirectory("klaf-upload-test-").toFile()
        try {
            val assets = DesktopMnemonicImageAssetRepository(root)
            assets.importSavedImage("new-image", UPLOAD_PNG)
            val request = SyncRequest("alice", "desktop", 2, 0, listOf(SyncOperation.AddCard("a", uploadCard("a", "new-image"))))
            var fail = true
            val calls = mutableListOf<String>()
            val uploader = RoomMnemonicImageUploader(assets) { _, _, id, _ ->
                calls += id
                if (fail) error("lost response")
            }
            assertFailsWith<IllegalStateException> { uploader.prepare(request) }
            fail = false
            uploader.prepare(request)
            assertEquals(listOf("new-image", "new-image"), calls)
            val cancelled = RoomMnemonicImageUploader(assets) { _, _, _, _ -> throw CancellationException("cancel") }
            assertFailsWith<CancellationException> { cancelled.prepare(request) }
            Unit
        } finally {
            root.deleteRecursively()
        }
    }
}
