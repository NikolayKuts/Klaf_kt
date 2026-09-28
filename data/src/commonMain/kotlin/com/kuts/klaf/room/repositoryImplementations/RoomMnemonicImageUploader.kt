package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.entities.CardMnemonic
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import com.kuts.klaf.mnemonic.isSafeMnemonicImageId
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncRequest
import kotlinx.serialization.json.Json

/** File retries use immutable asset IDs; card outbox entries remain the durable work queue. */
class RoomMnemonicImageUploader(
    private val assets: IMnemonicImageAssetRepository,
    private val upload: suspend (email: String, deviceId: String, assetId: String, bytes: ByteArray) -> Unit,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    suspend fun prepare(request: SyncRequest) {
        val ids = request.operations.mapNotNull { operation ->
            val card = when (operation) {
                is SyncOperation.AddCard -> operation.card
                is SyncOperation.EditCard -> operation.card
                else -> null
            } ?: return@mapNotNull null
            if (card.mnemonicJson.isBlank()) return@mapNotNull null
            json.decodeFromString<CardMnemonic>(card.mnemonicJson).selectedIllustration?.imageAssetId
                ?.takeIf(String::isNotBlank)
        }.distinct()
        for (id in ids) {
            require(isSafeMnemonicImageId(id)) { "Invalid mnemonic image ID" }
            // Old backups can intentionally contain a reference without its file.
            val bytes = assets.readSavedImageBytes(id) ?: continue
            upload(request.email, request.deviceId, id, bytes)
        }
    }
}
