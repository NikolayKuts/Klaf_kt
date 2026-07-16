package com.kuts.domain.useCases

import com.kuts.domain.common.DataSynchronizationValidator
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.StorageSaveVersionValidationData
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.StorageSaveVersion
import com.kuts.domain.entities.StorageSaveVersion.Companion.INITIAL_SAVE_VERSION
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import com.kuts.domain.repositories.IMnemonicImageRemoteRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

class SynchronizeLocalAndRemoteDataUseCase(
    private val localDeckRepository: IDeckRepository,
    private val localCardRepository: ICardRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localMnemonicImageAssetRepository: IMnemonicImageAssetRepository,
    private val remoteDeckRepository: IDeckRepository,
    private val remoteCardRepository: ICardRepository,
    private val remoteStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val remoteMnemonicImageRepository: IMnemonicImageRemoteRepository,
    private val dataSynchronizationValidator: DataSynchronizationValidator,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    companion object {

        private const val INCREMENT_STEP = 1
    }

    operator fun invoke(): Flow<String> = channelFlow {
        manageDataSynchronization()
    }.flowOn(context = coroutineContextProvider.io)

    private suspend fun ProducerScope<String>.manageDataSynchronization() {
        coroutineScope {
            val localDecksDeferred = async { localDeckRepository.fetchAllDecks() }
            val localCarsDeferred = async { localCardRepository.fetchAllCards() }
            val localSaveVersionDeferred = async {
                localStorageSaveVersionRepository.fetchVersion()
            }

            val remoteDecksDeferred = async { remoteDeckRepository.fetchAllDecks() }
            val remoteCardsDeferred = async { remoteCardRepository.fetchAllCards() }
            val remoteSaveVersionDeferred = async {
                remoteStorageSaveVersionRepository.fetchVersion()
            }

            val localDecks = localDecksDeferred.await()
            val localCards = localCarsDeferred.await()
            val localSaveVersion = localSaveVersionDeferred.await()

            val remoteDecks = remoteDecksDeferred.await()
            val remoteCards = remoteCardsDeferred.await()
            val remoteSaveVersion = remoteSaveVersionDeferred.await()

            val storageSaveVersionValidationData = StorageSaveVersionValidationData(
                localStorageSaveVersion = localSaveVersion?.version,
                remoteStorageSaveVersion = remoteSaveVersion?.version,
            )

            when {
                dataSynchronizationValidator.areSaveVersionUndefined(
                    validationData = storageSaveVersionValidationData
                ) -> {
                    localStorageSaveVersionRepository.insertVersion(
                        version = StorageSaveVersion()
                    )
                    remoteStorageSaveVersionRepository.insertVersion(
                        version = StorageSaveVersion()
                    )
                }

                dataSynchronizationValidator.shouldLocalStorageBeUpdated(
                    validationData = storageSaveVersionValidationData
                ) -> {
                    synchronizeToLocal(
                        remoteDecks = remoteDecks,
                        remoteCards = remoteCards,
                        localDecks = localDecks,
                        localCards = localCards,
                        targetStorageSaveVersion = remoteSaveVersion?.version,
                    )
                }

                dataSynchronizationValidator.shouldRemoteStorageBeUpdated(
                    validationData = storageSaveVersionValidationData
                ) -> {
                    val targetVersion =
                        (remoteSaveVersion?.version ?: INITIAL_SAVE_VERSION) + INCREMENT_STEP

                    synchronizeToRemote(
                        localDecks = localDecks,
                        localCards = localCards,
                        remoteDecks = remoteDecks,
                        remoteCards = remoteCards,
                        targetStorageSaveVersion = targetVersion,
                    )
                }
            }
        }
    }

    private suspend fun ProducerScope<String>.synchronizeToLocal(
        remoteDecks: List<Deck>,
        remoteCards: List<Card>,
        localDecks: List<Deck>,
        localCards: List<Card>,
        targetStorageSaveVersion: Long?,
    ) {
        downloadReferencedMnemonicImages(cards = remoteCards)

        synchronizeStructuredData(
            newerVersionDecks = remoteDecks,
            newerVersionCards = remoteCards,
            olderSaveVersionDecks = localDecks,
            olderSaveVersionCards = localCards,
            olderSaveVersionDeckRepository = localDeckRepository,
            olderSaveVersionCardRepository = localCardRepository,
        )
        deleteObsoleteLocalMnemonicImages(
            olderCards = localCards,
            newerCards = remoteCards,
        )
        updateStorageSaveVersion(targetVersion = targetStorageSaveVersion)
    }

    private suspend fun ProducerScope<String>.synchronizeToRemote(
        localDecks: List<Deck>,
        localCards: List<Card>,
        remoteDecks: List<Deck>,
        remoteCards: List<Card>,
        targetStorageSaveVersion: Long?,
    ) {
        uploadReferencedMnemonicImages(cards = localCards)

        synchronizeStructuredData(
            newerVersionDecks = localDecks,
            newerVersionCards = localCards,
            olderSaveVersionDecks = remoteDecks,
            olderSaveVersionCards = remoteCards,
            olderSaveVersionDeckRepository = remoteDeckRepository,
            olderSaveVersionCardRepository = remoteCardRepository,
        )
        deleteObsoleteRemoteMnemonicImages(
            olderCards = remoteCards,
            newerCards = localCards,
        )
        updateStorageSaveVersion(targetVersion = targetStorageSaveVersion)
    }

    private suspend fun ProducerScope<String>.synchronizeStructuredData(
        newerVersionDecks: List<Deck>,
        newerVersionCards: List<Card>,
        olderSaveVersionDecks: List<Deck>,
        olderSaveVersionCards: List<Card>,
        olderSaveVersionDeckRepository: IDeckRepository,
        olderSaveVersionCardRepository: ICardRepository,
    ) {
        val notContainedDecks = olderSaveVersionDecks.filterNot { deck ->
            deck.id in newerVersionDecks.map { it.id }
        }

        deleteUnnecessaryCards(
            olderVersionCards = olderSaveVersionCards,
            newerVersionCards = newerVersionCards,
            notContainedDecks = notContainedDecks,
            olderSaveVersionCardRepository = olderSaveVersionCardRepository,
        )
        deleteUnnecessaryDecks(
            notContainedDecks = notContainedDecks,
            olderSaveVersionDeckRepository = olderSaveVersionDeckRepository,
        )
        insertDecks(
            newerVersionDecks = newerVersionDecks,
            olderSaveVersionDeckRepository = olderSaveVersionDeckRepository,
        )
        insertCards(
            newerVersionCards = newerVersionCards,
            olderSaveVersionCardRepository = olderSaveVersionCardRepository,
        )
    }

    private suspend fun ProducerScope<String>.deleteUnnecessaryDecks(
        notContainedDecks: List<Deck>,
        olderSaveVersionDeckRepository: IDeckRepository,
    ) {
        notContainedDecks.forEach { deck ->
            olderSaveVersionDeckRepository.removeDeck(deckId = deck.id)
            send(deck.name)
        }
    }

    private suspend fun ProducerScope<String>.insertDecks(
        newerVersionDecks: List<Deck>,
        olderSaveVersionDeckRepository: IDeckRepository,
    ) {
        newerVersionDecks.forEach { deck ->
            olderSaveVersionDeckRepository.insertDeck(deck = deck)
            send(deck.name)
        }
    }

    private suspend fun ProducerScope<String>.deleteUnnecessaryCards(
        olderVersionCards: List<Card>,
        newerVersionCards: List<Card>,
        notContainedDecks: List<Deck>,
        olderSaveVersionCardRepository: ICardRepository,
    ) {
        val notContainedDeckIds = notContainedDecks.map { it.id }

        olderVersionCards.filter { card ->
            card.id !in newerVersionCards.map { it.id } || card.deckId in notContainedDeckIds
        }.forEach { cardForDeleting ->
            olderSaveVersionCardRepository.deleteCard(cardId = cardForDeleting.id)
            send(cardForDeleting.nativeWord)
        }
    }

    private suspend fun ProducerScope<String>.insertCards(
        newerVersionCards: List<Card>,
        olderSaveVersionCardRepository: ICardRepository,
    ) {
        newerVersionCards.forEach { card ->
            olderSaveVersionCardRepository.insertCard(card = card)
            send(card.nativeWord)
        }
    }

    private suspend fun uploadReferencedMnemonicImages(cards: List<Card>) {
        cards.referencedMnemonicImageAssetIds().forEach { assetId ->
            val imageBytes = requireNotNull(
                localMnemonicImageAssetRepository.readSavedImageBytes(assetId = assetId)
            ) {
                "Mnemonic image asset is missing locally. assetId=$assetId"
            }

            remoteMnemonicImageRepository.uploadImage(
                assetId = assetId,
                imageBytes = imageBytes,
            )
        }
    }

    private suspend fun downloadReferencedMnemonicImages(cards: List<Card>) {
        cards.referencedMnemonicImageAssetIds().forEach { assetId ->
            val existingAsset = localMnemonicImageAssetRepository.resolveSavedImage(assetId = assetId)

            if (existingAsset == null) {
                val imageBytes = remoteMnemonicImageRepository.downloadImage(assetId = assetId)
                    ?: return@forEach

                localMnemonicImageAssetRepository.importSavedImage(
                    assetId = assetId,
                    imageBytes = imageBytes,
                )
            }
        }
    }

    private suspend fun deleteObsoleteLocalMnemonicImages(
        olderCards: List<Card>,
        newerCards: List<Card>,
    ) {
        val obsoleteAssetIds = olderCards.referencedMnemonicImageAssetIds() -
            newerCards.referencedMnemonicImageAssetIds()

        obsoleteAssetIds.forEach { assetId ->
            localMnemonicImageAssetRepository.deleteSavedImage(assetId = assetId)
        }
    }

    private suspend fun deleteObsoleteRemoteMnemonicImages(
        olderCards: List<Card>,
        newerCards: List<Card>,
    ) {
        val obsoleteAssetIds = olderCards.referencedMnemonicImageAssetIds() -
            newerCards.referencedMnemonicImageAssetIds()

        obsoleteAssetIds.forEach { assetId ->
            remoteMnemonicImageRepository.deleteImage(assetId = assetId)
        }
    }

    private suspend fun updateStorageSaveVersion(
        targetVersion: Long?,
    ) {
        val updatedStorageSaveVersion =
            StorageSaveVersion(version = targetVersion ?: INITIAL_SAVE_VERSION)

        remoteStorageSaveVersionRepository.insertVersion(version = updatedStorageSaveVersion)
        localStorageSaveVersionRepository.insertVersion(version = updatedStorageSaveVersion)
    }
}

private fun List<Card>.referencedMnemonicImageAssetIds(): Set<String> {
    return mapNotNull { card ->
        card.mnemonic.selectedIllustration?.imageAssetId?.takeIf { imageAssetId ->
            imageAssetId.isNotBlank()
        }
    }.toSet()
}
