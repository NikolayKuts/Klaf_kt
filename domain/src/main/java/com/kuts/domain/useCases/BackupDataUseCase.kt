package com.kuts.domain.useCases

import com.kuts.domain.common.DataSynchronizationValidator
import com.kuts.domain.common.LocalCardRepository
import com.kuts.domain.common.LocalDeckRepository
import com.kuts.domain.common.LocalStorageSaveVersionRepository
import com.kuts.domain.common.RemoteCardRepository
import com.kuts.domain.common.RemoteDeckRepository
import com.kuts.domain.common.RemoteStorageSaveVersionRepository
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BackupDataUseCase @Inject constructor(
    @LocalDeckRepository
    private val localDeckRepository: IDeckRepository,
    @LocalCardRepository
    private val localCardRepository: ICardRepository,
    @LocalStorageSaveVersionRepository
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    @RemoteDeckRepository
    private val remoteDeckRepository: IDeckRepository,
    @RemoteCardRepository
    private val remoteCardRepository: ICardRepository,
    @RemoteStorageSaveVersionRepository
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
