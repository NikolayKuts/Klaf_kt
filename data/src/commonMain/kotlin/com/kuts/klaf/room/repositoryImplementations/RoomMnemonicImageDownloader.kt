package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.entities.CardMnemonic
import com.kuts.klaf.mnemonic.CachedMnemonicImageAssetRepository
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import kotlinx.serialization.json.Json

/** Completes an explicit sync with the saved images referenced by its resulting cards. */
class RoomMnemonicImageDownloader(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val images: CachedMnemonicImageAssetRepository,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    suspend fun download(accountId: String) {
        images.cacheMissingImages(accountId, referencedImageIds(accountId))
    }

    suspend fun hasMissingImages(accountId: String): Boolean = images.hasMissingImages(
        accountId,
        referencedImageIds(accountId),
    )

    private suspend fun referencedImageIds(accountId: String): List<String> {
        check(databaseSource.selection.value.accountEmail == accountId) { "Selected account changed" }
        return databaseSource.current().cardDao().getAllCards().mapNotNull { card ->
            card.mnemonicJson.takeIf(String::isNotBlank)
                ?.let { json.decodeFromString<CardMnemonic>(it).selectedIllustration?.imageAssetId }
                ?.takeIf(String::isNotBlank)
        }.distinct()
    }
}
