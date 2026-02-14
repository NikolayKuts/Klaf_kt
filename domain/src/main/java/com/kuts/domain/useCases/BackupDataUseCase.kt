package com.kuts.domain.useCases

import com.kuts.domain.common.DataSynchronizationValidator
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupDataUseCase(
    private val localDeckRepository: IDeckRepository,
    private val localCardRepository: ICardRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val remoteDeckRepository: IDeckRepository,
    private val remoteCardRepository: ICardRepository,
    private val remoteStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val dataSynchronizationValidator: DataSynchronizationValidator,
) {

    suspend operator fun invoke(backupPath: String) {
        withContext(Dispatchers.IO) {
            val localDecks = localDeckRepository.fetchAllDecks()
            val localCards = localCardRepository.fetchAllCards()
            val localVersion = localStorageSaveVersionRepository.fetchVersion()

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
