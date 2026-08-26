package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import com.kuts.domain.repositories.IMnemonicImageRemoteRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import kotlinx.coroutines.withContext

class BackupDataUseCase(
    private val localDeckRepository: IDeckRepository,
    private val localCardRepository: ICardRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localMnemonicImageAssetRepository: IMnemonicImageAssetRepository,
    private val remoteDeckRepository: IDeckRepository,
    private val remoteCardRepository: ICardRepository,
    private val remoteStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val remoteMnemonicImageRepository: IMnemonicImageRemoteRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(backupPath: String) {
        withContext(context = coroutineContextProvider.io) {
            val localDecks = localDeckRepository.fetchAllDecks()
            val localCards = localCardRepository.fetchAllCards()
            val localVersion = localStorageSaveVersionRepository.fetchVersion()

            if (remoteMnemonicImageRepository.isEnabled) {
                for (assetId in localCards.referencedMnemonicImageAssetIds()) {
                    val imageBytes = requireNotNull(
                        localMnemonicImageAssetRepository.readSavedImageBytes(assetId = assetId)
                    ) {
                        "Mnemonic image asset is missing locally. assetId=$assetId"
                    }

                    remoteMnemonicImageRepository.uploadImageAtPath(
                        assetId = assetId,
                        imageBytes = imageBytes,
                        rootEmailPath = backupPath,
                    )
                }
            }

            for (deck in localDecks) {
                remoteDeckRepository.insertDeckAtPath(deck = deck, rootEmailPath = backupPath)
            }

            for (card in localCards) {
                remoteCardRepository.insertCardAtPath(card = card, rootEmailPath = backupPath)
            }

            val storageVersionToSave = localVersion ?: StorageSaveVersion()

            remoteStorageSaveVersionRepository.insertVersionAtPath(
                version = storageVersionToSave,
                rootEmailPath = backupPath
            )
        }
    }
}

private fun List<Card>.referencedMnemonicImageAssetIds(): Set<String> {
    return mapNotNull { card ->
        card.mnemonic.selectedIllustration?.imageAssetId?.takeIf { imageAssetId ->
            imageAssetId.isNotBlank()
        }
    }.toSet()
}
