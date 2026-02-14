package com.kuts.domain.useCases

import com.kuts.domain.common.DataSynchronizationValidator
import com.kuts.domain.common.LocalCardRepositoryImp
import com.kuts.domain.common.LocalDeckRepositoryImp
import com.kuts.domain.common.LocalStorageSaveVersionRepositoryImp
import com.kuts.domain.common.RemoteCardRepositoryImp
import com.kuts.domain.common.RemoteDeckRepositoryImp
import com.kuts.domain.common.RemoteStorageSaveVersionRepositoryImp
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.repositories.CardRepository
import com.kuts.domain.repositories.DeckRepository
import com.kuts.domain.repositories.StorageSaveVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BackupDataUseCase @Inject constructor(
    @LocalDeckRepositoryImp
    private val localDeckRepository: DeckRepository,
    @LocalCardRepositoryImp
    private val localCardRepository: CardRepository,
    @LocalStorageSaveVersionRepositoryImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    @RemoteDeckRepositoryImp
    private val remoteDeckRepository: DeckRepository,
    @RemoteCardRepositoryImp
    private val remoteCardRepository: CardRepository,
    @RemoteStorageSaveVersionRepositoryImp
    private val remoteStorageSaveVersionRepository: StorageSaveVersionRepository,
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
